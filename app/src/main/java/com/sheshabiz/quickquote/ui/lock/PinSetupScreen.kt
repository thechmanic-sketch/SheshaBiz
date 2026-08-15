package com.sheshabiz.quickquote.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.data.prefs.AppPreferences

private enum class PinSetupStep { ENTER, CONFIRM }

@Composable
fun PinSetupScreen(
    preferences: AppPreferences,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableStateOf(PinSetupStep.ENTER) }
    var firstPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun onDigit(digit: String) {
        if (pin.length >= 4) return
        error = null
        val next = pin + digit
        pin = next
        if (next.length == 4) {
            when (step) {
                PinSetupStep.ENTER -> {
                    firstPin = next
                    pin = ""
                    step = PinSetupStep.CONFIRM
                }
                PinSetupStep.CONFIRM -> {
                    if (next == firstPin) {
                        preferences.setPin(next)
                        onDone()
                    } else {
                        error = "PINs didn't match — try again"
                        pin = ""
                        firstPin = ""
                        step = PinSetupStep.ENTER
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (step == PinSetupStep.ENTER) "Choose a 4-digit PIN" else "Confirm your PIN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = error ?: " ",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            PinDots(filled = pin.length)
            Spacer(Modifier.height(40.dp))
            PinKeypad(
                onDigit = ::onDigit,
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
            )
        }
    }
}
