package com.sheshabiz.quickquote.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormat {
    private val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
    private val format = DecimalFormat("#,##0.00", symbols)

    fun format(amount: Double): String = "R${format.format(amount)}"

    fun formatDate(epochMillis: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(java.util.Date(epochMillis))
    }
}
