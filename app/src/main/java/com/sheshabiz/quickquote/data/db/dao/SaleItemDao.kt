package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sheshabiz.quickquote.data.db.entity.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY sortOrder ASC")
    fun observeForSale(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY sortOrder ASC")
    suspend fun getForSale(saleId: Long): List<SaleItem>

    @Insert
    suspend fun insertAll(items: List<SaleItem>)
}
