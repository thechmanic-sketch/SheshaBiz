package com.sheshabiz.quickquote.domain

import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.data.repository.InvoiceRepository
import com.sheshabiz.quickquote.data.repository.ProductRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository
import com.sheshabiz.quickquote.data.repository.SaleRepository

/** Orchestrates a full local data export/import across all repositories. */
class BackupService(
    private val businessRepository: BusinessRepository,
    private val customerRepository: CustomerRepository,
    private val quoteRepository: QuoteRepository,
    private val invoiceRepository: InvoiceRepository,
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository
) {
    suspend fun exportJson(): String {
        val profile = businessRepository.getProfile()
        val customers = customerRepository.getAllOnce()
        val quotes = quoteRepository.getAllOnce()
        val quotePairs = quotes.map { it to quoteRepository.getItemsForQuote(it.id) }
        val invoices = invoiceRepository.getAllOnce()
        val invoicePairs = invoices.map { it to invoiceRepository.getItemsForInvoice(it.id) }
        val products = productRepository.getAllOnce()
        val sales = saleRepository.getAllOnce()
        val salePairs = sales.map { it to saleRepository.getItemsForSale(it.id) }
        return BackupManager.export(profile, customers, quotePairs, invoicePairs, products, salePairs)
    }

    suspend fun importJson(json: String) {
        val data = BackupManager.import(json)

        saleRepository.deleteAll()
        invoiceRepository.deleteAll()
        quoteRepository.deleteAll()
        customerRepository.deleteAll()
        productRepository.deleteAll()

        data.businessProfile?.let { businessRepository.saveProfile(it) }
        data.customers.forEach { customerRepository.upsert(it) }
        data.quotes.forEachIndexed { index, quote ->
            quoteRepository.createQuote(quote, data.itemsByQuoteIndex.getOrElse(index) { emptyList() })
        }
        data.invoices.forEachIndexed { index, invoice ->
            invoiceRepository.createInvoice(invoice, data.itemsByInvoiceIndex.getOrElse(index) { emptyList() })
        }
        data.products.forEach { productRepository.upsert(it) }
        data.sales.forEachIndexed { index, sale ->
            saleRepository.completeSale(sale, data.itemsBySaleIndex.getOrElse(index) { emptyList() })
        }
    }
}
