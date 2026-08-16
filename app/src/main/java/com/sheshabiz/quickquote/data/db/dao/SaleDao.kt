package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sheshabiz.quickquote.data.db.entity.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    fun observeById(id: Long): Flow<Sale?>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): Sale?

    /** Unfiltered by [Sale.deletedAt] — used by [com.sheshabiz.quickquote.data.sync.SyncManager]
     * so tombstoned rows still get pushed. */
    @Query("SELECT * FROM sales")
    suspend fun getAllForSync(): List<Sale>

    @Insert
    suspend fun insert(sale: Sale): Long

    /** Sales are otherwise immutable once completed (no edit UI exists) — this exists only
     * so [com.sheshabiz.quickquote.data.sync.SyncManager] can stamp a freshly generated
     * [Sale.syncId] onto a row before pushing it. */
    @Update
    suspend fun update(sale: Sale)

    @Delete
    suspend fun delete(sale: Sale)

    @Query("DELETE FROM sales")
    suspend fun deleteAll()
}
