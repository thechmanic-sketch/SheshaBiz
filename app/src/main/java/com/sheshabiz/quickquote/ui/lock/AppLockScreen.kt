package com.sheshabiz.quickquote.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.sheshabiz.quickquote.data.prefs.AppPreferences

@Composable
fun AppLockScreen(
    preferences: AppPreferences,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val biometricEnabled by preferences.biometricEnabled.collectAsState()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val onUnlockedState = rememberUpdatedState(onUnlocked)

    fun tryBiometric() {
        val activity = context as? FragmentActivity ?: return
        val canAuthenticate = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) return

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlockedState.value()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock SheshaBiz")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) tryBiometric()
    }

    fun onDigit(digit: String) {
        if (pin.length >= 4) return
        error = false
        val next = pin + digit
        pin = next
        if (next.length == 4) {
            if (preferences.verifyPin(next)) {
                onUnlockedState.value()
            } else {
                error = true
                pin = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter your PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (error) "Incorrect PIN — try again" else " ",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        PinDots(filled = pin.length)
        Spacer(Modifier.height(40.dp))
        PinKeypad(
            onDigit = ::onDigit,
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            showBiometric = biometricEnabled,
            onBiometric = ::tryBiometric
        )
    }
}
