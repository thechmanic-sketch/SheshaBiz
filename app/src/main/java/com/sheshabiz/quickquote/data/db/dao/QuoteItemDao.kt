package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sheshabiz.quickquote.data.db.entity.QuoteItem
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteItemDao {
    @Query("SELECT * FROM quote_items WHERE quoteId = :quoteId ORDER BY sortOrder ASC")
    fun observeForQuote(quoteId: Long): Flow<List<QuoteItem>>

    @Query("SELECT * FROM quote_items WHERE quoteId = :quoteId ORDER BY sortOrder ASC")
    suspend fun getForQuote(quoteId: Long): List<QuoteItem>

    @Insert
    suspend fun insertAll(items: List<QuoteItem>)

    @Query("DELETE FROM quote_items WHERE quoteId = :quoteId")
    suspend fun deleteForQuote(quoteId: Long)
}
