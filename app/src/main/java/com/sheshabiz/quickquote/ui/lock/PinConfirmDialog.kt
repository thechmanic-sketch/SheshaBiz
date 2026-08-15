package com.sheshabiz.quickquote.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sheshabiz.quickquote.data.prefs.AppPreferences

/**
 * PIN gate shown before a destructive action (delete, data import/overwrite) proceeds.
 * If no PIN has been set up yet, offers to set one up instead of silently allowing the
 * action through — "any deletion needs a password" only holds if there's always a PIN to check.
 */
@Composable
fun PinConfirmDialog(
    preferences: AppPreferences,
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit,
    onNeedsSetup: () -> Unit,
    title: String = "Confirm with your PIN",
    message: String = "Enter your PIN to authorise this action."
) {
    val hasPin = remember { preferences.hasPin() }

    if (!hasPin) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set up a PIN first") },
            text = { Text("Deleting requires a security PIN. Set one up in Settings > Security, then try again.") },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    onNeedsSetup()
                }) { Text("Set up PIN") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
        return
    }

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun onDigit(digit: String) {
        if (pin.length >= 4) return
        error = false
        val next = pin + digit
        pin = next
        if (next.length == 4) {
            if (preferences.verifyPin(next)) {
                onConfirmed()
            } else {
                error = true
                pin = ""
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (error) "Incorrect PIN — try again" else " ",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            PinDots(filled = pin.length)
            Spacer(Modifier.height(24.dp))
            PinKeypad(
                onDigit = ::onDigit,
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    }
}
