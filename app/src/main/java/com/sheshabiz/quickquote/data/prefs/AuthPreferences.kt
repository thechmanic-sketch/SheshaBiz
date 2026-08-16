package com.sheshabiz.quickquote.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sheshabiz.quickquote.data.remote.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The caller's cached trial/subscription state, as last reported by the
 * `get_my_business_status` RPC. Read by every "create/edit" gate in the app, so it has to
 * work fully offline between sync passes — hence it's cached here, not re-fetched live. */
enum class SubscriptionState { LOGGED_OUT, TRIALING, ACTIVE, LAPSED }

/**
 * Holds the logged-in account's email and session tokens, backed by
 * [EncryptedSharedPreferences] since a refresh token lives here. Entirely separate from
 * [AppPreferences] and from the Room database — this is login-only bookkeeping and never
 * touches Quote/Invoice/Customer/Product/Sale data.
 *
 * Also caches the account's cross-device-sync business id and trial/subscription state
 * (written by [com.sheshabiz.quickquote.data.sync.SyncManager] after each sync pass) so the
 * trial soft-lock can be checked instantly and offline, without waiting on a network call.
 */
class AuthPreferences(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext)

    private val _isLoggedIn = MutableStateFlow(prefs.contains(KEY_ACCESS_TOKEN))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _loggedInEmail = MutableStateFlow(prefs.getString(KEY_EMAIL, null))
    val loggedInEmail: StateFlow<String?> = _loggedInEmail

    private val _subscriptionState = MutableStateFlow(readSubscriptionState())
    /** [SubscriptionState.LOGGED_OUT] for anyone who has never logged in — the trial gate
     * only ever blocks on [SubscriptionState.LAPSED], so a logged-out user is never gated. */
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState

    private val _businessId = MutableStateFlow(prefs.getString(KEY_BUSINESS_ID, null))
    val businessId: StateFlow<String?> = _businessId

    private val _trialStartedAt = MutableStateFlow(readLongOrNull(KEY_TRIAL_STARTED_AT))
    val trialStartedAt: StateFlow<Long?> = _trialStartedAt

    private val _validUntil = MutableStateFlow(readLongOrNull(KEY_VALID_UNTIL))
    val validUntil: StateFlow<Long?> = _validUntil

    private val _lastSyncedAt = MutableStateFlow(readLongOrNull(KEY_LAST_SYNCED_AT))
    val lastSyncedAt: StateFlow<Long?> = _lastSyncedAt

    /** Persists the session after a successful OTP verification and updates observers.
     * Deliberately leaves any cached subscription/business fields alone — a re-login to the
     * same email should keep showing the last-known trial state until the sync pass that
     * [com.sheshabiz.quickquote.ui.auth.AuthViewModel] fires right after login corrects it
     * (or, for a brand-new login, [subscriptionState] is already [SubscriptionState.LOGGED_OUT],
     * which never gates anything on its own). */
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

    /** Logs out: wipes the stored session entirely, including the cached sync/subscription
     * state. Local business data is untouched. */
    fun clearSession() {
        prefs.edit().clear().apply()
        _loggedInEmail.value = null
        _isLoggedIn.value = false
        _subscriptionState.value = SubscriptionState.LOGGED_OUT
        _businessId.value = null
        _trialStartedAt.value = null
        _validUntil.value = null
        _lastSyncedAt.value = null
    }

    /** Stored access token, for [com.sheshabiz.quickquote.data.remote.SupabaseAuthClient] use only. */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /** Stored refresh token, for [com.sheshabiz.quickquote.data.remote.SupabaseAuthClient] use only. */
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    /** Called by [com.sheshabiz.quickquote.data.sync.SyncManager] right after
     * `bootstrap_business()` resolves the caller's business id (a no-op after the first
     * successful call, since it's idempotent). */
    fun setBusinessId(id: String) {
        prefs.edit().putString(KEY_BUSINESS_ID, id).apply()
        _businessId.value = id
    }

    /** Called by [com.sheshabiz.quickquote.data.sync.SyncManager] after every
     * `get_my_business_status()` call. An unrecognized [status] string leaves the previously
     * cached state alone rather than guessing. */
    fun updateSubscriptionStatus(status: String, trialStartedAt: Long?, validUntil: Long?) {
        val mapped = when (status) {
            "active" -> SubscriptionState.ACTIVE
            "lapsed" -> SubscriptionState.LAPSED
            "trialing" -> SubscriptionState.TRIALING
            else -> null
        } ?: return
        val editor = prefs.edit().putString(KEY_SUBSCRIPTION_STATUS, status)
        if (trialStartedAt != null) editor.putLong(KEY_TRIAL_STARTED_AT, trialStartedAt) else editor.remove(KEY_TRIAL_STARTED_AT)
        if (validUntil != null) editor.putLong(KEY_VALID_UNTIL, validUntil) else editor.remove(KEY_VALID_UNTIL)
        editor.apply()
        _subscriptionState.value = mapped
        _trialStartedAt.value = trialStartedAt
        _validUntil.value = validUntil
    }

    /** Called by [com.sheshabiz.quickquote.data.sync.SyncManager] once a full push+pull pass
     * completes, so Settings can show "Last synced …". */
    fun setLastSyncedAt(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNCED_AT, timestamp).apply()
        _lastSyncedAt.value = timestamp
    }

    private fun readSubscriptionState(): SubscriptionState {
        if (!prefs.contains(KEY_ACCESS_TOKEN)) return SubscriptionState.LOGGED_OUT
        return when (prefs.getString(KEY_SUBSCRIPTION_STATUS, null)) {
            "active" -> SubscriptionState.ACTIVE
            "lapsed" -> SubscriptionState.LAPSED
            // Logged in but no cached status yet (first login before the first sync pass
            // lands, or an already-shipped login-only account seeing this feature for the
            // first time) — default to non-blocking until a real status is known.
            else -> SubscriptionState.TRIALING
        }
    }

    private fun readLongOrNull(key: String): Long? = if (prefs.contains(key)) prefs.getLong(key, 0L) else null

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
        private const val KEY_BUSINESS_ID = "auth_business_id"
        private const val KEY_SUBSCRIPTION_STATUS = "auth_subscription_status"
        private const val KEY_TRIAL_STARTED_AT = "auth_trial_started_at"
        private const val KEY_VALID_UNTIL = "auth_valid_until"
        private const val KEY_LAST_SYNCED_AT = "auth_last_synced_at"
    }
}
