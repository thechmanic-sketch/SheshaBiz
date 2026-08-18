package com.sheshabiz.quickquote.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.sheshabiz.quickquote.domain.TrialGate

/** Shown wherever a "create new" / "edit" action is blocked by [TrialGate]. There are two
 * distinct reasons a user can land here — see [TrialGate.LockReason] — and they are NOT the
 * same problem: a never-logged-in user hasn't started their trial yet, while a lapsed user has
 * used up their trial. Each gets its own title/body/button/destination so the dialog always
 * leads somewhere actionable instead of a dead end. Every gated ViewModel exposes the reason
 * via its own uiState (rather than a boolean flag) so this can be driven directly by
 * `state.lockedReason?.let { ... }` at each call site. */
@Composable
fun TrialLockedDialog(
    reason: TrialGate.LockReason,
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val (title, message, confirmLabel, onConfirm) = when (reason) {
        TrialGate.LockReason.LOGGED_OUT -> DialogContent(
            title = "Start your free trial",
            message = TrialGate.LOGGED_OUT_MESSAGE,
            confirmLabel = "Sign up",
            onConfirm = onNavigateToLogin
        )
        TrialGate.LockReason.LAPSED -> DialogContent(
            title = "Trial ended",
            message = TrialGate.LAPSED_MESSAGE,
            confirmLabel = "Subscribe",
            onConfirm = onNavigateToSubscription
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

private data class DialogContent(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val onConfirm: () -> Unit
)
