package com.sheshabiz.quickquote.di

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.sheshabiz.quickquote.data.db.AppDatabase
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository
import com.sheshabiz.quickquote.domain.BackupService
import com.sheshabiz.quickquote.domain.DemoDataSeeder
import com.sheshabiz.quickquote.domain.PdfGenerator
import com.sheshabiz.quickquote.domain.copyLogoToInternalStorage

/** Simple hand-rolled dependency container — no DI framework needed for an app this size. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    ).build()

    val preferences = AppPreferences(appContext)
    val businessRepository = BusinessRepository(database.businessProfileDao())
    val customerRepository = CustomerRepository(database.customerDao())
    val quoteRepository = QuoteRepository(database.quoteDao(), database.quoteItemDao())
    val pdfGenerator = PdfGenerator(appContext)
    val demoDataSeeder = DemoDataSeeder(businessRepository, customerRepository, quoteRepository, preferences)
    val backupService = BackupService(businessRepository, customerRepository, quoteRepository)

    suspend fun copyLogo(uri: Uri): String? = copyLogoToInternalStorage(appContext, uri)
}
