package com.sheshabiz.quickquote.data.repository

import com.sheshabiz.quickquote.data.db.dao.ProductDao
import com.sheshabiz.quickquote.data.db.entity.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProductRepository(private val dao: ProductDao) {
    fun observeAll(): Flow<List<Product>> = dao.observeAll()

    suspend fun getAllOnce(): List<Product> = dao.observeAll().first()

    suspend fun getById(id: Long): Product? = dao.getById(id)

    suspend fun upsert(product: Product): Long =
        if (product.id == 0L) dao.insert(product) else {
            dao.update(product)
            product.id
        }

    suspend fun delete(product: Product) = dao.delete(product)

    suspend fun deleteAll() = dao.deleteAll()
}
