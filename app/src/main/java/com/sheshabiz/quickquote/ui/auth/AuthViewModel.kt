package com.sheshabiz.quickquote.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.remote.SendOtpResult
import com.sheshabiz.quickquote.data.remote.SupabaseAuthClient
import com.sheshabiz.quickquote.data.sync.SyncManager
import com.sheshabiz.quickquote.domain.Validators
import com.sheshabiz.quickquote.ui.subscription.SubscriptionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep { CHOOSE_START, EMAIL, CODE, SET_PASSWORD }

data class AuthUiState(
    val step: AuthStep = AuthStep.CHOOSE_START,
    /** What the user picked on the [AuthStep.CHOOSE_START] step. Null means the free 7-day
     * trial (the default); a [SubscriptionPlan] means they want to pay for that tier straight
     * away. This is only an intent signal for where to send them after verifying — every
     * business still gets the same real trial regardless of this choice; see
     * [com.sheshabiz.quickquote.ui.navigation.QuickQuoteNavHost] for how it's used post-login. */
    val chosenPlan: SubscriptionPlan? = null,
    val email: String = "",
    val emailError: String? = null,
    val code: String = "",
    val isSendingCode: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val loggedIn: Boolean = false,
    /** Password-login fields, used on the [AuthStep.EMAIL] step as a faster alternative to
     * the OTP flow above — see [AuthViewModel.loginWithPassword]. */
    val password: String = "",
    val passwordError: String? = null,
    val showPasswordLogin: Boolean = false,
    val isPasswordLoggingIn: Boolean = false,
    /** New-password fields, used on the [AuthStep.SET_PASSWORD] step right after a first-ever
     * OTP verification — see [AuthViewModel.savePassword]. */
    val newPassword: String = "",
    val newPasswordConfirm: String = "",
    val isSavingPassword: Boolean = false
)

/**
 * Drives the login-only email-OTP flow. Success persists tokens via [AuthPreferences] and
 * fires an immediate cross-device sync pass in the background — otherwise nothing here
 * touches the Room database or any Quote/Invoice/Customer/Product/Sale data directly.
 */
class AuthViewModel(
    private val authClient: SupabaseAuthClient,
    private val authPreferences: AuthPreferences,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    /** Advances from [AuthStep.CHOOSE_START] to email entry. [plan] is null for the free trial,
     * or the paid tier the user wants to buy straight away. */
    fun selectStart(plan: SubscriptionPlan?) {
        _uiState.update { it.copy(chosenPlan = plan, step = AuthStep.EMAIL) }
    }

    /** Email-step → choose-start-step in-screen back action (mirrors [changeEmail]). */
    fun backToStart() {
        _uiState.update { it.copy(step = AuthStep.CHOOSE_START, errorMessage = null) }
    }

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

    /** Verifies the entered code. On success, persists the session and moves to
     * [AuthStep.SET_PASSWORD] so this first-ever login on this account can offer a password
     * for faster logins later — see [savePassword] / [skipPassword] for how that step ends
     * in [AuthUiState.loggedIn]. */
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
                    _uiState.update { it.copy(isVerifying = false, step = AuthStep.SET_PASSWORD) }
                    // Fire-and-forget: don't block the success UI state on network/sync —
                    // this starts the trial clock server-side (via the idempotent
                    // bootstrap_business RPC inside SyncManager.sync()) and pulls in
                    // anything already on the account from another device.
                    viewModelScope.launch { syncManager.sync() }
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

    /** Shows/hides the password field on the [AuthStep.EMAIL] step, toggling between the
     * OTP flow and the instant password-login flow. */
    fun togglePasswordLogin() {
        _uiState.update { it.copy(showPasswordLogin = !it.showPasswordLogin, errorMessage = null, passwordError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    /** Instant login for a returning user who already set a password — skips the OTP email
     * entirely and, unlike [verifyCode], goes straight to [AuthUiState.loggedIn] since a
     * password already exists for this account. */
    fun loginWithPassword() {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password
        if (email.isBlank() || !Validators.isValidEmail(email)) {
            _uiState.update { it.copy(emailError = "Enter a valid email address") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Enter your password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPasswordLoggingIn = true, errorMessage = null, passwordError = null) }
            authClient.signInWithPassword(email, password).fold(
                onSuccess = { session ->
                    authPreferences.saveSession(email, session)
                    _uiState.update { it.copy(isPasswordLoggingIn = false, loggedIn = true) }
                    // Same fire-and-forget sync as the OTP path — see verifyCode().
                    viewModelScope.launch { syncManager.sync() }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isPasswordLoggingIn = false, errorMessage = error.message ?: "Incorrect email or password.")
                    }
                }
            )
        }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, passwordError = null) }
    }

    fun onNewPasswordConfirmChange(value: String) {
        _uiState.update { it.copy(newPasswordConfirm = value, passwordError = null) }
    }

    /** Sets a password for the account that was just verified via OTP, so future logins can
     * use [loginWithPassword] instead of waiting on another email. Never blocks app entry on
     * the network call succeeding — [AuthUiState.loggedIn] is set regardless of the result,
     * since a password can always be set later. */
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

        val accessToken = authPreferences.getAccessToken()
        if (accessToken == null) {
            // Shouldn't happen — saveSession() just ran in verifyCode() — but don't block entry.
            _uiState.update { it.copy(loggedIn = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPassword = true, passwordError = null) }
            authClient.setPassword(accessToken, password).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSavingPassword = false, loggedIn = true) }
                },
                onFailure = { error ->
                    // Don't block app entry on this failing — let them in anyway.
                    _uiState.update {
                        it.copy(
                            isSavingPassword = false,
                            errorMessage = error.message ?: "Couldn't save your password. You can try again later.",
                            loggedIn = true
                        )
                    }
                }
            )
        }
    }

    /** Skips setting a password for now — just finishes login. */
    fun skipPassword() {
        _uiState.update { it.copy(loggedIn = true) }
    }
}
