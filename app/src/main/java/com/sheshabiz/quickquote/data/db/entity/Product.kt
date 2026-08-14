package com.sheshabiz.quickquote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Doubles as the saved item/service catalog (for quick-filling quote and invoice line items)
 * and the POS product catalog. [trackStock] distinguishes a plain reusable service (no stock
 * concept, e.g. "Plumbing repair") from a stocked retail product whose [stockQuantity] is
 * decremented on each POS sale.
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unitPrice: Double,
    val sku: String?,
    val trackStock: Boolean,
    val stockQuantity: Double,
    val lowStockThreshold: Double?,
    val imageUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
