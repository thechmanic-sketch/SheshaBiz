package com.sheshabiz.quickquote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String?,
    val address: String?,
    val createdAt: Long,
    /** Added in v6 alongside [syncId]/[deletedAt] so cross-device sync can tell whether a
     * local edit or the last-pulled remote copy is newer (LWW). Existing rows migrate to 0,
     * i.e. "older than anything the server has" — safe, since it just means the very first
     * sync pull is free to fill in each customer's real timestamp. */
    val updatedAt: Long = 0,
    val syncId: String? = null,
    val deletedAt: Long? = null
)
