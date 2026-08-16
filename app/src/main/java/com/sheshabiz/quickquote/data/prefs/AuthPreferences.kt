package com.sheshabiz.quickquote.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sheshabiz.quickquote.data.remote.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the logged-in account's email and session tokens, backed by
 * [EncryptedSharedPreferences] since a refresh token lives here. Entirely separate from
 * [AppPreferences] and from the Room database — this is login-only bookkeeping and never
 * touches Quote/Invoice/Customer/Product/Sale data.
 */
class AuthPreferences(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext)

    private val _isLoggedIn = MutableStateFlow(prefs.contains(KEY_ACCESS_TOKEN))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _loggedInEmail = MutableStateFlow(prefs.getString(KEY_EMAIL, null))
    val loggedInEmail: StateFlow<String?> = _loggedInEmail

    /** Persists the session after a successful OTP verification and updates observers. */
    fun saveSession(email: String, session: AuthSession) {
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_IN, session.expiresInSeconds)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
        _loggedInEmail.value = email
        _isLoggedIn.value = true
    }

    /** Logs out: wipes the stored session entirely. Local business data is untouched. */
    fun clearSession() {
        prefs.edit().clear().apply()
        _loggedInEmail.value = null
        _isLoggedIn.value = false
    }

    /** Stored access token, for [com.sheshabiz.quickquote.data.remote.SupabaseAuthClient] use only. */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /** Stored refresh token, for [com.sheshabiz.quickquote.data.remote.SupabaseAuthClient] use only. */
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_NAME = "quickquote_auth_prefs"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_EXPIRES_IN = "auth_expires_in"
        private const val KEY_SAVED_AT = "auth_saved_at"
    }
}
