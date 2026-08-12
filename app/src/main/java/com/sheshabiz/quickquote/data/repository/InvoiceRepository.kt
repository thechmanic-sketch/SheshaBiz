package com.sheshabiz.quickquote.data.repository

import androidx.room.withTransaction
import com.sheshabiz.quickquote.data.db.AppDatabase
import com.sheshabiz.quickquote.data.db.dao.InvoiceDao
import com.sheshabiz.quickquote.data.db.dao.InvoiceItemDao
import com.sheshabiz.quickquote.data.db.entity.Invoice
import com.sheshabiz.quickquote.data.db.entity.InvoiceItem
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus
import com.sheshabiz.quickquote.domain.model.InvoiceWithItems
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class InvoiceRepository(
    private val database: AppDatabase,
    private val invoiceDao: InvoiceDao,
    private val itemDao: InvoiceItemDao
) {
    fun observeAll(): Flow<List<Invoice>> = invoiceDao.observeAll()

    suspend fun getAllOnce(): List<Invoice> = invoiceDao.observeAll().first()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeInvoiceWithItems(invoiceId: Long): Flow<InvoiceWithItems?> =
        invoiceDao.observeById(invoiceId).flatMapLatest { invoice ->
            if (invoice == null) {
                flowOf(null)
            } else {
                itemDao.observeForInvoice(invoiceId).map { items -> InvoiceWithItems(invoice, items) }
            }
        }

    suspend fun getById(id: Long): Invoice? = invoiceDao.getById(id)

    suspend fun getItemsForInvoice(invoiceId: Long): List<InvoiceItem> = itemDao.getForInvoice(invoiceId)

    suspend fun createInvoice(invoice: Invoice, items: List<InvoiceItem>): Long = database.withTransaction {
        val newId = invoiceDao.insert(invoice)
        itemDao.insertAll(items.mapIndexed { index, item ->
            item.copy(id = 0, invoiceId = newId, sortOrder = index)
        })
        newId
    }

    suspend fun updateInvoice(invoice: Invoice, items: List<InvoiceItem>) = database.withTransaction {
        invoiceDao.update(invoice)
        itemDao.deleteForInvoice(invoice.id)
        itemDao.insertAll(items.mapIndexed { index, item ->
            item.copy(id = 0, invoiceId = invoice.id, sortOrder = index)
        })
    }

    suspend fun deleteInvoice(invoice: Invoice) = invoiceDao.delete(invoice)

    suspend fun deleteAll() = invoiceDao.deleteAll()

    suspend fun updateStatus(id: Long, status: InvoiceStatus, paidAt: Long?, updatedAt: Long) =
        invoiceDao.updateStatus(id, status, paidAt, updatedAt)
}
