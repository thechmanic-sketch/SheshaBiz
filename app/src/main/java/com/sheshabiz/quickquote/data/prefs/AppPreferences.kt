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

    private val _overdueRemindersEnabled = MutableStateFlow(prefs.getBoolean(KEY_OVERDUE_REMINDERS_ENABLED, false))
    val overdueRemindersEnabled: StateFlow<Boolean> = _overdueRemindersEnabled

    fun setOverdueRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERDUE_REMINDERS_ENABLED, enabled).apply()
        _overdueRemindersEnabled.value = enabled
    }

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
        } ?: AppThemeMode.LIGHT

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

    var invoiceNumberPrefix: String
        get() = prefs.getString(KEY_INVOICE_PREFIX, DEFAULT_INVOICE_PREFIX) ?: DEFAULT_INVOICE_PREFIX
        set(value) = prefs.edit().putString(KEY_INVOICE_PREFIX, value.ifBlank { DEFAULT_INVOICE_PREFIX }).apply()

    var nextInvoiceNumber: Int
        get() = prefs.getInt(KEY_NEXT_INVOICE_NUMBER, 1)
        set(value) = prefs.edit().putInt(KEY_NEXT_INVOICE_NUMBER, value).apply()

    /** Reserves and returns the next invoice number, formatted with the configured prefix. */
    fun reserveNextInvoiceNumber(): String {
        val current = nextInvoiceNumber
        nextInvoiceNumber = current + 1
        return "%s-%04d".format(invoiceNumberPrefix, current)
    }

    var saleNumberPrefix: String
        get() = prefs.getString(KEY_SALE_PREFIX, DEFAULT_SALE_PREFIX) ?: DEFAULT_SALE_PREFIX
        set(value) = prefs.edit().putString(KEY_SALE_PREFIX, value.ifBlank { DEFAULT_SALE_PREFIX }).apply()

    var nextSaleNumber: Int
        get() = prefs.getInt(KEY_NEXT_SALE_NUMBER, 1)
        set(value) = prefs.edit().putInt(KEY_NEXT_SALE_NUMBER, value).apply()

    /** Reserves and returns the next POS sale/receipt number, formatted with the configured prefix. */
    fun reserveNextSaleNumber(): String {
        val current = nextSaleNumber
        nextSaleNumber = current + 1
        return "%s-%04d".format(saleNumberPrefix, current)
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
        private const val KEY_INVOICE_PREFIX = "invoice_prefix"
        private const val KEY_NEXT_INVOICE_NUMBER = "next_invoice_number"
        private const val KEY_SALE_PREFIX = "sale_prefix"
        private const val KEY_NEXT_SALE_NUMBER = "next_sale_number"
        private const val KEY_VAT_DEFAULT_ENABLED = "vat_default_enabled"
        private const val KEY_VAT_RATE = "vat_rate"
        private const val KEY_PAYMENT_TERMS = "payment_terms"
        private const val KEY_OVERDUE_REMINDERS_ENABLED = "overdue_reminders_enabled"
        const val DEFAULT_PREFIX = "Q"
        const val DEFAULT_INVOICE_PREFIX = "INV"
        const val DEFAULT_SALE_PREFIX = "R"
        const val DEFAULT_VAT_RATE = 15.0
        const val DEFAULT_PAYMENT_TERMS = "Payment due within 7 days."
    }
}
