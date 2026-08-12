package com.sheshabiz.quickquote.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.sheshabiz.quickquote.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Small app-settings store backed by SharedPreferences. Values are mirrored into
 * StateFlows so Compose screens can observe changes (e.g. theme, VAT defaults)
 * without re-reading SharedPreferences on every recomposition.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode

    private val _onboardingComplete = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete

    private val _businessSetupComplete = MutableStateFlow(prefs.getBoolean(KEY_BUSINESS_SETUP_DONE, false))
    val businessSetupComplete: StateFlow<Boolean> = _businessSetupComplete

    fun setOnboardingComplete(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
        _onboardingComplete.value = done
    }

    fun setBusinessSetupComplete(done: Boolean) {
        prefs.edit().putBoolean(KEY_BUSINESS_SETUP_DONE, done).apply()
        _businessSetupComplete.value = done
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun readThemeMode(): AppThemeMode =
        prefs.getString(KEY_THEME_MODE, null)?.let {
            runCatching { AppThemeMode.valueOf(it) }.getOrNull()
        } ?: AppThemeMode.SYSTEM

    var quoteNumberPrefix: String
        get() = prefs.getString(KEY_QUOTE_PREFIX, DEFAULT_PREFIX) ?: DEFAULT_PREFIX
        set(value) = prefs.edit().putString(KEY_QUOTE_PREFIX, value.ifBlank { DEFAULT_PREFIX }).apply()

    var nextQuoteNumber: Int
        get() = prefs.getInt(KEY_NEXT_NUMBER, 1)
        set(value) = prefs.edit().putInt(KEY_NEXT_NUMBER, value).apply()

    /** Reserves and returns the next quote number, formatted with the configured prefix. */
    fun reserveNextQuoteNumber(): String {
        val current = nextQuoteNumber
        nextQuoteNumber = current + 1
        return "%s-%04d".format(quoteNumberPrefix, current)
    }

    var defaultVatEnabled: Boolean
        get() = prefs.getBoolean(KEY_VAT_DEFAULT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VAT_DEFAULT_ENABLED, value).apply()

    var vatRate: Double
        get() = prefs.getFloat(KEY_VAT_RATE, DEFAULT_VAT_RATE.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_VAT_RATE, value.toFloat()).apply()

    var defaultPaymentTerms: String
        get() = prefs.getString(KEY_PAYMENT_TERMS, DEFAULT_PAYMENT_TERMS) ?: DEFAULT_PAYMENT_TERMS
        set(value) = prefs.edit().putString(KEY_PAYMENT_TERMS, value).apply()

    val currencySymbol: String get() = "R"
    val currencyCode: String get() = "ZAR"

    companion object {
        private const val PREFS_NAME = "quickquote_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_BUSINESS_SETUP_DONE = "business_setup_done"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_QUOTE_PREFIX = "quote_prefix"
        private const val KEY_NEXT_NUMBER = "next_quote_number"
        private const val KEY_VAT_DEFAULT_ENABLED = "vat_default_enabled"
        private const val KEY_VAT_RATE = "vat_rate"
        private const val KEY_PAYMENT_TERMS = "payment_terms"
        const val DEFAULT_PREFIX = "Q"
        const val DEFAULT_VAT_RATE = 15.0
        const val DEFAULT_PAYMENT_TERMS = "Payment due within 7 days."
    }
}
