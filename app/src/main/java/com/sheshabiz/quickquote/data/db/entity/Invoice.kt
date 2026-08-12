package com.sheshabiz.quickquote.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Customer fields are snapshotted (same rationale as [Quote]) so an invoice keeps showing
 * what it was sent with even if the customer record changes later. [sourceQuoteId] is set
 * when the invoice was generated from a quote via "Convert to Invoice"; it is null for
 * invoices created directly.
 */
@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Quote::class,
            parentColumns = ["id"],
            childColumns = ["sourceQuoteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("customerId"), Index("sourceQuoteId"), Index("invoiceNumber", unique = true)]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val sourceQuoteId: Long?,
    val customerId: Long?,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String?,
    val customerAddress: String?,
    val invoiceDate: Long,
    val dueDate: Long,
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
    val status: InvoiceStatus,
    val paidAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
