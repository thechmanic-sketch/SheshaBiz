package com.sheshabiz.quickquote.domain

import android.util.Patterns

object Validators {
    fun isBlank(value: String): Boolean = value.trim().isEmpty()

    fun isValidEmail(value: String): Boolean =
        value.isBlank() || Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()

    fun isValidPrice(value: String): Boolean =
        value.toDoubleOrNull()?.let { it >= 0.0 } ?: false

    fun isValidQuantity(value: String): Boolean =
        value.toDoubleOrNull()?.let { it > 0.0 } ?: false

    fun parsePriceOrZero(value: String): Double = value.toDoubleOrNull() ?: 0.0

    fun parseQuantityOrZero(value: String): Double = value.toDoubleOrNull() ?: 0.0
}
