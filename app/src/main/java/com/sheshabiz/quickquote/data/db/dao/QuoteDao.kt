package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE id = :id")
    fun observeById(id: Long): Flow<Quote?>

    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getById(id: Long): Quote?

    @Query("SELECT * FROM quotes WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): Quote?

    /** Unfiltered by [Quote.deletedAt] — used by [com.sheshabiz.quickquote.data.sync.SyncManager]
     * so tombstoned rows still get pushed. */
    @Query("SELECT * FROM quotes")
    suspend fun getAllForSync(): List<Quote>

    @Insert
    suspend fun insert(quote: Quote): Long

    @Update
    suspend fun update(quote: Quote)

    @Delete
    suspend fun delete(quote: Quote)

    @Query("UPDATE quotes SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: QuoteStatus, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM quotes WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM quotes")
    suspend fun deleteAll()
}
