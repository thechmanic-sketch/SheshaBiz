package com.sheshabiz.quickquote.data.db

import androidx.room.TypeConverter
import com.sheshabiz.quickquote.data.db.entity.DiscountType
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
}
