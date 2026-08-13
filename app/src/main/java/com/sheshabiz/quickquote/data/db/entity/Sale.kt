package com.sheshabiz.quickquote.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A completed point-of-sale transaction, paid at the time of sale. */
@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("customerId"), Index("saleNumber", unique = true)]
)
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleNumber: String,
    val customerId: Long?,
    val customerName: String,
    val saleDate: Long,
    val vatEnabled: Boolean,
    val vatRate: Double,
    val discountType: DiscountType,
    val discountValue: Double,
    val subtotal: Double,
    val discountAmount: Double,
    val vatAmount: Double,
    val total: Double,
    val paymentMethod: PaymentMethod,
    val notes: String?,
    val createdAt: Long
)
