package com.sheshabiz.quickquote.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sheshabiz.quickquote.data.db.dao.BusinessProfileDao
import com.sheshabiz.quickquote.data.db.dao.CustomerDao
import com.sheshabiz.quickquote.data.db.dao.InvoiceDao
import com.sheshabiz.quickquote.data.db.dao.InvoiceItemDao
import com.sheshabiz.quickquote.data.db.dao.QuoteDao
import com.sheshabiz.quickquote.data.db.dao.QuoteItemDao
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.Invoice
import com.sheshabiz.quickquote.data.db.entity.InvoiceItem
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteItem

@Database(
    entities = [
        BusinessProfile::class, Customer::class,
        Quote::class, QuoteItem::class,
        Invoice::class, InvoiceItem::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun customerDao(): CustomerDao
    abstract fun quoteDao(): QuoteDao
    abstract fun quoteItemDao(): QuoteItemDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao

    companion object {
        const val DATABASE_NAME = "quickquote.db"
    }
}
