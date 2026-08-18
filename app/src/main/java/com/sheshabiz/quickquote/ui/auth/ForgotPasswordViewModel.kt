package com.sheshabiz.quickquote.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.remote.SupabaseAuthClient
import com.sheshabiz.quickquote.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The deep link Supabase's recovery email redirects to once the user taps it — must match the
 * `sheshabiz://reset-password` intent filter on `MainActivity`. Recovery tokens are parsed back
 * out of that redirect by [com.sheshabiz.quickquote.MainActivity]. */
const val PASSWORD_RESET_REDIRECT_URL = "sheshabiz://reset-password"

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isSending: Boolean = false,
    /** Set after a request completes, success or failure — always the generic
     * anti-enumeration message on success; the real error message on failure (safe to show,
     * since a failure here can only mean a genuine network/server problem — see
     * [SupabaseAuthClient.requestPasswordReset]). */
    val message: String? = null,
    val isError: Boolean = false
)

/**
 * Drives the standalone "forgot password" reset-link flow — a separate, more standard
 * alternative to the existing OTP-code login fallback reachable from [AuthScreen]'s login mode.
 * That OTP fallback is unchanged; this is for someone who has a password but forgot it.
 */
class ForgotPasswordViewModel(
    private val authClient: SupabaseAuthClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, message = null, isError = false) }
    }

    /** Requests a reset link for the entered email. Always ends in the same generic success
     * message on a completed request, regardless of whether the account actually exists — see
     * [SupabaseAuthClient.requestPasswordReset]'s anti-enumeration note. */
    fun sendResetLink() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !Validators.isValidEmail(email)) {
            _uiState.update { it.copy(emailError = "Enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, message = null, isError = false) }
            authClient.requestPasswordReset(email, PASSWORD_RESET_REDIRECT_URL).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            isError = false,
                            message = "If an account exists for that email, we've sent a reset link."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            isError = true,
                            message = error.message ?: "Couldn't send the reset link. Please try again."
                        )
                    }
                }
            )
        }
    }
}
