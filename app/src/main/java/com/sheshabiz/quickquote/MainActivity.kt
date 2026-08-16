package com.sheshabiz.quickquote

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.sheshabiz.quickquote.ui.navigation.QuickQuoteNavHost
import com.sheshabiz.quickquote.ui.theme.QuickQuoteTheme

/** Extends FragmentActivity (not just ComponentActivity) because androidx.biometric.BiometricPrompt
 * requires one for the app-lock biometric unlock flow. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = appContainer()

        setContent {
            val themeMode by container.preferences.themeMode.collectAsState()
            QuickQuoteTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    QuickQuoteNavHost(container = container)
                }
            }
        }
    }

    /** One-off cross-device sync pass whenever the app comes to the foreground — covers the
     * initial launch too, since onResume always follows onCreate. No-ops for a logged-out
     * user (see [com.sheshabiz.quickquote.data.sync.SyncManager.sync]). */
    override fun onResume() {
        super.onResume()
        appContainer().syncScheduler.syncNow()
    }
}
