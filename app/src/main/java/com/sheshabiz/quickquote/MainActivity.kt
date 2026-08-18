package com.sheshabiz.quickquote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.sheshabiz.quickquote.ui.navigation.QuickQuoteNavHost
import com.sheshabiz.quickquote.ui.theme.QuickQuoteTheme
import org.json.JSONObject

/** Recovery-session tokens parsed out of a `sheshabiz://reset-password` deep link — see
 * [MainActivity.parseRecoveryTokens]. Supabase's password-recovery email redirects here with
 * `access_token`/`refresh_token`/`type=recovery` appended as a URL fragment (its documented
 * implicit-grant redirect shape). [email] is best-effort, decoded from the access token's JWT
 * payload since Supabase's recovery redirect doesn't include it directly. */
data class PasswordRecoveryTokens(
    val accessToken: String,
    val refreshToken: String,
    val email: String?
)

/** Extends FragmentActivity (not just ComponentActivity) because androidx.biometric.BiometricPrompt
 * requires one for the app-lock biometric unlock flow. */
class MainActivity : FragmentActivity() {

    /** Non-null only right after a `sheshabiz://reset-password` deep link arrives — read once
     * by [QuickQuoteNavHost] to route into [com.sheshabiz.quickquote.ui.auth.ResetPasswordScreen].
     * Backed by Compose's [mutableStateOf] (not a plain var) so recomposition picks up a token
     * update delivered via [onNewIntent] while the app is already running. */
    private var recoveryTokens by mutableStateOf<PasswordRecoveryTokens?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        recoveryTokens = parseRecoveryTokens(intent)

        val container = appContainer()

        setContent {
            val themeMode by container.preferences.themeMode.collectAsState()
            QuickQuoteTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    QuickQuoteNavHost(container = container, recoveryTokens = recoveryTokens)
                }
            }
        }
    }

    /** Handles the deep link arriving while the app is already running — the manifest's
     * `singleTask` launch mode on this activity routes it here instead of spawning a second
     * instance; see the `sheshabiz://reset-password` intent filter in AndroidManifest.xml. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseRecoveryTokens(intent)?.let { recoveryTokens = it }
    }

    /** One-off cross-device sync pass whenever the app comes to the foreground — covers the
     * initial launch too, since onResume always follows onCreate. No-ops for a logged-out
     * user (see [com.sheshabiz.quickquote.data.sync.SyncManager.sync]). */
    override fun onResume() {
        super.onResume()
        appContainer().syncScheduler.syncNow()
    }

    /** Parses a `sheshabiz://reset-password` link. Supabase's recovery redirect appends the
     * tokens as a URL fragment (`#access_token=...&refresh_token=...&type=recovery`) rather
     * than a query string, so both are checked — query params first, then the fragment —
     * matching whichever shape the actual redirect uses. Returns null for any other intent
     * (e.g. a normal launcher start, or a link missing the tokens it needs). */
    private fun parseRecoveryTokens(intent: Intent?): PasswordRecoveryTokens? {
        val uri = intent?.data ?: return null
        if (uri.scheme != "sheshabiz" || uri.host != "reset-password") return null

        val params = mutableMapOf<String, String>()
        runCatching {
            uri.queryParameterNames.forEach { name -> uri.getQueryParameter(name)?.let { params[name] = it } }
        }
        uri.fragment?.split("&")?.forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = pair.substring(0, idx)
                val rawValue = pair.substring(idx + 1)
                params[key] = runCatching { Uri.decode(rawValue) }.getOrDefault(rawValue)
            }
        }

        val accessToken = params["access_token"] ?: return null
        val refreshToken = params["refresh_token"] ?: return null
        return PasswordRecoveryTokens(accessToken, refreshToken, extractEmailFromJwt(accessToken))
    }

    /** Best-effort decode of the `email` claim out of the access token's JWT payload (the
     * middle, base64url-encoded segment) — Supabase's recovery redirect doesn't include the
     * email directly, and [com.sheshabiz.quickquote.data.prefs.AuthPreferences.saveSession]
     * wants one. Returns null on any parse failure; callers treat that as "unknown for now". */
    private fun extractEmailFromJwt(accessToken: String): String? = runCatching {
        val payload = accessToken.split(".").getOrNull(1) ?: return@runCatching null
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        JSONObject(String(decoded, Charsets.UTF_8)).optString("email").takeIf { it.isNotBlank() }
    }.getOrNull()
}
