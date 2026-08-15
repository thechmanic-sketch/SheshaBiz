package com.sheshabiz.quickquote.domain.model

/** Values needed to render the printable end-of-day/period sales report PDF. */
data class ReportPdfData(
    val periodLabel: String,
    val generatedAt: Long,
    val salesTotal: Double,
    val salesCount: Int,
    val quotesCreated: Int,
    val quotesAccepted: Int,
    val quotesTotal: Double,
    val invoicesPaidTotal: Double,
    val invoicesPaidCount: Int,
    val invoicesUnpaidTotal: Double,
    val invoicesUnpaidCount: Int,
    val invoicesOverdueCount: Int
)
