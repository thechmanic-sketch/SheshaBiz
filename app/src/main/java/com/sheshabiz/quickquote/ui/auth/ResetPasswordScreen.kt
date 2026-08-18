package com.sheshabiz.quickquote.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenHeader

/**
 * Landing screen for the `sheshabiz://reset-password` deep link: two password fields (mirroring
 * [SetPasswordStep]'s validation exactly), then [onDone] hands off to the caller once the new
 * password is saved and the recovery session is persisted — see [ResetPasswordViewModel].
 *
 * Reached only via a cold-start or already-running deep link, never from in-app navigation —
 * there's nowhere sensible to "back" to, so this has no back button.
 */
@Composable
fun ResetPasswordScreen(
    viewModel: ResetPasswordViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Set new password")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Choose a new password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Enter and confirm a new password for your account.",
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

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
            }

            QQPrimaryButton(
                text = "Set new password",
                onClick = viewModel::savePassword,
                loading = state.isSaving
            )
        }
    }
}
