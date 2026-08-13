package com.sheshabiz.quickquote.domain.model

import com.sheshabiz.quickquote.data.db.entity.Sale
import com.sheshabiz.quickquote.data.db.entity.SaleItem

data class SaleWithItems(
    val sale: Sale,
    val items: List<SaleItem>
)
