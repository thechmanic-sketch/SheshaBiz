package com.sheshabiz.quickquote.domain

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
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val businessProfile: BusinessProfile?,
    val customers: List<Customer>,
    val quotes: List<Quote>,
    val itemsByQuoteIndex: List<List<QuoteItem>>,
    val invoices: List<Invoice>,
    val itemsByInvoiceIndex: List<List<InvoiceItem>>,
    val products: List<Product>,
    val sales: List<Sale>,
    val itemsBySaleIndex: List<List<SaleItem>>
)

/** Exports/imports the app's full local data set as a single human-readable JSON file. */
object BackupManager {
    private const val SCHEMA_VERSION = 3

    fun export(
        profile: BusinessProfile?,
        customers: List<Customer>,
        quotesWithItems: List<Pair<Quote, List<QuoteItem>>>,
        invoicesWithItems: List<Pair<Invoice, List<InvoiceItem>>>,
        products: List<Product>,
        salesWithItems: List<Pair<Sale, List<SaleItem>>>
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        profile?.let {
            root.put("businessProfile", JSONObject().apply {
                put("businessName", it.businessName)
                put("ownerName", it.ownerName)
                put("phone", it.phone)
                put("whatsappNumber", it.whatsappNumber)
                put("email", it.email)
                put("address", it.address)
                put("vatNumber", it.vatNumber)
                put("registrationNumber", it.registrationNumber)
                put("logoUri", it.logoUri)
                put("bankName", it.bankName)
                put("accountHolder", it.accountHolder)
                put("accountNumber", it.accountNumber)
                put("branchCode", it.branchCode)
                put("accountType", it.accountType)
            })
        }

        val customersArray = JSONArray()
        customers.forEach { c ->
            customersArray.put(JSONObject().apply {
                put("localId", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("email", c.email)
                put("address", c.address)
                put("createdAt", c.createdAt)
            })
        }
        root.put("customers", customersArray)

        val quotesArray = JSONArray()
        quotesWithItems.forEach { (quote, items) ->
            val quoteJson = JSONObject().apply {
                put("quoteNumber", quote.quoteNumber)
                put("customerLocalId", quote.customerId ?: JSONObject.NULL)
                put("customerName", quote.customerName)
                put("customerPhone", quote.customerPhone)
                put("customerEmail", quote.customerEmail)
                put("customerAddress", quote.customerAddress)
                put("quoteDate", quote.quoteDate)
                put("validUntil", quote.validUntil)
                put("vatEnabled", quote.vatEnabled)
                put("vatRate", quote.vatRate)
                put("discountType", quote.discountType.name)
                put("discountValue", quote.discountValue)
                put("subtotal", quote.subtotal)
                put("discountAmount", quote.discountAmount)
                put("vatAmount", quote.vatAmount)
                put("total", quote.total)
                put("notes", quote.notes)
                put("paymentTerms", quote.paymentTerms)
                put("status", quote.status.name)
                put("createdAt", quote.createdAt)
                put("updatedAt", quote.updatedAt)
                put("items", itemsToJson(items) { item ->
                    JSONObject().apply {
                        put("description", item.description)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("lineTotal", item.lineTotal)
                        put("sortOrder", item.sortOrder)
                    }
                })
            }
            quotesArray.put(quoteJson)
        }
        root.put("quotes", quotesArray)

        val invoicesArray = JSONArray()
        invoicesWithItems.forEach { (invoice, items) ->
            val invoiceJson = JSONObject().apply {
                put("invoiceNumber", invoice.invoiceNumber)
                put("customerLocalId", invoice.customerId ?: JSONObject.NULL)
                put("customerName", invoice.customerName)
                put("customerPhone", invoice.customerPhone)
                put("customerEmail", invoice.customerEmail)
                put("customerAddress", invoice.customerAddress)
                put("invoiceDate", invoice.invoiceDate)
                put("dueDate", invoice.dueDate)
                put("vatEnabled", invoice.vatEnabled)
                put("vatRate", invoice.vatRate)
                put("discountType", invoice.discountType.name)
                put("discountValue", invoice.discountValue)
                put("subtotal", invoice.subtotal)
                put("discountAmount", invoice.discountAmount)
                put("vatAmount", invoice.vatAmount)
                put("total", invoice.total)
                put("notes", invoice.notes)
                put("paymentTerms", invoice.paymentTerms)
                put("status", invoice.status.name)
                put("paidAt", invoice.paidAt ?: JSONObject.NULL)
                put("createdAt", invoice.createdAt)
                put("updatedAt", invoice.updatedAt)
                put("items", itemsToJson(items) { item ->
                    JSONObject().apply {
                        put("description", item.description)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("lineTotal", item.lineTotal)
                        put("sortOrder", item.sortOrder)
                    }
                })
            }
            invoicesArray.put(invoiceJson)
        }
        root.put("invoices", invoicesArray)

        val productsArray = JSONArray()
        products.forEach { p ->
            productsArray.put(JSONObject().apply {
                put("name", p.name)
                put("unitPrice", p.unitPrice)
                put("sku", p.sku)
                put("trackStock", p.trackStock)
                put("stockQuantity", p.stockQuantity)
                put("lowStockThreshold", p.lowStockThreshold ?: JSONObject.NULL)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            })
        }
        root.put("products", productsArray)

        val salesArray = JSONArray()
        salesWithItems.forEach { (sale, items) ->
            val saleJson = JSONObject().apply {
                put("saleNumber", sale.saleNumber)
                put("customerName", sale.customerName)
                put("saleDate", sale.saleDate)
                put("vatEnabled", sale.vatEnabled)
                put("vatRate", sale.vatRate)
                put("discountType", sale.discountType.name)
                put("discountValue", sale.discountValue)
                put("subtotal", sale.subtotal)
                put("discountAmount", sale.discountAmount)
                put("vatAmount", sale.vatAmount)
                put("total", sale.total)
                put("paymentMethod", sale.paymentMethod.name)
                put("notes", sale.notes)
                put("createdAt", sale.createdAt)
                put("amountTendered", sale.amountTendered)
                put("changeGiven", sale.changeGiven)
                put("items", itemsToJson(items) { item ->
                    JSONObject().apply {
                        put("description", item.description)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("lineTotal", item.lineTotal)
                        put("sortOrder", item.sortOrder)
                    }
                })
            }
            salesArray.put(saleJson)
        }
        root.put("sales", salesArray)

        return root.toString(2)
    }

    private fun <T> itemsToJson(items: List<T>, toJson: (T) -> JSONObject): JSONArray {
        val array = JSONArray()
        items.forEach { array.put(toJson(it)) }
        return array
    }

    fun import(json: String): BackupData {
        val root = JSONObject(json)

        val profile = root.optJSONObject("businessProfile")?.let {
            BusinessProfile(
                businessName = it.optString("businessName"),
                ownerName = it.optString("ownerName"),
                phone = it.optString("phone"),
                whatsappNumber = it.optString("whatsappNumber"),
                email = it.optString("email"),
                address = it.optString("address"),
                vatNumber = it.optStringOrNull("vatNumber"),
                registrationNumber = it.optStringOrNull("registrationNumber"),
                logoUri = it.optStringOrNull("logoUri"),
                bankName = it.optStringOrNull("bankName"),
                accountHolder = it.optStringOrNull("accountHolder"),
                accountNumber = it.optStringOrNull("accountNumber"),
                branchCode = it.optStringOrNull("branchCode"),
                accountType = it.optStringOrNull("accountType")
            )
        }

        val customersJson = root.optJSONArray("customers") ?: JSONArray()
        val localIdToCustomer = LinkedHashMap<Long, Customer>()
        for (i in 0 until customersJson.length()) {
            val c = customersJson.getJSONObject(i)
            val localId = c.optLong("localId", -1)
            localIdToCustomer[localId] = Customer(
                name = c.optString("name"),
                phone = c.optString("phone"),
                email = c.optStringOrNull("email"),
                address = c.optStringOrNull("address"),
                createdAt = c.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val quotesJson = root.optJSONArray("quotes") ?: JSONArray()
        val quotes = mutableListOf<Quote>()
        val itemsByQuoteIndex = mutableListOf<List<QuoteItem>>()
        for (i in 0 until quotesJson.length()) {
            val q = quotesJson.getJSONObject(i)
            quotes.add(
                Quote(
                    quoteNumber = q.optString("quoteNumber"),
                    customerId = null,
                    customerName = q.optString("customerName"),
                    customerPhone = q.optString("customerPhone"),
                    customerEmail = q.optStringOrNull("customerEmail"),
                    customerAddress = q.optStringOrNull("customerAddress"),
                    quoteDate = q.optLong("quoteDate"),
                    validUntil = q.optLong("validUntil"),
                    vatEnabled = q.optBoolean("vatEnabled", true),
                    vatRate = q.optDouble("vatRate", 15.0),
                    discountType = runCatching { DiscountType.valueOf(q.optString("discountType")) }.getOrDefault(DiscountType.PERCENT),
                    discountValue = q.optDouble("discountValue", 0.0),
                    subtotal = q.optDouble("subtotal", 0.0),
                    discountAmount = q.optDouble("discountAmount", 0.0),
                    vatAmount = q.optDouble("vatAmount", 0.0),
                    total = q.optDouble("total", 0.0),
                    notes = q.optStringOrNull("notes"),
                    paymentTerms = q.optStringOrNull("paymentTerms"),
                    status = runCatching { QuoteStatus.valueOf(q.optString("status")) }.getOrDefault(QuoteStatus.DRAFT),
                    createdAt = q.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = q.optLong("updatedAt", System.currentTimeMillis())
                )
            )
            itemsByQuoteIndex.add(parseItems(q.optJSONArray("items")) { desc, qty, price, total, order ->
                QuoteItem(quoteId = 0, description = desc, quantity = qty, unitPrice = price, lineTotal = total, sortOrder = order)
            })
        }

        val invoicesJson = root.optJSONArray("invoices") ?: JSONArray()
        val invoices = mutableListOf<Invoice>()
        val itemsByInvoiceIndex = mutableListOf<List<InvoiceItem>>()
        for (i in 0 until invoicesJson.length()) {
            val inv = invoicesJson.getJSONObject(i)
            invoices.add(
                Invoice(
                    invoiceNumber = inv.optString("invoiceNumber"),
                    sourceQuoteId = null,
                    customerId = null,
                    customerName = inv.optString("customerName"),
                    customerPhone = inv.optString("customerPhone"),
                    customerEmail = inv.optStringOrNull("customerEmail"),
                    customerAddress = inv.optStringOrNull("customerAddress"),
                    invoiceDate = inv.optLong("invoiceDate"),
                    dueDate = inv.optLong("dueDate"),
                    vatEnabled = inv.optBoolean("vatEnabled", true),
                    vatRate = inv.optDouble("vatRate", 15.0),
                    discountType = runCatching { DiscountType.valueOf(inv.optString("discountType")) }.getOrDefault(DiscountType.PERCENT),
                    discountValue = inv.optDouble("discountValue", 0.0),
                    subtotal = inv.optDouble("subtotal", 0.0),
                    discountAmount = inv.optDouble("discountAmount", 0.0),
                    vatAmount = inv.optDouble("vatAmount", 0.0),
                    total = inv.optDouble("total", 0.0),
                    notes = inv.optStringOrNull("notes"),
                    paymentTerms = inv.optStringOrNull("paymentTerms"),
                    status = runCatching { InvoiceStatus.valueOf(inv.optString("status")) }.getOrDefault(InvoiceStatus.UNPAID),
                    paidAt = if (inv.isNull("paidAt")) null else inv.optLong("paidAt"),
                    createdAt = inv.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = inv.optLong("updatedAt", System.currentTimeMillis())
                )
            )
            itemsByInvoiceIndex.add(parseItems(inv.optJSONArray("items")) { desc, qty, price, total, order ->
                InvoiceItem(invoiceId = 0, description = desc, quantity = qty, unitPrice = price, lineTotal = total, sortOrder = order)
            })
        }

        val productsJson = root.optJSONArray("products") ?: JSONArray()
        val products = mutableListOf<Product>()
        for (i in 0 until productsJson.length()) {
            val p = productsJson.getJSONObject(i)
            products.add(
                Product(
                    name = p.optString("name"),
                    unitPrice = p.optDouble("unitPrice", 0.0),
                    sku = p.optStringOrNull("sku"),
                    trackStock = p.optBoolean("trackStock", false),
                    stockQuantity = p.optDouble("stockQuantity", 0.0),
                    lowStockThreshold = if (p.isNull("lowStockThreshold")) null else p.optDouble("lowStockThreshold"),
                    createdAt = p.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = p.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val salesJson = root.optJSONArray("sales") ?: JSONArray()
        val sales = mutableListOf<Sale>()
        val itemsBySaleIndex = mutableListOf<List<SaleItem>>()
        for (i in 0 until salesJson.length()) {
            val s = salesJson.getJSONObject(i)
            sales.add(
                Sale(
                    saleNumber = s.optString("saleNumber"),
                    customerId = null,
                    customerName = s.optString("customerName"),
                    saleDate = s.optLong("saleDate"),
                    vatEnabled = s.optBoolean("vatEnabled", true),
                    vatRate = s.optDouble("vatRate", 15.0),
                    discountType = runCatching { DiscountType.valueOf(s.optString("discountType")) }.getOrDefault(DiscountType.PERCENT),
                    discountValue = s.optDouble("discountValue", 0.0),
                    subtotal = s.optDouble("subtotal", 0.0),
                    discountAmount = s.optDouble("discountAmount", 0.0),
                    vatAmount = s.optDouble("vatAmount", 0.0),
                    total = s.optDouble("total", 0.0),
                    paymentMethod = runCatching { PaymentMethod.valueOf(s.optString("paymentMethod")) }.getOrDefault(PaymentMethod.CASH),
                    notes = s.optStringOrNull("notes"),
                    createdAt = s.optLong("createdAt", System.currentTimeMillis()),
                    amountTendered = if (s.has("amountTendered") && !s.isNull("amountTendered")) s.optDouble("amountTendered") else null,
                    changeGiven = if (s.has("changeGiven") && !s.isNull("changeGiven")) s.optDouble("changeGiven") else null
                )
            )
            itemsBySaleIndex.add(parseItems(s.optJSONArray("items")) { desc, qty, price, total, order ->
                SaleItem(saleId = 0, productId = null, description = desc, quantity = qty, unitPrice = price, lineTotal = total, sortOrder = order)
            })
        }

        return BackupData(
            profile, localIdToCustomer.values.toList(),
            quotes, itemsByQuoteIndex,
            invoices, itemsByInvoiceIndex,
            products,
            sales, itemsBySaleIndex
        )
    }

    private fun <T> parseItems(
        itemsJson: JSONArray?,
        build: (description: String, quantity: Double, unitPrice: Double, lineTotal: Double, sortOrder: Int) -> T
    ): List<T> {
        val array = itemsJson ?: JSONArray()
        val result = mutableListOf<T>()
        for (j in 0 until array.length()) {
            val it = array.getJSONObject(j)
            result.add(
                build(
                    it.optString("description"),
                    it.optDouble("quantity", 0.0),
                    it.optDouble("unitPrice", 0.0),
                    it.optDouble("lineTotal", 0.0),
                    it.optInt("sortOrder", j)
                )
            )
        }
        return result
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
