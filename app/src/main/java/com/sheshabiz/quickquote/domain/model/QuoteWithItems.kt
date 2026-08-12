package com.sheshabiz.quickquote.domain.model

import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteItem

data class QuoteWithItems(
    val quote: Quote,
    val items: List<QuoteItem>
)
