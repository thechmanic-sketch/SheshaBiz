package com.sheshabiz.quickquote.domain

import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.domain.model.QuoteTotals
import java.math.BigDecimal
import java.math.RoundingMode

data class LineItemInput(val quantity: Double, val unitPrice: Double)

object QuoteCalculator {

    fun lineTotal(quantity: Double, unitPrice: Double): Double =
        round2(quantity * unitPrice)

    fun calculate(
        items: List<LineItemInput>,
        vatEnabled: Boolean,
        vatRatePercent: Double,
        discountType: DiscountType,
        discountValue: Double
    ): QuoteTotals {
        val subtotal = round2(items.sumOf { it.quantity * it.unitPrice })

        val rawDiscount = when (discountType) {
            DiscountType.PERCENT -> subtotal * (discountValue.coerceIn(0.0, 100.0) / 100.0)
            DiscountType.FIXED -> discountValue.coerceAtLeast(0.0)
        }
        val discountAmount = round2(rawDiscount.coerceIn(0.0, subtotal))

        val afterDiscount = subtotal - discountAmount
        val vatAmount = if (vatEnabled) round2(afterDiscount * (vatRatePercent / 100.0)) else 0.0
        val total = round2(afterDiscount + vatAmount)

        return QuoteTotals(
            subtotal = subtotal,
            discountAmount = discountAmount,
            vatAmount = vatAmount,
            total = total
        )
    }

    fun round2(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}
