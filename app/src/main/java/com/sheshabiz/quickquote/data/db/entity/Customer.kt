package com.sheshabiz.quickquote.data.db.entity

import androidx.room.ColumnInfo
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
     * sync pull is free to fill in each customer's real timestamp.
     *
     * [ColumnInfo.defaultValue] must match MIGRATION_5_6's `DEFAULT 0` exactly — without it,
     * Room's compile-time schema (no default) disagrees with the actual migrated column
     * (default '0'), and Room throws on database open ("Migration didn't properly handle...").
     * A Kotlin default parameter value alone is invisible to Room's schema validation. */
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0,
    val syncId: String? = null,
    val deletedAt: Long? = null
)
