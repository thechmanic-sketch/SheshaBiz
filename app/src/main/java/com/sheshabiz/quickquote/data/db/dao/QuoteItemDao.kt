package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sheshabiz.quickquote.data.db.entity.QuoteItem
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteItemDao {
    @Query("SELECT * FROM quote_items WHERE quoteId = :quoteId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeForQuote(quoteId: Long): Flow<List<QuoteItem>>

    @Query("SELECT * FROM quote_items WHERE quoteId = :quoteId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    suspend fun getForQuote(quoteId: Long): List<QuoteItem>

    @Query("SELECT * FROM quote_items WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): QuoteItem?

    /** Unfiltered, across every quote — used by [com.sheshabiz.quickquote.data.sync.SyncManager]. */
    @Query("SELECT * FROM quote_items")
    suspend fun getAllForSync(): List<QuoteItem>

    @Insert
    suspend fun insertAll(items: List<QuoteItem>)

    /** Single-row insert, for [com.sheshabiz.quickquote.data.sync.SyncManager] pulling one
     * remote item at a time rather than replacing a quote's whole item set. */
    @Insert
    suspend fun insert(item: QuoteItem): Long

    @Update
    suspend fun update(item: QuoteItem)

    @Query("DELETE FROM quote_items WHERE quoteId = :quoteId")
    suspend fun deleteForQuote(quoteId: Long)
}
