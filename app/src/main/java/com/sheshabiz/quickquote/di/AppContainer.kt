package com.sheshabiz.quickquote.di

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.sheshabiz.quickquote.data.db.AppDatabase
import com.sheshabiz.quickquote.data.db.MIGRATION_1_2
import com.sheshabiz.quickquote.data.db.MIGRATION_2_3
import com.sheshabiz.quickquote.data.db.MIGRATION_3_4
import com.sheshabiz.quickquote.data.db.MIGRATION_4_5
import com.sheshabiz.quickquote.data.db.MIGRATION_5_6
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.remote.SupabaseAuthClient
import com.sheshabiz.quickquote.data.remote.SupabaseRestClient
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.data.repository.InvoiceRepository
import com.sheshabiz.quickquote.data.repository.ProductRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository
import com.sheshabiz.quickquote.data.repository.SaleRepository
import com.sheshabiz.quickquote.data.sync.SyncManager
import com.sheshabiz.quickquote.data.sync.SyncScheduler
import com.sheshabiz.quickquote.domain.BackupService
import com.sheshabiz.quickquote.domain.DemoDataSeeder
import com.sheshabiz.quickquote.domain.PdfGenerator
import com.sheshabiz.quickquote.domain.ReminderScheduler
import com.sheshabiz.quickquote.domain.copyLogoToInternalStorage
import com.sheshabiz.quickquote.domain.copyProductImageToInternalStorage

/** Simple hand-rolled dependency container — no DI framework needed for an app this size. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()

    val preferences = AppPreferences(appContext)
    val authPreferences = AuthPreferences(appContext)
    val supabaseAuthClient = SupabaseAuthClient
    val supabaseRestClient = SupabaseRestClient(authPreferences)
    val businessRepository = BusinessRepository(database.businessProfileDao())
    val customerRepository = CustomerRepository(database.customerDao())
    val quoteRepository = QuoteRepository(database, database.quoteDao(), database.quoteItemDao())
    val invoiceRepository = InvoiceRepository(database, database.invoiceDao(), database.invoiceItemDao())
    val productRepository = ProductRepository(database.productDao())
    val saleRepository = SaleRepository(database, database.saleDao(), database.saleItemDao(), database.productDao())
    val pdfGenerator = PdfGenerator(appContext)
    val demoDataSeeder = DemoDataSeeder(businessRepository, customerRepository, quoteRepository, preferences)
    val backupService = BackupService(
        businessRepository, customerRepository, quoteRepository, invoiceRepository, productRepository, saleRepository
    )
    val reminderScheduler = ReminderScheduler(appContext)
    val syncManager = SyncManager(database, supabaseRestClient, authPreferences)
    val syncScheduler = SyncScheduler(appContext)

    suspend fun copyLogo(uri: Uri): String? = copyLogoToInternalStorage(appContext, uri)
    suspend fun copyProductImage(uri: Uri): String? = copyProductImageToInternalStorage(appContext, uri)
}
