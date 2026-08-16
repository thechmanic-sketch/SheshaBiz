package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sheshabiz.quickquote.data.db.entity.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeForSale(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    suspend fun getForSale(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): SaleItem?

    /** Unfiltered, across every sale — used by [com.sheshabiz.quickquote.data.sync.SyncManager]. */
    @Query("SELECT * FROM sale_items")
    suspend fun getAllForSync(): List<SaleItem>

    @Insert
    suspend fun insertAll(items: List<SaleItem>)

    /** Single-row insert, for [com.sheshabiz.quickquote.data.sync.SyncManager] pulling one
     * remote item at a time. */
    @Insert
    suspend fun insert(item: SaleItem): Long

    /** Only used by [com.sheshabiz.quickquote.data.sync.SyncManager] to stamp a freshly
     * generated [SaleItem.syncId] onto a row before pushing it — sale items are otherwise
     * immutable once created. */
    @Update
    suspend fun update(item: SaleItem)
}
