package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sheshabiz.quickquote.data.db.entity.InvoiceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeForInvoice(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    suspend fun getForInvoice(invoiceId: Long): List<InvoiceItem>

    @Query("SELECT * FROM invoice_items WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): InvoiceItem?

    /** Unfiltered, across every invoice — used by [com.sheshabiz.quickquote.data.sync.SyncManager]. */
    @Query("SELECT * FROM invoice_items")
    suspend fun getAllForSync(): List<InvoiceItem>

    @Insert
    suspend fun insertAll(items: List<InvoiceItem>)

    /** Single-row insert, for [com.sheshabiz.quickquote.data.sync.SyncManager] pulling one
     * remote item at a time rather than replacing an invoice's whole item set. */
    @Insert
    suspend fun insert(item: InvoiceItem): Long

    @Update
    suspend fun update(item: InvoiceItem)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteForInvoice(invoiceId: Long)
}
