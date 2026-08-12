package com.sheshabiz.quickquote.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = ${BusinessProfile.SINGLETON_ID} LIMIT 1")
    fun observe(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = ${BusinessProfile.SINGLETON_ID} LIMIT 1")
    suspend fun get(): BusinessProfile?

    @Upsert
    suspend fun upsert(profile: BusinessProfile)
}
