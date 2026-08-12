package com.sheshabiz.quickquote.data.repository

import com.sheshabiz.quickquote.data.db.dao.QuoteDao
import com.sheshabiz.quickquote.data.db.dao.QuoteItemDao
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteItem
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.domain.model.QuoteWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class QuoteRepository(
    private val quoteDao: QuoteDao,
    private val itemDao: QuoteItemDao
) {
    fun observeAll(): Flow<List<Quote>> = quoteDao.observeAll()

    suspend fun getAllOnce(): List<Quote> = quoteDao.observeAll().first()

    fun observeCount(): Flow<Int> = quoteDao.observeCount()

    fun observeItemsForQuote(quoteId: Long): Flow<List<QuoteItem>> =
        itemDao.observeForQuote(quoteId)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeQuoteWithItems(quoteId: Long): Flow<QuoteWithItems?> =
        quoteDao.observeById(quoteId).flatMapLatest { quote ->
            if (quote == null) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                itemDao.observeForQuote(quoteId).map { items -> QuoteWithItems(quote, items) }
            }
        }

    fun observeAllWithItems(): Flow<List<QuoteWithItems>> =
        quoteDao.observeAll().flatMapLatest { quotes ->
            if (quotes.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                combine(quotes.map { quote -> itemDao.observeForQuote(quote.id).map { quote to it } }) { pairs ->
                    pairs.map { (quote, items) -> QuoteWithItems(quote, items) }
                }
            }
        }

    suspend fun getById(id: Long): Quote? = quoteDao.getById(id)

    suspend fun getItemsForQuote(quoteId: Long): List<QuoteItem> = itemDao.getForQuote(quoteId)

    suspend fun createQuote(quote: Quote, items: List<QuoteItem>): Long {
        val newId = quoteDao.insert(quote)
        itemDao.insertAll(items.mapIndexed { index, item ->
            item.copy(id = 0, quoteId = newId, sortOrder = index)
        })
        return newId
    }

    suspend fun updateQuote(quote: Quote, items: List<QuoteItem>) {
        quoteDao.update(quote)
        itemDao.deleteForQuote(quote.id)
        itemDao.insertAll(items.mapIndexed { index, item ->
            item.copy(id = 0, quoteId = quote.id, sortOrder = index)
        })
    }

    suspend fun deleteQuote(quote: Quote) = quoteDao.delete(quote)

    suspend fun deleteAll() = quoteDao.deleteAll()

    suspend fun updateStatus(id: Long, status: QuoteStatus, updatedAt: Long) =
        quoteDao.updateStatus(id, status, updatedAt)
}
