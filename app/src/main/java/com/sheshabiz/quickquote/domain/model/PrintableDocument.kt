package com.sheshabiz.quickquote.domain.model

import com.sheshabiz.quickquote.data.db.entity.DiscountType

enum class DocumentKind(val label: String, val fileNamePrefix: String) {
    QUOTATION("QUOTATION", "Quote"),
    INVOICE("INVOICE", "Invoice"),
    RECEIPT("RECEIPT", "Receipt")
}

data class PrintableItem(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double,
    val sortOrder: Int
)

/** Shape shared by quotes and invoices so [com.sheshabiz.quickquote.domain.PdfGenerator] renders both from one code path. */
data class PrintableDocument(
    val kind: DocumentKind,
    val number: String,
    val primaryDate: Long,
    val secondaryDateLabel: String,
    val secondaryDate: Long,
    val secondaryValueText: String? = null,
    val statusLabel: String?,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String?,
    val customerAddress: String?,
    val items: List<PrintableItem>,
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
    val amountTendered: Double? = null,
    val changeGiven: Double? = null
)
