package com.sheshabiz.quickquote.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.sheshabiz.quickquote.domain.TrialGate

/** Shown wherever a "create new" / "edit" action is blocked because the account's trial has
 * lapsed. Every gated ViewModel exposes the message via its own uiState (rather than a
 * boolean flag) so this can be driven directly by `if (state.lockedMessage != null) { ... }`
 * at each call site. [onSubscribe] sends the user to the in-app subscription screen so the
 * lock leads somewhere actionable instead of a dead end. */
@Composable
fun TrialLockedDialog(onDismiss: () -> Unit, onSubscribe: () -> Unit, message: String = TrialGate.LOCKED_MESSAGE) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trial ended") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onSubscribe) { Text("Subscribe") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
