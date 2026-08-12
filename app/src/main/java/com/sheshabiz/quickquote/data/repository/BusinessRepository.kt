package com.sheshabiz.quickquote.data.repository

import com.sheshabiz.quickquote.data.db.dao.BusinessProfileDao
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import kotlinx.coroutines.flow.Flow

class BusinessRepository(private val dao: BusinessProfileDao) {
    fun observeProfile(): Flow<BusinessProfile?> = dao.observe()

    suspend fun getProfile(): BusinessProfile? = dao.get()

    suspend fun saveProfile(profile: BusinessProfile) = dao.upsert(profile)
}
