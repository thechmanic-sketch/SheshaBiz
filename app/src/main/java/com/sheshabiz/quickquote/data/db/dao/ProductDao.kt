package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sheshabiz.quickquote.data.db.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): Product?

    /** Unfiltered by [Product.deletedAt] — used by [com.sheshabiz.quickquote.data.sync.SyncManager]
     * so tombstoned rows still get pushed. */
    @Query("SELECT * FROM products")
    suspend fun getAllForSync(): List<Product>

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :amount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun decrementStock(id: Long, amount: Double, updatedAt: Long)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}
