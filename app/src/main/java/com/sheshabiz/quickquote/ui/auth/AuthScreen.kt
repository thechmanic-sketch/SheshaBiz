package com.sheshabiz.quickquote.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextActionButton
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenHeader
import com.sheshabiz.quickquote.ui.subscription.SubscriptionPlan

/**
 * Single-screen login: pick how to start (free trial or a paid plan), then email+password entry
 * — account creation by default, or login mode for returning users (see [AuthViewModel.
 * toggleReturningUser]) — with OTP code entry and an optional set-a-password step reachable only
 * as an explicit fallback for legacy no-password accounts, all switching in place (no separate
 * nav route per step, mirroring [com.sheshabiz.quickquote.ui.lock.PinSetupScreen]).
 *
 * This is login only. It never syncs, pushes, or pulls any business data — a successful
 * verification only stores a session via [com.sheshabiz.quickquote.data.prefs.AuthPreferences].
 * Every business gets the same real 7-day trial regardless of what's picked on the first step —
 * [onDone] just tells the caller where to send the user next: [onDone] receives the plan chosen
 * on [AuthStep.CHOOSE_START] (null for the free trial) so it can route a would-be paying
 * customer straight to the subscription screen instead of back to wherever they came from.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onDone: (SubscriptionPlan?) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onDone(state.chosenPlan)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Account",
            onBack = {
                if (state.step == AuthStep.EMAIL) viewModel.backToStart() else onBack()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            when (state.step) {
                AuthStep.CHOOSE_START -> ChooseStartStep(viewModel = viewModel)
                AuthStep.EMAIL -> EmailStep(state = state, viewModel = viewModel)
                AuthStep.CODE -> CodeStep(state = state, viewModel = viewModel)
                AuthStep.SET_PASSWORD -> SetPasswordStep(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ChooseStartStep(viewModel: AuthViewModel) {
    Text(
        text = "How would you like to start?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Start with a free trial, or pick a plan now and pay straight away via EFT.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(24.dp))

    StartOptionCard(
        title = "Free 7-Day Trial",
        subtitle = "Try SheshaBiz free for 7 days — no payment needed",
        priceLabel = "Recommended",
        highlighted = true,
        onClick = { viewModel.selectStart(null) }
    )
    Spacer(Modifier.height(12.dp))

    SubscriptionPlan.entries.forEach { plan ->
        StartOptionCard(
            title = plan.displayName,
            subtitle = plan.periodLabel,
            priceLabel = plan.priceLabel,
            highlighted = false,
            onClick = { viewModel.selectStart(plan) }
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StartOptionCard(
    title: String,
    subtitle: String,
    priceLabel: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            priceLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmailStep(state: AuthUiState, viewModel: AuthViewModel) {
    Text(
        text = if (state.isReturningUser) "Log in" else "Create your account",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = if (state.isReturningUser) {
            "Enter your email and password."
        } else {
            "Set a password so you're always signed in — even after reinstalling or switching devices."
        },
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

    QQTextField(
        value = state.password,
        onValueChange = viewModel::onPasswordChange,
        label = "Password",
        keyboardType = KeyboardType.Password,
        isError = state.passwordError != null,
        errorText = state.passwordError,
        visualTransformation = PasswordVisualTransformation()
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

    if (state.isReturningUser) {
        QQPrimaryButton(
            text = "Log in",
            onClick = viewModel::loginWithPassword,
            loading = state.isPasswordLoggingIn
        )
        Spacer(Modifier.height(8.dp))
        QQTextActionButton(text = "New here? Create an account", onClick = viewModel::toggleReturningUser)
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = viewModel::sendCode) {
            Text(
                text = "Forgot your password? Log in with a code instead",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        QQPrimaryButton(
            text = "Create account",
            onClick = viewModel::createAccount,
            loading = state.isCreatingAccount
        )
        Spacer(Modifier.height(8.dp))
        QQTextActionButton(text = "Already have an account? Log in", onClick = viewModel::toggleReturningUser)
    }
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

@Composable
private fun SetPasswordStep(state: AuthUiState, viewModel: AuthViewModel) {
    Text(
        text = "Set a password (optional)",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Skip the wait next time — set a password now and log in instantly on any device.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(24.dp))

    QQTextField(
        value = state.newPassword,
        onValueChange = viewModel::onNewPasswordChange,
        label = "New password",
        keyboardType = KeyboardType.Password,
        visualTransformation = PasswordVisualTransformation()
    )
    Spacer(Modifier.height(12.dp))

    QQTextField(
        value = state.newPasswordConfirm,
        onValueChange = viewModel::onNewPasswordConfirmChange,
        label = "Confirm password",
        keyboardType = KeyboardType.Password,
        isError = state.passwordError != null,
        errorText = state.passwordError,
        visualTransformation = PasswordVisualTransformation()
    )
    Spacer(Modifier.height(16.dp))

    QQPrimaryButton(
        text = "Save password",
        onClick = viewModel::savePassword,
        loading = state.isSavingPassword
    )
    Spacer(Modifier.height(8.dp))

    QQTextActionButton(text = "Skip for now", onClick = viewModel::skipPassword)
}
