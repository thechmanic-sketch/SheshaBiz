package com.sheshabiz.quickquote.data.remote

import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

/** The caller's business row, as returned by the `get_my_business_status` RPC. */
data class BusinessStatus(
    val status: String,
    val trialStartedAt: Long?,
    val validUntil: Long?,
    val isActive: Boolean
)

/**
 * Minimal authenticated PostgREST wrapper around the app's Supabase project — no Supabase
 * SDK dependency, same plain HttpURLConnection + org.json style as [SupabaseAuthClient] and
 * [SignupApi]. Every request carries the *logged-in user's* access token (never the anon key
 * alone), since `bootstrap_business`/`get_my_business_status` and every table read/write rely
 * on `auth.uid()` resolving via RLS.
 */
class SupabaseRestClient(private val authPreferences: AuthPreferences) {
    private val connectTimeoutMs = 8000
    private val readTimeoutMs = 15000

    /** Idempotent — creates the caller's `businesses` row on first call (starting their
     * trial), returns the same row's id on every call after, including re-logins. Returns
     * null if the caller isn't logged in, or the request fails. */
    suspend fun bootstrapBusiness(): String? {
        val (code, body) = post("rpc/bootstrap_business", "{}")
        if (code !in 200..299 || body.isNullOrBlank()) return null
        return runCatching { extractUuid(body) }.getOrNull()
    }

    /** Reads the caller's current trial/subscription status. Returns null on failure so the
     * sync engine can leave the last cached [com.sheshabiz.quickquote.data.prefs.AuthPreferences]
     * value in place rather than guessing. */
    suspend fun getMyBusinessStatus(): BusinessStatus? {
        val (code, body) = post("rpc/get_my_business_status", "{}")
        if (code !in 200..299 || body.isNullOrBlank()) return null
        return runCatching {
            val obj = asSingleObject(body) ?: return null
            BusinessStatus(
                status = obj.optString("status", "trialing"),
                trialStartedAt = parseTimestamp(obj.opt("trial_started_at")),
                validUntil = parseTimestamp(obj.opt("valid_until")),
                isActive = obj.optBoolean("is_active", false)
            )
        }.getOrNull()
    }

    /** Best-effort admin-visibility signal: records which paid tier ('monthly', 'quarterly',
     * 'biannual', or 'annual') the logged-in caller is currently looking at on the pay-confirm
     * screen, via the `set_pending_plan` RPC, so the admin dashboard can show "wants R70" before
     * the customer has actually paid. Scoped server-side to the caller's own business via
     * `auth.uid()`. Fire-and-forget by design — callers should log/ignore a failure rather than
     * surface it or block on it. */
    suspend fun setPendingPlan(tier: String): Result<Unit> {
        val body = JSONObject().put("tier", tier).toString()
        val (code, _) = post("rpc/set_pending_plan", body)
        return if (code in 200..299) {
            Result.success(Unit)
        } else {
            Result.failure(IOException("set_pending_plan failed: HTTP $code"))
        }
    }

    /** GET under `/rest/v1/`, e.g. `get("quotes?business_id=eq.$id&updated_at=gt.$since")`.
     * Returns the HTTP status code and raw response body (success or error). */
    suspend fun get(path: String, extraHeaders: Map<String, String> = emptyMap()): Pair<Int, String?> =
        request("GET", path, null, extraHeaders)

    /** POST under `/rest/v1/`, e.g. an upsert (`Prefer: resolution=merge-duplicates`) or an
     * `rpc/<name>` call. Returns the HTTP status code and raw response body. */
    suspend fun post(path: String, body: String, extraHeaders: Map<String, String> = emptyMap()): Pair<Int, String?> =
        request("POST", path, body, extraHeaders)

    private suspend fun request(
        method: String,
        path: String,
        body: String?,
        extraHeaders: Map<String, String>
    ): Pair<Int, String?> = withContext(Dispatchers.IO) {
        val token = authPreferences.getAccessToken() ?: return@withContext 401 to null
        try {
            val connection = (URL("$SUPABASE_URL/rest/v1/$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = body != null
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                extraHeaders.forEach { (key, value) -> setRequestProperty(key, value) }
            }
            try {
                if (body != null) {
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { reader -> reader.readText() }
                code to text
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            -1 to null
        } catch (e: Exception) {
            -1 to null
        }
    }

    /** A scalar-returning RPC (`bootstrap_business` returns `uuid`) comes back from PostgREST
     * as a bare, quoted JSON string — but tolerate an object/array wrapper too. */
    private fun extractUuid(body: String): String? =
        when (val value = JSONTokener(body).nextValue()) {
            is String -> value.trim('"')
            is JSONObject -> value.optString("bootstrap_business", null) ?: value.optString("id", null)
            is JSONArray -> value.optJSONObject(0)?.let { it.optString("bootstrap_business", null) ?: it.optString("id", null) }
            else -> null
        }

    /** `get_my_business_status` returns a single composite row — normally a bare JSON object,
     * but PostgREST wraps some composite RPCs in a one-element array, so accept either. */
    private fun asSingleObject(body: String): JSONObject? =
        when (val value = JSONTokener(body).nextValue()) {
            is JSONObject -> value
            is JSONArray -> value.optJSONObject(0)
            else -> null
        }

    companion object {
        private const val SUPABASE_URL = "https://djeipbwcyfaxjlllazbl.supabase.co"
        private const val SUPABASE_ANON_KEY = "sb_publishable_nuoqwOSuf6pJcTaj9lrrDg_hxgLHiHK"

        private val offsetPattern: Pattern = Pattern.compile("([+-]\\d{2}:?\\d{2}|Z)$")

        /** Epoch millis -> the ISO-8601 UTC string Postgres/PostgREST expects for a
         * `timestamptz` value, e.g. in an upsert body or an `updated_at=gt.<...>` filter. */
        fun isoString(millis: Long): String {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            return format.format(Date(millis))
        }

        /** Parses a Postgres/PostgREST `timestamptz` JSON string (e.g.
         * `2026-08-23T10:15:30.123456+00:00` or `...Z`) into epoch millis. minSdk 21 has no
         * `java.time` (no core-library desugaring in this build), so this is done by hand
         * rather than with `Instant.parse`. Returns null for a null/blank/unparseable value. */
        fun parseTimestamp(raw: Any?): Long? {
            val text = (raw as? String)?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
            return runCatching {
                val offsetMatcher = offsetPattern.matcher(text)
                val hasOffset = offsetMatcher.find()
                val offsetRaw = if (hasOffset) offsetMatcher.group() else "Z"
                val withoutOffset = if (hasOffset) text.substring(0, offsetMatcher.start()) else text
                val dotIndex = withoutOffset.indexOf('.')
                val base = if (dotIndex >= 0) withoutOffset.substring(0, dotIndex) else withoutOffset
                val fraction = if (dotIndex >= 0) withoutOffset.substring(dotIndex + 1) else ""
                val millisFraction = (fraction + "000").substring(0, 3)
                val normalizedOffset = if (offsetRaw == "Z") "+0000" else offsetRaw.replace(":", "")
                val normalized = "$base.$millisFraction$normalizedOffset"
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
                format.parse(normalized)?.time
            }.getOrNull()
        }
    }
}
