package com.sheshabiz.quickquote.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Session tokens returned by Supabase after a successful OTP verification.
 * [expiresInSeconds] is the access token's lifetime as reported by Supabase, not an
 * absolute timestamp — callers that need an expiry moment should compute it from the
 * time the session was saved.
 */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long
)

/** Distinguishes a rate-limited OTP request from other failures so the UI can show a
 * specific "please wait" message instead of a generic error. */
sealed class SendOtpResult {
    object Success : SendOtpResult()
    object RateLimited : SendOtpResult()
    data class Failure(val message: String) : SendOtpResult()
}

/**
 * Plain REST wrapper around Supabase Auth (GoTrue) for email OTP login — no new backend,
 * no Supabase SDK dependency, mirroring [SignupApi]'s HttpURLConnection + JSONObject style.
 *
 * This client only ever touches `auth.users` via Supabase's own built-in auth endpoints.
 * It does not read or write any app data (quotes, invoices, customers, products, sales).
 */
object SupabaseAuthClient {
    private const val SUPABASE_URL = "https://djeipbwcyfaxjlllazbl.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_nuoqwOSuf6pJcTaj9lrrDg_hxgLHiHK"
    private const val CONNECT_TIMEOUT_MS = 8000
    private const val READ_TIMEOUT_MS = 8000

    /** Requests a one-time code by email. Works for both brand-new and returning accounts —
     * Supabase creates the auth.users row on first use when `create_user` is true. */
    suspend fun sendOtp(email: String): SendOtpResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("create_user", true)
            }
            val (code, responseText) = postJson("$SUPABASE_URL/auth/v1/otp", body.toString())
            when {
                code in 200..299 -> SendOtpResult.Success
                code == 429 -> SendOtpResult.RateLimited
                else -> SendOtpResult.Failure(extractErrorMessage(responseText) ?: "Couldn't send the code. Please try again.")
            }
        } catch (e: IOException) {
            SendOtpResult.Failure("Couldn't reach the server. Check your connection and try again.")
        } catch (e: Exception) {
            SendOtpResult.Failure("Something went wrong. Please try again.")
        }
    }

    /** Verifies a 6-digit email OTP and returns the resulting session on success. */
    suspend fun verifyOtp(email: String, code: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("type", "email")
                put("email", email)
                put("token", code)
            }
            val (statusCode, responseText) = postJson("$SUPABASE_URL/auth/v1/verify", body.toString())
            if (statusCode in 200..299) {
                val json = JSONObject(responseText)
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val expiresIn = json.optLong("expires_in", 3600L)
                if (accessToken.isNotEmpty() && refreshToken.isNotEmpty()) {
                    Result.success(AuthSession(accessToken, refreshToken, expiresIn))
                } else {
                    Result.failure(Exception("Login failed. Please try again."))
                }
            } else {
                Result.failure(Exception("That code is incorrect or expired."))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Couldn't reach the server. Check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(Exception("That code is incorrect or expired."))
        }
    }

    private fun extractErrorMessage(responseText: String?): String? {
        if (responseText.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(responseText)
            json.optString("error_description", null) ?: json.optString("msg", null) ?: json.optString("error", null)
        }.getOrNull()
    }

    /** Returns the HTTP status code and the response body (from either stream depending on success). */
    private fun postJson(urlString: String, json: String): Pair<Int, String?> {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("apikey", SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(json.toByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { reader -> reader.readText() }
            code to text
        } finally {
            connection.disconnect()
        }
    }
}
