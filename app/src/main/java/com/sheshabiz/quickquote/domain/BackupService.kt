package com.sheshabiz.quickquote.domain

import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.data.repository.InvoiceRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository

/** Orchestrates a full local data export/import across all repositories. */
class BackupService(
    private val businessRepository: BusinessRepository,
    private val customerRepository: CustomerRepository,
    private val quoteRepository: QuoteRepository,
    private val invoiceRepository: InvoiceRepository
) {
    suspend fun exportJson(): String {
        val profile = businessRepository.getProfile()
        val customers = customerRepository.getAllOnce()
        val quotes = quoteRepository.getAllOnce()
        val quotePairs = quotes.map { it to quoteRepository.getItemsForQuote(it.id) }
        val invoices = invoiceRepository.getAllOnce()
        val invoicePairs = invoices.map { it to invoiceRepository.getItemsForInvoice(it.id) }
        return BackupManager.export(profile, customers, quotePairs, invoicePairs)
    }

    suspend fun importJson(json: String) {
        val data = BackupManager.import(json)

        invoiceRepository.deleteAll()
        quoteRepository.deleteAll()
        customerRepository.deleteAll()

        data.businessProfile?.let { businessRepository.saveProfile(it) }
        data.customers.forEach { customerRepository.upsert(it) }
        data.quotes.forEachIndexed { index, quote ->
            quoteRepository.createQuote(quote, data.itemsByQuoteIndex.getOrElse(index) { emptyList() })
        }
        data.invoices.forEachIndexed { index, invoice ->
            invoiceRepository.createInvoice(invoice, data.itemsByInvoiceIndex.getOrElse(index) { emptyList() })
        }
    }
}
