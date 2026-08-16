package com.sheshabiz.quickquote.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.remote.SendOtpResult
import com.sheshabiz.quickquote.data.remote.SupabaseAuthClient
import com.sheshabiz.quickquote.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep { EMAIL, CODE }

data class AuthUiState(
    val step: AuthStep = AuthStep.EMAIL,
    val email: String = "",
    val emailError: String? = null,
    val code: String = "",
    val isSendingCode: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val loggedIn: Boolean = false
)

/**
 * Drives the login-only email-OTP flow. Nothing here touches the Room database or any
 * Quote/Invoice/Customer/Product/Sale data — success only persists tokens via
 * [AuthPreferences].
 */
class AuthViewModel(
    private val authClient: SupabaseAuthClient,
    private val authPreferences: AuthPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun onCodeChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(code = digitsOnly, errorMessage = null) }
    }

    /** Sends (or resends) the OTP for the current email and moves to the code step on success. */
    fun sendCode() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !Validators.isValidEmail(email)) {
            _uiState.update { it.copy(emailError = "Enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCode = true, errorMessage = null, infoMessage = null) }
            when (val result = authClient.sendOtp(email)) {
                is SendOtpResult.Success -> _uiState.update {
                    it.copy(
                        isSendingCode = false,
                        step = AuthStep.CODE,
                        code = "",
                        infoMessage = "We sent a 6-digit code to $email"
                    )
                }
                is SendOtpResult.RateLimited -> _uiState.update {
                    it.copy(isSendingCode = false, errorMessage = "Please wait a bit before requesting another code.")
                }
                is SendOtpResult.Failure -> _uiState.update {
                    it.copy(isSendingCode = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Verifies the entered code. On success, persists the session and marks [AuthUiState.loggedIn]. */
    fun verifyCode() {
        val state = _uiState.value
        val email = state.email.trim()
        val code = state.code
        if (code.length != 6) {
            _uiState.update { it.copy(errorMessage = "Enter the 6-digit code") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, errorMessage = null, infoMessage = null) }
            authClient.verifyOtp(email, code).fold(
                onSuccess = { session ->
                    authPreferences.saveSession(email, session)
                    _uiState.update { it.copy(isVerifying = false, loggedIn = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isVerifying = false, errorMessage = error.message ?: "That code is incorrect or expired.")
                    }
                }
            )
        }
    }

    fun changeEmail() {
        _uiState.update {
            it.copy(step = AuthStep.EMAIL, code = "", errorMessage = null, infoMessage = null)
        }
    }
}
