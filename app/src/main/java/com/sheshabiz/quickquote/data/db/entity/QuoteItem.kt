package com.sheshabiz.quickquote.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quote_items",
    foreignKeys = [
        ForeignKey(
            entity = Quote::class,
            parentColumns = ["id"],
            childColumns = ["quoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quoteId")]
)
data class QuoteItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quoteId: Long,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double,
    val sortOrder: Int,
    val syncId: String? = null,
    val deletedAt: Long? = null
)
