package com.sheshabiz.quickquote.domain

import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteItem
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val businessProfile: BusinessProfile?,
    val customers: List<Customer>,
    val quotes: List<Quote>,
    val itemsByQuoteIndex: List<List<QuoteItem>>
)

/** Exports/imports the app's full local data set as a single human-readable JSON file. */
object BackupManager {
    private const val SCHEMA_VERSION = 1

    fun export(
        profile: BusinessProfile?,
        customers: List<Customer>,
        quotesWithItems: List<Pair<Quote, List<QuoteItem>>>
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
                put("logoUri", it.logoUri)
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
                put("items", JSONArray().apply {
                    items.forEach { item ->
                        put(JSONObject().apply {
                            put("description", item.description)
                            put("quantity", item.quantity)
                            put("unitPrice", item.unitPrice)
                            put("lineTotal", item.lineTotal)
                            put("sortOrder", item.sortOrder)
                        })
                    }
                })
            }
            quotesArray.put(quoteJson)
        }
        root.put("quotes", quotesArray)

        return root.toString(2)
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
                logoUri = it.optStringOrNull("logoUri")
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
            val itemsJson = q.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<QuoteItem>()
            for (j in 0 until itemsJson.length()) {
                val it = itemsJson.getJSONObject(j)
                items.add(
                    QuoteItem(
                        quoteId = 0,
                        description = it.optString("description"),
                        quantity = it.optDouble("quantity", 0.0),
                        unitPrice = it.optDouble("unitPrice", 0.0),
                        lineTotal = it.optDouble("lineTotal", 0.0),
                        sortOrder = it.optInt("sortOrder", j)
                    )
                )
            }
            itemsByQuoteIndex.add(items)
        }

        return BackupData(profile, localIdToCustomer.values.toList(), quotes, itemsByQuoteIndex)
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
