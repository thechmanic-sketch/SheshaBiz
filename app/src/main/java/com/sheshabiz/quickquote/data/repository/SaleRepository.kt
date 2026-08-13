package com.sheshabiz.quickquote.data.repository

import androidx.room.withTransaction
import com.sheshabiz.quickquote.data.db.AppDatabase
import com.sheshabiz.quickquote.data.db.dao.ProductDao
import com.sheshabiz.quickquote.data.db.dao.SaleDao
import com.sheshabiz.quickquote.data.db.dao.SaleItemDao
import com.sheshabiz.quickquote.data.db.entity.Sale
import com.sheshabiz.quickquote.data.db.entity.SaleItem
import com.sheshabiz.quickquote.domain.model.SaleWithItems
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SaleRepository(
    private val database: AppDatabase,
    private val saleDao: SaleDao,
    private val itemDao: SaleItemDao,
    private val productDao: ProductDao
) {
    fun observeAll(): Flow<List<Sale>> = saleDao.observeAll()

    suspend fun getAllOnce(): List<Sale> = saleDao.observeAll().first()

    suspend fun getById(id: Long): Sale? = saleDao.getById(id)

    suspend fun getItemsForSale(saleId: Long): List<SaleItem> = itemDao.getForSale(saleId)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSaleWithItems(saleId: Long): Flow<SaleWithItems?> =
        saleDao.observeById(saleId).flatMapLatest { sale ->
            if (sale == null) flowOf(null) else itemDao.observeForSale(saleId).map { items -> SaleWithItems(sale, items) }
        }

    /** Records a sale and, for any item linked to a stock-tracked product, decrements its stock in the same transaction. */
    suspend fun completeSale(sale: Sale, items: List<SaleItem>): Long = database.withTransaction {
        val newId = saleDao.insert(sale)
        val indexedItems = items.mapIndexed { index, item -> item.copy(id = 0, saleId = newId, sortOrder = index) }
        itemDao.insertAll(indexedItems)
        val now = System.currentTimeMillis()
        indexedItems.forEach { item ->
            val productId = item.productId
            if (productId != null) {
                val product = productDao.getById(productId)
                if (product != null && product.trackStock) {
                    productDao.decrementStock(productId, item.quantity, now)
                }
            }
        }
        newId
    }

    suspend fun deleteSale(sale: Sale) = saleDao.delete(sale)

    suspend fun deleteAll() = saleDao.deleteAll()
}
