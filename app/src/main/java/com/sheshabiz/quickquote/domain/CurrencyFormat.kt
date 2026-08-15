package com.sheshabiz.quickquote.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Backed by a Compose [mutableStateOf] rather than a plain var: this object is called
 * directly from dozens of Composables (`CurrencyFormat.format(...)`) instead of being
 * threaded through ViewModel state, so the symbol needs to be a snapshot-tracked value
 * for switching country (Settings) to recompose every screen showing an amount.
 */
object CurrencyFormat {
    private val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
    private val decimalFormat = DecimalFormat("#,##0.00", symbols)

    var currencySymbol: String by mutableStateOf(Country.SOUTH_AFRICA.currencySymbol)
        private set

    fun setCountry(country: Country) {
        currencySymbol = country.currencySymbol
    }

    fun format(amount: Double): String = "$currencySymbol${decimalFormat.format(amount)}"

    fun formatDate(epochMillis: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(java.util.Date(epochMillis))
    }
}
