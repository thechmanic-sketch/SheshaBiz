package com.sheshabiz.quickquote.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sheshabiz.quickquote.data.db.dao.BusinessProfileDao
import com.sheshabiz.quickquote.data.db.dao.CustomerDao
import com.sheshabiz.quickquote.data.db.dao.InvoiceDao
import com.sheshabiz.quickquote.data.db.dao.InvoiceItemDao
import com.sheshabiz.quickquote.data.db.dao.ProductDao
import com.sheshabiz.quickquote.data.db.dao.QuoteDao
import com.sheshabiz.quickquote.data.db.dao.QuoteItemDao
import com.sheshabiz.quickquote.data.db.dao.SaleDao
import com.sheshabiz.quickquote.data.db.dao.SaleItemDao
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.Invoice
import com.sheshabiz.quickquote.data.db.entity.InvoiceItem
import com.sheshabiz.quickquote.data.db.entity.Product
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteItem
import com.sheshabiz.quickquote.data.db.entity.Sale
import com.sheshabiz.quickquote.data.db.entity.SaleItem

@Database(
    entities = [
        BusinessProfile::class, Customer::class,
        Quote::class, QuoteItem::class,
        Invoice::class, InvoiceItem::class,
        Product::class, Sale::class, SaleItem::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun customerDao(): CustomerDao
    abstract fun quoteDao(): QuoteDao
    abstract fun quoteItemDao(): QuoteItemDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao

    companion object {
        const val DATABASE_NAME = "quickquote.db"
    }
}
