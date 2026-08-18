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

    /** Creates a brand-new account with email + password — the primary signup path now that
     * "Confirm email" is off in the Supabase dashboard, so this sends zero email and returns a
     * usable session immediately. Supabase's anti-enumeration behavior means an email that's
     * already registered comes back as a 2xx with no access_token rather than an error, so that
     * case is surfaced as a distinct [Result.failure] (message == "ACCOUNT_EXISTS") for the
     * caller to pattern-match on instead of showing it to the user directly. [data], when
     * present, is sent under the signup request's `"data"` key — Supabase stores this verbatim
     * as `auth.users.raw_user_meta_data`, no schema change needed. Used to capture light signup
     * context (full name, phone, business name, city) for the admin doing manual plan
     * activation later; see [com.sheshabiz.quickquote.ui.auth.AuthViewModel.createAccount]. */
    suspend fun signUp(email: String, password: String, data: Map<String, String>? = null): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
                if (!data.isNullOrEmpty()) {
                    val metadata = JSONObject()
                    data.forEach { (key, value) -> metadata.put(key, value) }
                    put("data", metadata)
                }
            }
            val (statusCode, responseText) = postJson("$SUPABASE_URL/auth/v1/signup", body.toString())
            if (statusCode in 200..299) {
                val json = JSONObject(responseText)
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val expiresIn = json.optLong("expires_in", 3600L)
                if (accessToken.isNotEmpty() && refreshToken.isNotEmpty()) {
                    Result.success(AuthSession(accessToken, refreshToken, expiresIn))
                } else {
                    // Email already registered: Supabase returns 200 with a user object
                    // (empty `identities`, no session) instead of an error, to avoid leaking
                    // which emails exist.
                    Result.failure(Exception("ACCOUNT_EXISTS"))
                }
            } else {
                Result.failure(Exception(extractErrorMessage(responseText) ?: "Couldn't create the account. Please try again."))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Couldn't reach the server. Check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't create the account. Please try again."))
        }
    }

    /** Signs in with email + password — the fast path for a returning user who already set a
     * password via [setPassword], skipping the OTP email entirely. */
    suspend fun signInWithPassword(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val (statusCode, responseText) = postJson("$SUPABASE_URL/auth/v1/token?grant_type=password", body.toString())
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
                Result.failure(Exception(extractErrorMessage(responseText) ?: "Incorrect email or password."))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Couldn't reach the server. Check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(Exception("Incorrect email or password."))
        }
    }

    /** Sets (or replaces) the password on the currently-signed-in account, so future logins
     * can skip the OTP email via [signInWithPassword]. [accessToken] must be a freshly-issued
     * session token (e.g. right after an OTP verification). */
    suspend fun setPassword(accessToken: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("password", password)
            }
            val (statusCode, responseText) = putJson("$SUPABASE_URL/auth/v1/user", body.toString(), accessToken)
            if (statusCode in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(extractErrorMessage(responseText) ?: "Couldn't save your password. Please try again."))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Couldn't reach the server. Check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't save your password. Please try again."))
        }
    }

    /** Requests a password-reset email for [email], separate from the OTP-login fallback — this
     * is the standard "forgot password" flow for an account that already has a password. Posts
     * to `/auth/v1/recover` with [redirectTo] (URL-encoded) as the `redirect_to` query param, so
     * Supabase's emailed link opens back into the app there with recovery tokens attached — see
     * [com.sheshabiz.quickquote.MainActivity] for how those are parsed back out.
     *
     * Supabase's `/recover` endpoint deliberately never reveals whether an account exists for
     * [email] (anti-enumeration) — it responds 2xx either way, regardless of whether an account
     * is actually found. So a [Result.failure] here only ever means a genuine network or server
     * problem, never "no such account": callers are safe to show a generic "if an account
     * exists..." message on [Result.success] and the real error message on failure. */
    suspend fun requestPasswordReset(email: String, redirectTo: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email)
            }
            val encodedRedirect = java.net.URLEncoder.encode(redirectTo, "UTF-8")
            val (statusCode, responseText) = postJson("$SUPABASE_URL/auth/v1/recover?redirect_to=$encodedRedirect", body.toString())
            if (statusCode in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(extractErrorMessage(responseText) ?: "Couldn't send the reset link. Please try again."))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Couldn't reach the server. Check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't send the reset link. Please try again."))
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

    /** Same shape as [postJson] but for PUT requests that need an additional bearer token on
     * top of the anon `apikey` header — used for authenticated calls like updating the current
     * user's password. */
    private fun putJson(urlString: String, json: String, accessToken: String): Pair<Int, String?> {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("apikey", SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
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
