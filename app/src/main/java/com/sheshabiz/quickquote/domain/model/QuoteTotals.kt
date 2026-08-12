package com.sheshabiz.quickquote.domain.model

data class QuoteTotals(
    val subtotal: Double,
    val discountAmount: Double,
    val vatAmount: Double,
    val total: Double
)
