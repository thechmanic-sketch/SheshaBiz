package com.sheshabiz.quickquote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table: the business owner's own company details, shown on every quote. */
@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val businessName: String,
    val ownerName: String,
    val phone: String,
    val whatsappNumber: String,
    val email: String,
    val address: String,
    val vatNumber: String?,
    val registrationNumber: String? = null,
    val logoUri: String?,
    val bankName: String? = null,
    val accountHolder: String? = null,
    val accountNumber: String? = null,
    val branchCode: String? = null,
    val accountType: String? = null
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
