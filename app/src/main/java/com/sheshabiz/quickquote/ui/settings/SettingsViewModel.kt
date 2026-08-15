package com.sheshabiz.quickquote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.domain.BackupService
import com.sheshabiz.quickquote.domain.Country
import com.sheshabiz.quickquote.domain.ReminderScheduler
import com.sheshabiz.quickquote.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val businessProfile: BusinessProfile? = null,
    val quotePrefix: String = AppPreferences.DEFAULT_PREFIX,
    val invoicePrefix: String = AppPreferences.DEFAULT_INVOICE_PREFIX,
    val receiptPrefix: String = AppPreferences.DEFAULT_SALE_PREFIX,
    val vatEnabledDefault: Boolean = true,
    val vatRate: String = AppPreferences.DEFAULT_VAT_RATE.toString(),
    val paymentTerms: String = AppPreferences.DEFAULT_PAYMENT_TERMS,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val overdueRemindersEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val country: Country = Country.SOUTH_AFRICA
)

class SettingsViewModel(
    private val businessRepository: BusinessRepository,
    private val preferences: AppPreferences,
    private val backupService: BackupService,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val quotePrefix = MutableStateFlow(preferences.quoteNumberPrefix)
    private val invoicePrefix = MutableStateFlow(preferences.invoiceNumberPrefix)
    private val receiptPrefix = MutableStateFlow(preferences.saleNumberPrefix)
    private val vatEnabledDefault = MutableStateFlow(preferences.defaultVatEnabled)
    private val vatRate = MutableStateFlow(preferences.vatRate.let(::formatNumber))
    private val paymentTerms = MutableStateFlow(preferences.defaultPaymentTerms)

    val uiState: StateFlow<SettingsUiState> = combine(
        businessRepository.observeProfile(),
        preferences.themeMode,
        preferences.overdueRemindersEnabled,
        combine(
            quotePrefix, invoicePrefix, receiptPrefix, vatEnabledDefault,
            combine(vatRate, paymentTerms) { r, t -> r to t }
        ) { qp, ip, rp, v, rt ->
            listOf(qp, ip, rp, v, rt.first, rt.second)
        },
        combine(preferences.appLockEnabled, preferences.biometricEnabled, preferences.country) { lockEnabled, bioEnabled, country ->
            Triple(lockEnabled, bioEnabled, country)
        }
    ) { profile, theme, remindersEnabled, fields, lockState ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            businessProfile = profile,
            quotePrefix = fields[0] as String,
            invoicePrefix = fields[1] as String,
            receiptPrefix = fields[2] as String,
            vatEnabledDefault = fields[3] as Boolean,
            vatRate = fields[4] as String,
            paymentTerms = fields[5] as String,
            themeMode = theme,
            overdueRemindersEnabled = remindersEnabled,
            appLockEnabled = lockState.first,
            biometricEnabled = lockState.second,
            country = lockState.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun onOverdueRemindersChange(enabled: Boolean) {
        preferences.setOverdueRemindersEnabled(enabled)
        if (enabled) reminderScheduler.start() else reminderScheduler.stop()
    }

    fun onDisableAppLock() = preferences.disableAppLock()

    fun onBiometricChange(enabled: Boolean) = preferences.setBiometricEnabled(enabled)

    fun onCountryChange(country: Country) = preferences.setCountry(country)

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    fun onPrefixChange(value: String) {
        val prefix = value.uppercase().ifBlank { AppPreferences.DEFAULT_PREFIX }
        preferences.quoteNumberPrefix = prefix
        quotePrefix.value = prefix
    }

    fun onInvoicePrefixChange(value: String) {
        val prefix = value.uppercase().ifBlank { AppPreferences.DEFAULT_INVOICE_PREFIX }
        preferences.invoiceNumberPrefix = prefix
        invoicePrefix.value = prefix
    }

    fun onReceiptPrefixChange(value: String) {
        val prefix = value.uppercase().ifBlank { AppPreferences.DEFAULT_SALE_PREFIX }
        preferences.saleNumberPrefix = prefix
        receiptPrefix.value = prefix
    }

    fun onVatDefaultChange(enabled: Boolean) {
        preferences.defaultVatEnabled = enabled
        vatEnabledDefault.value = enabled
    }

    fun onVatRateChange(value: String) {
        vatRate.value = value
        value.toDoubleOrNull()?.let { preferences.vatRate = it }
    }

    fun onPaymentTermsChange(value: String) {
        preferences.defaultPaymentTerms = value
        paymentTerms.value = value
    }

    fun onThemeModeChange(mode: AppThemeMode) {
        preferences.setThemeMode(mode)
    }

    suspend fun exportData(): String? = runCatching { backupService.exportJson() }.getOrNull()

    suspend fun importData(json: String): Boolean = runCatching { backupService.importJson(json) }.isSuccess
}
