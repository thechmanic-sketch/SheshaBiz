package com.sheshabiz.quickquote.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Customer fields are snapshotted onto the quote at creation time (rather than only
 * joined via [customerId]) so a quote keeps showing the details it was sent with even
 * if the customer record is later edited or removed.
 */
@Entity(
    tableName = "quotes",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("customerId"), Index("quoteNumber", unique = true)]
)
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quoteNumber: String,
    val customerId: Long?,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String?,
    val customerAddress: String?,
    val quoteDate: Long,
    val validUntil: Long,
    val vatEnabled: Boolean,
    val vatRate: Double,
    val discountType: DiscountType,
    val discountValue: Double,
    val subtotal: Double,
    val discountAmount: Double,
    val vatAmount: Double,
    val total: Double,
    val notes: String?,
    val paymentTerms: String?,
    val status: QuoteStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncId: String? = null,
    val deletedAt: Long? = null
)
