package com.sheshabiz.quickquote.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextActionButton
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenHeader

/**
 * Two-step, single-screen email OTP login: email entry, then code entry, switching in place
 * (no separate nav route for the second step, mirroring [com.sheshabiz.quickquote.ui.lock.PinSetupScreen]).
 *
 * This is login only. It never syncs, pushes, or pulls any business data — a successful
 * verification only stores a session via [com.sheshabiz.quickquote.data.prefs.AuthPreferences].
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onDone()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Account", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            when (state.step) {
                AuthStep.EMAIL -> EmailStep(state = state, viewModel = viewModel)
                AuthStep.CODE -> CodeStep(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun EmailStep(state: AuthUiState, viewModel: AuthViewModel) {
    Text(
        text = "Log in or create an account",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Create an account to secure future access to SheshaBiz — for example, if you reinstall the app. " +
            "This does not sync or back up your data.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(24.dp))

    QQTextField(
        value = state.email,
        onValueChange = viewModel::onEmailChange,
        label = "Email",
        keyboardType = KeyboardType.Email,
        isError = state.emailError != null,
        errorText = state.emailError
    )
    Spacer(Modifier.height(16.dp))

    if (state.errorMessage != null) {
        Text(
            text = state.errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
    }

    QQPrimaryButton(
        text = "Send code",
        onClick = viewModel::sendCode,
        loading = state.isSendingCode
    )
}

@Composable
private fun CodeStep(state: AuthUiState, viewModel: AuthViewModel) {
    Text(
        text = "Enter your code",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = state.infoMessage ?: "Enter the 6-digit code sent to ${state.email}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(24.dp))

    QQTextField(
        value = state.code,
        onValueChange = viewModel::onCodeChange,
        label = "6-digit code",
        keyboardType = KeyboardType.NumberPassword
    )
    Spacer(Modifier.height(16.dp))

    if (state.errorMessage != null) {
        Text(
            text = state.errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
    }

    QQPrimaryButton(
        text = "Verify",
        onClick = viewModel::verifyCode,
        loading = state.isVerifying
    )
    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        QQTextActionButton(text = "Resend code", onClick = viewModel::sendCode)
        QQTextActionButton(text = "Change email", onClick = viewModel::changeEmail)
    }
}
