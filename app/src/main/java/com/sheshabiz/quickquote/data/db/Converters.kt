package com.sheshabiz.quickquote.data.db

import androidx.room.TypeConverter
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus
import com.sheshabiz.quickquote.data.db.entity.PaymentMethod
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus

class Converters {
    @TypeConverter
    fun fromQuoteStatus(value: QuoteStatus): String = value.name

    @TypeConverter
    fun toQuoteStatus(value: String): QuoteStatus = QuoteStatus.valueOf(value)

    @TypeConverter
    fun fromDiscountType(value: DiscountType): String = value.name

    @TypeConverter
    fun toDiscountType(value: String): DiscountType = DiscountType.valueOf(value)

    @TypeConverter
    fun fromInvoiceStatus(value: InvoiceStatus): String = value.name

    @TypeConverter
    fun toInvoiceStatus(value: String): InvoiceStatus = InvoiceStatus.valueOf(value)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)
}
