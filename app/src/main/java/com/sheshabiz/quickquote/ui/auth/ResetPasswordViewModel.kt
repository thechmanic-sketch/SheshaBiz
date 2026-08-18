package com.sheshabiz.quickquote.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.remote.AuthSession
import com.sheshabiz.quickquote.data.remote.SupabaseAuthClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
    val newPassword: String = "",
    val newPasswordConfirm: String = "",
    val isSaving: Boolean = false,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val done: Boolean = false
)

/**
 * Sets a new password from a Supabase password-recovery deep link. [accessToken] and
 * [refreshToken] are the recovery session tokens parsed out of the `sheshabiz://reset-password`
 * link by [com.sheshabiz.quickquote.MainActivity] — they ARE a valid session (that's how
 * Supabase recovery links work), so on success this saves them via [AuthPreferences.saveSession]
 * exactly like every other login path, then reports [ResetPasswordUiState.done] so the caller
 * routes to the app's normal logged-in entry point (see
 * [com.sheshabiz.quickquote.ui.navigation.QuickQuoteNavHost]'s splash routing) — this is a
 * password reset, not a fresh signup, so it deliberately does NOT go through [AuthScreen]'s
 * onDone/chosenPlan mechanism.
 */
class ResetPasswordViewModel(
    private val authClient: SupabaseAuthClient,
    private val authPreferences: AuthPreferences,
    private val accessToken: String,
    private val refreshToken: String,
    /** Best-effort, decoded from the access token's JWT payload by [MainActivity] since
     * Supabase's recovery redirect doesn't include the email directly. Falls back to an empty
     * string if that decode ever fails — [AuthPreferences.saveSession] still works fine, it
     * just won't have an email to display in Settings until the next real login. */
    private val email: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, passwordError = null, errorMessage = null) }
    }

    fun onNewPasswordConfirmChange(value: String) {
        _uiState.update { it.copy(newPasswordConfirm = value, passwordError = null, errorMessage = null) }
    }

    /** Mirrors [AuthViewModel.savePassword]'s validation exactly: at least 8 characters, and
     * the confirmation must match. */
    fun savePassword() {
        val state = _uiState.value
        val password = state.newPassword
        val confirm = state.newPasswordConfirm
        if (password.length < 8) {
            _uiState.update { it.copy(passwordError = "Password must be at least 8 characters") }
            return
        }
        if (password != confirm) {
            _uiState.update { it.copy(passwordError = "Passwords don't match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, passwordError = null) }
            authClient.setPassword(accessToken, password).fold(
                onSuccess = {
                    authPreferences.saveSession(email.orEmpty(), AuthSession(accessToken, refreshToken, 3600L))
                    _uiState.update { it.copy(isSaving = false, done = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Couldn't save your password. Please try again."
                        )
                    }
                }
            )
        }
    }
}
