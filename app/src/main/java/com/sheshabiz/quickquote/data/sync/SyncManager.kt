package com.sheshabiz.quickquote.data.sync

import com.sheshabiz.quickquote.data.db.AppDatabase
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.data.db.entity.Invoice
import com.sheshabiz.quickquote.data.db.entity.InvoiceItem
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus
import com.sheshabiz.quickquote.data.db.entity.PaymentMethod
import com.sheshabiz.quickquote.data.db.entity.Product
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteItem
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.data.db.entity.Sale
import com.sheshabiz.quickquote.data.db.entity.SaleItem
import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.remote.SupabaseRestClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Cross-device sync: push-then-pull, parent tables before children
 * (`business_profiles`/`customers`/`products` -> `quotes`/`invoices`/`sales` -> `*_items`),
 * talking straight to Room's DAOs the same way [com.sheshabiz.quickquote.domain.OverdueInvoiceCheckWorker]
 * does — repositories are left alone entirely.
 *
 * Two entity shapes are handled differently:
 *  - **Editable** entities (business profile, customers, products, quotes, invoices) can be
 *    changed locally after creation, so every local row is re-pushed each pass and a pulled
 *    remote row only overwrites the local one when the remote `updated_at` is strictly newer
 *    (last-write-wins).
 *  - **Write-once** entities (sales, and every `*_items` table — nothing in this app ever
 *    calls their DAOs' `update`/edits after creation; a quote/invoice *edit* deletes and
 *    reinserts its whole item set instead) only ever need a syncId assigned once and pushed
 *    once, and a pulled row is only ever inserted, never merged into an existing local one.
 *
 * Known limitation: because item rows are wholesale deleted-and-reinserted by
 * [com.sheshabiz.quickquote.data.repository.QuoteRepository.updateQuote] /
 * [com.sheshabiz.quickquote.data.repository.InvoiceRepository.updateInvoice] on every edit
 * (untouched here, per this feature's scope), an edited quote/invoice's *previous* item rows
 * lose their syncId before this class ever sees them, so their remote counterparts are never
 * tombstoned — an edited document can leave orphaned old line-item rows on the server. Local
 * deletes are similarly hard deletes today (not soft, so no tombstone is ever produced for a
 * deleted quote/invoice/customer/product/sale) — a deletion on one device does not yet remove
 * that record on another. Both are pre-existing repository behaviours this feature's scope
 * doesn't touch; the `deletedAt` column and every pull path already honour a tombstone should
 * one arrive from the server (e.g. an admin edit), so wiring true soft-deletes through the
 * repositories later needs no further schema or sync-engine changes.
 */
class SyncManager(
    private val database: AppDatabase,
    private val restClient: SupabaseRestClient,
    private val authPreferences: AuthPreferences
) {
    /** No-op entirely for a user who has never logged in — the whole point of this class is
     * invisible to someone using the app fully offline. Safe to call from multiple triggers
     * (login, connectivity regained, app foreground, the periodic worker); each call runs to
     * completion sequentially and just does nothing if a previous call already covered it. */
    suspend fun sync() {
        if (!authPreferences.isLoggedIn.value) return

        // Idempotent: creates the caller's `businesses` row on first-ever call (starting the
        // trial clock), and transparently covers an already-logged-in user from the
        // login-only feature that shipped before this one existed and has no `businesses`
        // row yet.
        val businessId = restClient.bootstrapBusiness() ?: return
        authPreferences.setBusinessId(businessId)

        val since = authPreferences.lastSyncedAt.value ?: 0L
        val passStartedAt = System.currentTimeMillis()

        runCatching { syncBusinessProfile(businessId) }
        runCatching { syncCustomers(businessId, since) }
        runCatching { syncProducts(businessId, since) }
        runCatching { syncQuotes(businessId, since) }
        runCatching { syncInvoices(businessId, since) }
        runCatching { syncSales(businessId, since) }
        runCatching { syncQuoteItems(businessId, since) }
        runCatching { syncInvoiceItems(businessId, since) }
        runCatching { syncSaleItems(businessId, since) }

        // Only advance the cursor once every table has had a chance to run — a single
        // entity's failure (network blip mid-pass) just means it's retried in full next time.
        authPreferences.setLastSyncedAt(passStartedAt)

        val status = restClient.getMyBusinessStatus()
        if (status != null) {
            authPreferences.updateSubscriptionStatus(status.status, status.trialStartedAt, status.validUntil)
        }
    }

    // ---------------------------------------------------------------------
    // business_profiles — singleton row, no local updatedAt to run LWW against.
    // ---------------------------------------------------------------------

    private suspend fun syncBusinessProfile(businessId: String) {
        val dao = database.businessProfileDao()
        val local = dao.get()
        if (local != null) {
            val row = if (local.syncId == null) local.copy(syncId = businessId) else local
            if (row.syncId != local.syncId) dao.upsert(row)
            val json = JSONObject().apply {
                put0("business_id", businessId)
                put0("business_name", row.businessName)
                put0("owner_name", row.ownerName)
                put0("phone", row.phone)
                put0("whatsapp_number", row.whatsappNumber)
                put0("email", row.email)
                put0("address", row.address)
                put0("vat_number", row.vatNumber)
                put0("registration_number", row.registrationNumber)
                put0("logo_uri", row.logoUri)
                put0("bank_name", row.bankName)
                put0("account_holder", row.accountHolder)
                put0("account_number", row.accountNumber)
                put0("branch_code", row.branchCode)
                put0("account_type", row.accountType)
                put0("updated_at", SupabaseRestClient.isoString(System.currentTimeMillis()))
            }
            restClient.post("business_profiles", json.toString(), mergeHeader)
        } else {
            val (code, body) = restClient.get("business_profiles?business_id=eq.$businessId&limit=1")
            if (code !in 200..299 || body.isNullOrBlank()) return
            val remote = JSONArray(body).optJSONObject(0) ?: return
            dao.upsert(
                BusinessProfile(
                    businessName = remote.stringOrNull("business_name") ?: "",
                    ownerName = remote.stringOrNull("owner_name") ?: "",
                    phone = remote.stringOrNull("phone") ?: "",
                    whatsappNumber = remote.stringOrNull("whatsapp_number") ?: "",
                    email = remote.stringOrNull("email") ?: "",
                    address = remote.stringOrNull("address") ?: "",
                    vatNumber = remote.stringOrNull("vat_number"),
                    registrationNumber = remote.stringOrNull("registration_number"),
                    logoUri = remote.stringOrNull("logo_uri"),
                    bankName = remote.stringOrNull("bank_name"),
                    accountHolder = remote.stringOrNull("account_holder"),
                    accountNumber = remote.stringOrNull("account_number"),
                    branchCode = remote.stringOrNull("branch_code"),
                    accountType = remote.stringOrNull("account_type"),
                    syncId = businessId
                )
            )
        }
    }

    // ---------------------------------------------------------------------
    // customers
    // ---------------------------------------------------------------------

    private suspend fun syncCustomers(businessId: String, since: Long) {
        val dao = database.customerDao()

        dao.getAllForSync().forEach { customer ->
            val syncId = customer.syncId ?: UUID.randomUUID().toString().also { newId ->
                dao.update(customer.copy(syncId = newId))
            }
            val row = if (customer.syncId == null) customer.copy(syncId = syncId) else customer
            val json = JSONObject().apply {
                put0("id", syncId)
                put0("business_id", businessId)
                put0("name", row.name)
                put0("phone", row.phone)
                put0("email", row.email)
                put0("address", row.address)
                put0("created_at", SupabaseRestClient.isoString(row.createdAt))
                put0("updated_at", SupabaseRestClient.isoString(row.updatedAt))
                put0("deleted_at", row.deletedAt?.let(SupabaseRestClient::isoString))
            }
            restClient.post("customers", json.toString(), mergeHeader)
        }

        val (code, body) = restClient.get("customers?business_id=eq.$businessId&updated_at=gt.${SupabaseRestClient.isoString(since)}")
        if (code !in 200..299 || body.isNullOrBlank()) return
        val rows = JSONArray(body)
        for (i in 0 until rows.length()) {
            val remote = rows.getJSONObject(i)
            val remoteSyncId = remote.stringOrNull("id") ?: continue
            val remoteUpdatedAt = SupabaseRestClient.parseTimestamp(remote.opt("updated_at")) ?: continue
            val local = dao.getBySyncId(remoteSyncId)
            val deletedAt = SupabaseRestClient.parseTimestamp(remote.opt("deleted_at"))
            if (local != null) {
                if (remoteUpdatedAt <= local.updatedAt) continue
                dao.update(
                    local.copy(
                        name = remote.stringOrNull("name") ?: local.name,
                        phone = remote.stringOrNull("phone") ?: local.phone,
                        email = remote.stringOrNull("email"),
                        address = remote.stringOrNull("address"),
                        updatedAt = remoteUpdatedAt,
                        deletedAt = deletedAt
                    )
                )
            } else {
                dao.insert(
                    Customer(
                        name = remote.stringOrNull("name") ?: "",
                        phone = remote.stringOrNull("phone") ?: "",
                        email = remote.stringOrNull("email"),
                        address = remote.stringOrNull("address"),
                        createdAt = SupabaseRestClient.parseTimestamp(remote.opt("created_at")) ?: remoteUpdatedAt,
                        updatedAt = remoteUpdatedAt,
                        syncId = remoteSyncId,
                        deletedAt = deletedAt
                    )
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // products
    // ---------------------------------------------------------------------

    private suspend fun syncProducts(businessId: String, since: Long) {
        val dao = database.productDao()

        dao.getAllForSync().forEach { product ->
            val syncId = product.syncId ?: UUID.randomUUID().toString().also { newId ->
                dao.update(product.copy(syncId = newId))
            }
            val row = if (product.syncId == null) product.copy(syncId = syncId) else product
            val json = JSONObject().apply {
                put0("id", syncId)
                put0("business_id", businessId)
                put0("name", row.name)
                put0("unit_price", row.unitPrice)
                put0("sku", row.sku)
                put0("track_stock", row.trackStock)
                put0("stock_quantity", row.stockQuantity)
                put0("low_stock_threshold", row.lowStockThreshold)
                put0("image_uri", row.imageUri)
                put0("created_at", SupabaseRestClient.isoString(row.createdAt))
                put0("updated_at", SupabaseRestClient.isoString(row.updatedAt))
                put0("deleted_at", row.deletedAt?.let(SupabaseRestClient::isoString))
            }
            restClient.post("products", json.toString(), mergeHeader)
        }

        val (code, body) = restClient.get("products?business_id=eq.$businessId&updated_at=gt.${SupabaseRestClient.isoString(since)}")
        if (code !in 200..299 || body.isNullOrBlank()) return
        val rows = JSONArray(body)
        for (i in 0 until rows.length()) {
            val remote = rows.getJSONObject(i)
            val remoteSyncId = remote.stringOrNull("id") ?: continue
            val remoteUpdatedAt = SupabaseRestClient.parseTimestamp(remote.opt("updated_at")) ?: continue
            val local = dao.getBySyncId(remoteSyncId)
            val deletedAt = SupabaseRestClient.parseTimestamp(remote.opt("deleted_at"))
            if (local != null) {
                if (remoteUpdatedAt <= local.updatedAt) continue
                dao.update(
                    local.copy(
                        name = remote.stringOrNull("name") ?: local.name,
                        unitPrice = remote.doubleOrNull("unit_price") ?: local.unitPrice,
                        sku = remote.stringOrNull("sku"),
                        trackStock = remote.optBoolean("track_stock", local.trackStock),
                        stockQuantity = remote.doubleOrNull("stock_quantity") ?: local.stockQuantity,
                        lowStockThreshold = remote.doubleOrNull("low_stock_threshold"),
                        imageUri = remote.stringOrNull("image_uri") ?: local.imageUri,
                        updatedAt = remoteUpdatedAt,
                        deletedAt = deletedAt
                    )
                )
            } else {
                dao.insert(
                    Product(
                        name = remote.stringOrNull("name") ?: "",
                        unitPrice = remote.doubleOrNull("unit_price") ?: 0.0,
                        sku = remote.stringOrNull("sku"),
                        trackStock = remote.optBoolean("track_stock", false),
                        stockQuantity = remote.doubleOrNull("stock_quantity") ?: 0.0,
                        lowStockThreshold = remote.doubleOrNull("low_stock_threshold"),
                        imageUri = remote.stringOrNull("image_uri"),
                        createdAt = SupabaseRestClient.parseTimestamp(remote.opt("created_at")) ?: remoteUpdatedAt,
                        updatedAt = remoteUpdatedAt,
                        syncId = remoteSyncId,
                        deletedAt = deletedAt
                    )
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // quotes
    // ---------------------------------------------------------------------

    private suspend fun syncQuotes(businessId: String, since: Long) {
        val dao = database.quoteDao()

        dao.getAllForSync().forEach { quote ->
            val syncId = quote.syncId ?: UUID.randomUUID().toString().also { newId ->
                dao.update(quote.copy(syncId = newId))
            }
            val row = if (quote.syncId == null) quote.copy(syncId = syncId) else quote
            val json = JSONObject().apply {
                put0("id", syncId)
                put0("business_id", businessId)
                put0("quote_number", row.quoteNumber)
                put0("customer_id", customerSyncId(row.customerId))
                put0("customer_name", row.customerName)
                put0("customer_phone", row.customerPhone)
                put0("customer_email", row.customerEmail)
                put0("customer_address", row.customerAddress)
                put0("quote_date", SupabaseRestClient.isoString(row.quoteDate))
                put0("valid_until", SupabaseRestClient.isoString(row.validUntil))
                put0("vat_enabled", row.vatEnabled)
                put0("vat_rate", row.vatRate)
                put0("discount_type", row.discountType.name)
                put0("discount_value", row.discountValue)
                put0("subtotal", row.subtotal)
                put0("discount_amount", row.discountAmount)
                put0("vat_amount", row.vatAmount)
                put0("total", row.total)
                put0("notes", row.notes)
                put0("payment_terms", row.paymentTerms)
                put0("status", row.status.name)
                put0("created_at", SupabaseRestClient.isoString(row.createdAt))
                put0("updated_at", SupabaseRestClient.isoString(row.updatedAt))
                put0("deleted_at", row.deletedAt?.let(SupabaseRestClient::isoString))
            }
            restClient.post("quotes", json.toString(), mergeHeader)
        }

        val (code, body) = restClient.get("quotes?business_id=eq.$businessId&updated_at=gt.${SupabaseRestClient.isoString(since)}")
        if (code !in 200..299 || body.isNullOrBlank()) return
        val rows = JSONArray(body)
        for (i in 0 until rows.length()) {
            val remote = rows.getJSONObject(i)
            val remoteSyncId = remote.stringOrNull("id") ?: continue
            val remoteUpdatedAt = SupabaseRestClient.parseTimestamp(remote.opt("updated_at")) ?: continue
            val local = dao.getBySyncId(remoteSyncId)
            val discountType = parseDiscountType(remote.stringOrNull("discount_type"))
            val status = runCatching { QuoteStatus.valueOf(remote.stringOrNull("status") ?: "DRAFT") }
                .getOrDefault(QuoteStatus.DRAFT)
            val resolvedCustomerId = localCustomerId(remote.stringOrNull("customer_id"))
            val deletedAt = SupabaseRestClient.parseTimestamp(remote.opt("deleted_at"))
            if (local != null) {
                if (remoteUpdatedAt <= local.updatedAt) continue
                dao.update(
                    local.copy(
                        quoteNumber = remote.stringOrNull("quote_number") ?: local.quoteNumber,
                        customerId = resolvedCustomerId,
                        customerName = remote.stringOrNull("customer_name") ?: local.customerName,
                        customerPhone = remote.stringOrNull("customer_phone") ?: local.customerPhone,
                        customerEmail = remote.stringOrNull("customer_email"),
                        customerAddress = remote.stringOrNull("customer_address"),
                        quoteDate = SupabaseRestClient.parseTimestamp(remote.opt("quote_date")) ?: local.quoteDate,
                        validUntil = SupabaseRestClient.parseTimestamp(remote.opt("valid_until")) ?: local.validUntil,
                        vatEnabled = remote.optBoolean("vat_enabled", local.vatEnabled),
                        vatRate = remote.doubleOrNull("vat_rate") ?: local.vatRate,
                        discountType = discountType,
                        discountValue = remote.doubleOrNull("discount_value") ?: local.discountValue,
                        subtotal = remote.doubleOrNull("subtotal") ?: local.subtotal,
                        discountAmount = remote.doubleOrNull("discount_amount") ?: local.discountAmount,
                        vatAmount = remote.doubleOrNull("vat_amount") ?: local.vatAmount,
                        total = remote.doubleOrNull("total") ?: local.total,
                        notes = remote.stringOrNull("notes"),
                        paymentTerms = remote.stringOrNull("payment_terms"),
                        status = status,
                        updatedAt = remoteUpdatedAt,
                        deletedAt = deletedAt
                    )
                )
            } else {
                dao.insert(
                    Quote(
                        quoteNumber = remote.stringOrNull("quote_number") ?: "",
                        customerId = resolvedCustomerId,
                        customerName = remote.stringOrNull("customer_name") ?: "",
                        customerPhone = remote.stringOrNull("customer_phone") ?: "",
                        customerEmail = remote.stringOrNull("customer_email"),
                        customerAddress = remote.stringOrNull("customer_address"),
                        quoteDate = SupabaseRestClient.parseTimestamp(remote.opt("quote_date")) ?: remoteUpdatedAt,
                        validUntil = SupabaseRestClient.parseTimestamp(remote.opt("valid_until")) ?: remoteUpdatedAt,
                        vatEnabled = remote.optBoolean("vat_enabled", true),
                        vatRate = remote.doubleOrNull("vat_rate") ?: 0.0,
                        discountType = discountType,
                        discountValue = remote.doubleOrNull("discount_value") ?: 0.0,
                        subtotal = remote.doubleOrNull("subtotal") ?: 0.0,
                        discountAmount = remote.doubleOrNull("discount_amount") ?: 0.0,
                        vatAmount = remote.doubleOrNull("vat_amount") ?: 0.0,
                        total = remote.doubleOrNull("total") ?: 0.0,
                        notes = remote.stringOrNull("notes"),
                        paymentTerms = remote.stringOrNull("payment_terms"),
                        status = status,
                        createdAt = SupabaseRestClient.parseTimestamp(remote.opt("created_at")) ?: remoteUpdatedAt,
                        updatedAt = remoteUpdatedAt,
                        syncId = remoteSyncId,
                        deletedAt = deletedAt
                    )
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // invoices
    // ---------------------------------------------------------------------

    private suspend fun syncInvoices(businessId: String, since: Long) {
        val dao = database.invoiceDao()

        dao.getAllForSync().forEach { invoice ->
            val syncId = invoice.syncId ?: UUID.randomUUID().toString().also { newId ->
                dao.update(invoice.copy(syncId = newId))
            }
            val row = if (invoice.syncId == null) invoice.copy(syncId = syncId) else invoice
            val json = JSONObject().apply {
                put0("id", syncId)
                put0("business_id", businessId)
                put0("invoice_number", row.invoiceNumber)
                put0("source_quote_id", quoteSyncId(row.sourceQuoteId))
                put0("customer_id", customerSyncId(row.customerId))
                put0("customer_name", row.customerName)
                put0("customer_phone", row.customerPhone)
                put0("customer_email", row.customerEmail)
                put0("customer_address", row.customerAddress)
                put0("invoice_date", SupabaseRestClient.isoString(row.invoiceDate))
                put0("due_date", SupabaseRestClient.isoString(row.dueDate))
                put0("vat_enabled", row.vatEnabled)
                put0("vat_rate", row.vatRate)
                put0("discount_type", row.discountType.name)
                put0("discount_value", row.discountValue)
                put0("subtotal", row.subtotal)
                put0("discount_amount", row.discountAmount)
                put0("vat_amount", row.vatAmount)
                put0("total", row.total)
                put0("notes", row.notes)
                put0("payment_terms", row.paymentTerms)
                put0("status", row.status.name)
                put0("paid_at", row.paidAt?.let(SupabaseRestClient::isoString))
                put0("created_at", SupabaseRestClient.isoString(row.createdAt))
                put0("updated_at", SupabaseRestClient.isoString(row.updatedAt))
                put0("deleted_at", row.deletedAt?.let(SupabaseRestClient::isoString))
            }
            restClient.post("invoices", json.toString(), mergeHeader)
        }

        val (code, body) = restClient.get("invoices?business_id=eq.$businessId&updated_at=gt.${SupabaseRestClient.isoString(since)}")
        if (code !in 200..299 || body.isNullOrBlank()) return
        val rows = JSONArray(body)
        for (i in 0 until rows.length()) {
            val remote = rows.getJSONObject(i)
            val remoteSyncId = remote.stringOrNull("id") ?: continue
            val remoteUpdatedAt = SupabaseRestClient.parseTimestamp(remote.opt("updated_at")) ?: continue
            val local = dao.getBySyncId(remoteSyncId)
            val discountType = parseDiscountType(remote.stringOrNull("discount_type"))
            val status = runCatching { InvoiceStatus.valueOf(remote.stringOrNull("status") ?: "UNPAID") }
                .getOrDefault(InvoiceStatus.UNPAID)
            val resolvedCustomerId = localCustomerId(remote.stringOrNull("customer_id"))
            val resolvedSourceQuoteId = localQuoteId(remote.stringOrNull("source_quote_id"))
            val paidAt = SupabaseRestClient.parseTimestamp(remote.opt("paid_at"))
            val deletedAt = SupabaseRestClient.parseTimestamp(remote.opt("deleted_at"))
            if (local != null) {
                if (remoteUpdatedAt <= local.updatedAt) continue
                dao.update(
                    local.copy(
                        invoiceNumber = remote.stringOrNull("invoice_number") ?: local.invoiceNumber,
                        sourceQuoteId = resolvedSourceQuoteId,
                        customerId = resolvedCustomerId,
                        customerName = remote.stringOrNull("customer_name") ?: local.customerName,
                        customerPhone = remote.stringOrNull("customer_phone") ?: local.customerPhone,
                        customerEmail = remote.stringOrNull("customer_email"),
                        customerAddress = remote.stringOrNull("customer_address"),
                        invoiceDate = SupabaseRestClient.parseTimestamp(remote.opt("invoice_date")) ?: local.invoiceDate,
                        dueDate = SupabaseRestClient.parseTimestamp(remote.opt("due_date")) ?: local.dueDate,
                        vatEnabled = remote.optBoolean("vat_enabled", local.vatEnabled),
                        vatRate = remote.doubleOrNull("vat_rate") ?: local.vatRate,
                        discountType = discountType,
                        discountValue = remote.doubleOrNull("discount_value") ?: local.discountValue,
                        subtotal = remote.doubleOrNull("subtotal") ?: local.subtotal,
                        discountAmount = remote.doubleOrNull("discount_amount") ?: local.discountAmount,
                        vatAmount = remote.doubleOrNull("vat_amount") ?: local.vatAmount,
                        total = remote.doubleOrNull("total") ?: local.total,
                        notes = remote.stringOrNull("notes"),
                        paymentTerms = remote.stringOrNull("payment_terms"),
                        status = status,
                        paidAt = paidAt,
                        updatedAt = remoteUpdatedAt,
                        deletedAt = deletedAt
                    )
                )
            } else {
                dao.insert(
                    Invoice(
                        invoiceNumber = remote.stringOrNull("invoice_number") ?: "",
                        sourceQuoteId = resolvedSourceQuoteId,
                        customerId = resolvedCustomerId,
                        customerName = remote.stringOrNull("customer_name") ?: "",
                        customerPhone = remote.stringOrNull("customer_phone") ?: "",
                        customerEmail = remote.stringOrNull("customer_email"),
                        customerAddress = remote.stringOrNull("customer_address"),
                        invoiceDate = SupabaseRestClient.parseTimestamp(remote.opt("invoice_date")) ?: remoteUpdatedAt,
                        dueDate = SupabaseRestClient.parseTimestamp(remote.opt("due_date")) ?: remoteUpdatedAt,
                        vatEnabled = remote.optBoolean("vat_enabled", true),
                        vatRate = remote.doubleOrNull("vat_rate") ?: 0.0,
                        discountType = discountType,
                        discountValue = remote.doubleOrNull("discount_value") ?: 0.0,
                        subtotal = remote.doubleOrNull("subtotal") ?: 0.0,
                        discountAmount = remote.doubleOrNull("discount_amount") ?: 0.0,
                        vatAmount = remote.doubleOrNull("vat_amount") ?: 0.0,
                        total = remote.doubleOrNull("total") ?: 0.0,
                        notes = remote.stringOrNull("notes"),
                        paymentTerms = remote.stringOrNull("payment_terms"),
                        status = status,
                        paidAt = paidAt,
                        createdAt = SupabaseRestClient.parseTimestamp(remote.opt("created_at")) ?: remoteUpdatedAt,
                        updatedAt = remoteUpdatedAt,
                        syncId = remoteSyncId,
                        deletedAt = deletedAt
                    )
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // sales — write-once (no edit UI/DAO path exists for a completed sale).
    // ---------------------------------------------------------------------

    private suspend fun syncSales(businessId: String, since: Long) {
        val dao = database.saleDao()

        dao.getAllForSync().filter { it.syncId == null }.forEach { sale ->
            val syncId = UUID.randomUUID().toString()
            dao.update(sale.copy(syncId = syncId))
            val row = sale.copy(syncId = syncId)
            val json = JSONObject().apply {
                put0("id", syncId)
                put0("business_id", businessId)
                put0("sale_number", row.saleNumber)
                put0("customer_id", customerSyncId(row.customerId))
                put0("customer_name", row.customerName)
                put0("sale_date", SupabaseRestClient.isoString(row.saleDate))
                put0("vat_enabled", row.vatEnabled)
                put0("vat_rate", row.vatRate)
                put0("discount_type", row.discountType.name)
                put0("discount_value", row.discountValue)
                put0("subtotal", row.subtotal)
                put0("discount_amount", row.discountAmount)
                put0("vat_amount", row.vatAmount)
                put0("total", row.total)
                put0("payment_method", row.paymentMethod.name)
                put0("notes", row.notes)
                put0("amount_tendered", row.amountTendered)
                put0("change_given", row.changeGiven)
                put0("created_at", SupabaseRestClient.isoString(row.createdAt))
                // Sale has no local updatedAt (it's never edited after creation) — createdAt
                // doubles as its sync clock, which is exact since this row is only ever
                // pushed once, right here, the first time it's seen with syncId == null.
                put0("updated_at", SupabaseRestClient.isoString(row.createdAt))
                put0("deleted_at", row.deletedAt?.let(SupabaseRestClient::isoString))
            }
            restClient.post("sales", json.toString(), mergeHeader)
        }

        val (code, body) = restClient.get("sales?business_id=eq.$businessId&updated_at=gt.${SupabaseRestClient.isoString(since)}")
        if (code !in 200..299 || body.isNullOrBlank()) return
        val rows = JSONArray(body)
        for (i in 0 until rows.length()) {
            val remote = rows.getJSONObject(i)
            val remoteSyncId = remote.stringOrNull("id") ?: continue
            if (dao.getBySyncId(remoteSyncId) != null) continue // write-once: never merged, only inserted
            val createdAt = SupabaseRestClient.parseTimestamp(remote.opt("created_at"))
                ?: SupabaseRestClient.parseTimestamp(remote.opt("updated_at")) ?: continue
            dao.insert(
                Sale(
                    saleNumber = remote.stringOrNull("sale_number") ?: "",
                    customerId = localCustomerId(remote.stringOrNull("customer_id")),
                    customerName = remote.stringOrNull("customer_name") ?: "",
                    saleDate = SupabaseRestClient.parseTimestamp(remote.opt("sale_date")) ?: createdAt,
                    vatEnabled = remote.optBoolean("vat_enabled", true),
                    vatRate = remote.doubleOrNull("vat_rate") ?: 0.0,
                    discountType = parseDiscountType(remote.stringOrNull("discount_type")),
                    discountValue = remote.doubleOrNull("discount_value") ?: 0.0,
                    subtotal = remote.doubleOrNull("subtotal") ?: 0.0,
                    discountAmount = remote.doubleOrNull("discount_amount") ?: 0.0,
                    vatAmount = remote.doubleOrNull("vat_amount") ?: 0.0,
                    total = remote.doubleOrNull("total") ?: 0.0,
                    paymentMethod = runCatching { PaymentMethod.valueOf(remote.stringOrNull("payment_method") ?: "CASH") }
                        .getOrDefault(PaymentMethod.CASH),
                    notes = remote.stringOrNull("notes"),
                    createdAt = createdAt,
                    amountTendered = remote.doubleOrNull("amount_tendered"),
                    changeGiven = remote.doubleOrNull("change_given"),
                    syncId = remoteSyncId,
                    deletedAt = SupabaseRestClient.parseTimestamp(remote.opt("deleted_at"))
                )
            )
        }
    }

    // ---------------------------------------------------------------------
    // *_items — write-once, same rationale as sales (see class doc for the caveat around
    // items losing their syncId across a quote/invoice edit).
    // ---------------------------------------------------------------------

    private suspend fun syncQuoteItems(businessId: String, since: Long) {
        val dao = database.quoteItemDao()
        val quoteDao = database.quoteDao()

        dao.getAllForSync().filter { it.syncId == null }.forEach { item ->
            val parentSyncId = quoteDao.getById(item.quoteId)?.syncId ?: return@forEach
            val syncId = UUID.randomUUID().toString()
            dao.update(item.copy(syncId = syncId))
            val json = JSONObject().apply {
                put0("id", syncId)
                put0("business_id", businessId)
                put0("quote_id", parentSyncId)
                put0("description", item.description)
                put0("quantity", item.quantity)
                put0("unit_price", item.unitPrice)
                put0("line_total", item.lineTotal)
                put0("sort_order", item.sortOrder)
                put0("updated_at", SupabaseRestClient.isoString(System.currentTimeMillis()))
            }
            restClient.post("quote_items", json.toString(), mergeHeader)
        }

        val (code, body) = restClient.get("quote_items?business_id=eq.$businessId&updated_at=gt.${SupabaseRestClient.isoString(since)}")
        if (code !in 200..299 || body.isNullOrBlank()) return
        val rows = JSONArray(body)
        for (i in 0 until rows.length()) {
            val remote = rows.getJSONObject(i)
            val remoteSyncId = remote.stringOrNull("id") ?: continue
            if (dao.getBySyncId(remoteSyncId) != null) continue
            val parentId = quoteDao.getBySyncId(remote.stringOrNull("quote_id") ?: continue)?.id ?: continue
            dao.insert(
                QuoteItem(
                    quoteId = parentId,
                    description = remote.stringOrNull("description") ?: "",
                    quantity = remote.doubleOrNull("quantity") ?: 0.0,
                    unitPrice = remote.doubleOrNull("unit_price") ?: 0.0,
                    lineTotal = remote.doubleOrNull("line_total") ?: 0.0,
                    sortOrder = remote.optInt("sort_order", 0),
                    syncId = remoteSyncId,
                    deletedAt = SupabaseRestClient.parseTimestamp(remote.opt("deleted_at"))
                )
            )
        }
    }

    private suspend fun syncInvoiceItems(businessId: String, since: Long) {
        val dao = database.invoiceItemDao()
        val invoiceDao = database.invoiceDao()

        dao.getAllForSync().filter { it.syncId == null }.forEach { item ->
            val parentSyncId = invoiceDao.getById(item.invoiceId)?.syncId ?: return@forEach
            val syncId = UUID.randomUUID().toString()
            dao.update(item.copy(syncId = syncId))
            val json = JSONObject().apply {
                put0("id", syncId)
                put0("business_id", businessId)
                put0("invoice_id", parentSyncId)
                put0("description", item.description)
                put0("quantity", item.quantity)
                put0("unit_price", item.unitPrice)
                put0("line_total", item.lineTotal)
                put0("sort_order", item.sortOrder)
                put0("updated_at", SupabaseRestClient.isoString(System.currentTimeMillis()))
            }
            restClient.post("invoice_items", json.toString(), mergeHeader)
        }

        val (code, body) = restClient.get("invoice_items?business_id=eq.$businessId&updated_at=gt.${SupabaseRestClient.isoString(since)}")
        if (code !in 200..299 || body.isNullOrBlank()) return
        val rows = JSONArray(body)
        for (i in 0 until rows.length()) {
            val remote = rows.getJSONObject(i)
            val remoteSyncId = remote.stringOrNull("id") ?: continue
            if (dao.getBySyncId(remoteSyncId) != null) continue
            val parentId = invoiceDao.getBySyncId(remote.stringOrNull("invoice_id") ?: continue)?.id ?: continue
            dao.insert(
                InvoiceItem(
                    invoiceId = parentId,
                    description = remote.stringOrNull("description") ?: "",
                    quantity = remote.doubleOrNull("quantity") ?: 0.0,
                    unitPrice = remote.doubleOrNull("unit_price") ?: 0.0,
                    lineTotal = remote.doubleOrNull("line_total") ?: 0.0,
                    sortOrder = remote.optInt("sort_order", 0),
                    syncId = remoteSyncId,
                    deletedAt = SupabaseRestClient.parseTimestamp(remote.opt("deleted_at"))
                )
            )
        }
    }

    private suspend fun syncSaleItems(businessId: String, since: Long) {
        val dao = database.saleItemDao()
        val saleDao = database.saleDao()

        dao.getAllForSync().filter { it.syncId == null }.forEach { item ->
            val parentSyncId = saleDao.getById(item.saleId)?.syncId ?: return@forEach
            val syncId = UUID.randomUUID().toString()
            dao.update(item.copy(syncId = syncId))
            val json = JSONObject().apply {
                put0("id", syncId)
                put0("business_id", businessId)
                put0("sale_id", parentSyncId)
                put0("product_id", productSyncId(item.productId))
                put0("description", item.description)
                put0("quantity", item.quantity)
                put0("unit_price", item.unitPrice)
                put0("line_total", item.lineTotal)
                put0("sort_order", item.sortOrder)
                put0("updated_at", SupabaseRestClient.isoString(System.currentTimeMillis()))
            }
            restClient.post("sale_items", json.toString(), mergeHeader)
        }

        val (code, body) = restClient.get("sale_items?business_id=eq.$businessId&updated_at=gt.${SupabaseRestClient.isoString(since)}")
        if (code !in 200..299 || body.isNullOrBlank()) return
        val rows = JSONArray(body)
        for (i in 0 until rows.length()) {
            val remote = rows.getJSONObject(i)
            val remoteSyncId = remote.stringOrNull("id") ?: continue
            if (dao.getBySyncId(remoteSyncId) != null) continue
            val parentId = saleDao.getBySyncId(remote.stringOrNull("sale_id") ?: continue)?.id ?: continue
            dao.insert(
                SaleItem(
                    saleId = parentId,
                    productId = localProductId(remote.stringOrNull("product_id")),
                    description = remote.stringOrNull("description") ?: "",
                    quantity = remote.doubleOrNull("quantity") ?: 0.0,
                    unitPrice = remote.doubleOrNull("unit_price") ?: 0.0,
                    lineTotal = remote.doubleOrNull("line_total") ?: 0.0,
                    sortOrder = remote.optInt("sort_order", 0),
                    syncId = remoteSyncId,
                    deletedAt = SupabaseRestClient.parseTimestamp(remote.opt("deleted_at"))
                )
            )
        }
    }

    // ---------------------------------------------------------------------
    // FK resolution helpers — local Long id <-> remote syncId uuid.
    // ---------------------------------------------------------------------

    private suspend fun customerSyncId(customerId: Long?): String? =
        customerId?.let { database.customerDao().getById(it)?.syncId }

    private suspend fun quoteSyncId(quoteId: Long?): String? =
        quoteId?.let { database.quoteDao().getById(it)?.syncId }

    private suspend fun productSyncId(productId: Long?): String? =
        productId?.let { database.productDao().getById(it)?.syncId }

    private suspend fun localCustomerId(remoteSyncId: String?): Long? =
        remoteSyncId?.let { database.customerDao().getBySyncId(it)?.id }

    private suspend fun localQuoteId(remoteSyncId: String?): Long? =
        remoteSyncId?.let { database.quoteDao().getBySyncId(it)?.id }

    private suspend fun localProductId(remoteSyncId: String?): Long? =
        remoteSyncId?.let { database.productDao().getBySyncId(it)?.id }

    companion object {
        private val mergeHeader = mapOf("Prefer" to "resolution=merge-duplicates")

        private fun parseDiscountType(raw: String?): DiscountType =
            runCatching { DiscountType.valueOf(raw ?: "PERCENT") }.getOrDefault(DiscountType.PERCENT)
    }
}

private fun JSONObject.stringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

private fun JSONObject.doubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) getDouble(key) else null

/** [JSONObject.put] with a null value silently *removes* the key on Android instead of
 * writing a JSON `null` — which would make it impossible to push a field being cleared back
 * to empty (e.g. a customer's email) via a `merge-duplicates` upsert, since PostgREST leaves
 * an omitted column untouched rather than nulling it out. This always writes an explicit
 * `null` for a null value instead, and behaves exactly like [JSONObject.put] otherwise. */
private fun JSONObject.put0(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)
