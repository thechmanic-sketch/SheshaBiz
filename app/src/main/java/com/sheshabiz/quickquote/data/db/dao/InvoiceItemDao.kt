package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sheshabiz.quickquote.data.db.entity.InvoiceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY sortOrder ASC")
    fun observeForInvoice(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY sortOrder ASC")
    suspend fun getForInvoice(invoiceId: Long): List<InvoiceItem>

    @Insert
    suspend fun insertAll(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteForInvoice(invoiceId: Long)
}
