package com.sheshabiz.quickquote.domain.model

import com.sheshabiz.quickquote.data.db.entity.Invoice
import com.sheshabiz.quickquote.data.db.entity.InvoiceItem

data class InvoiceWithItems(
    val invoice: Invoice,
    val items: List<InvoiceItem>
)
