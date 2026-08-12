package com.sheshabiz.quickquote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.domain.BackupService
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
    val vatEnabledDefault: Boolean = true,
    val vatRate: String = AppPreferences.DEFAULT_VAT_RATE.toString(),
    val paymentTerms: String = AppPreferences.DEFAULT_PAYMENT_TERMS,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

class SettingsViewModel(
    private val businessRepository: BusinessRepository,
    private val preferences: AppPreferences,
    private val backupService: BackupService
) : ViewModel() {

    private val quotePrefix = MutableStateFlow(preferences.quoteNumberPrefix)
    private val invoicePrefix = MutableStateFlow(preferences.invoiceNumberPrefix)
    private val vatEnabledDefault = MutableStateFlow(preferences.defaultVatEnabled)
    private val vatRate = MutableStateFlow(preferences.vatRate.let(::formatNumber))
    private val paymentTerms = MutableStateFlow(preferences.defaultPaymentTerms)

    val uiState: StateFlow<SettingsUiState> = combine(
        businessRepository.observeProfile(),
        preferences.themeMode,
        combine(quotePrefix, invoicePrefix, vatEnabledDefault, vatRate, paymentTerms) { qp, ip, v, r, t ->
            listOf(qp, ip, v, r, t)
        }
    ) { profile, theme, fields ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            businessProfile = profile,
            quotePrefix = fields[0] as String,
            invoicePrefix = fields[1] as String,
            vatEnabledDefault = fields[2] as Boolean,
            vatRate = fields[3] as String,
            paymentTerms = fields[4] as String,
            themeMode = theme
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

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
