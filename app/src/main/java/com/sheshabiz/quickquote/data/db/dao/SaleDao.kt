package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.sheshabiz.quickquote.data.db.entity.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    fun observeById(id: Long): Flow<Sale?>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): Sale?

    @Insert
    suspend fun insert(sale: Sale): Long

    @Delete
    suspend fun delete(sale: Sale)

    @Query("DELETE FROM sales")
    suspend fun deleteAll()
}
