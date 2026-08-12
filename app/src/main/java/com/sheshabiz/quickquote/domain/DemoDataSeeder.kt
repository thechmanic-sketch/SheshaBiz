package com.sheshabiz.quickquote.domain

import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteItem
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository

/** Populates realistic sample data so the app can be demonstrated with zero manual entry. */
class DemoDataSeeder(
    private val businessRepository: BusinessRepository,
    private val customerRepository: CustomerRepository,
    private val quoteRepository: QuoteRepository,
    private val prefs: AppPreferences
) {
    suspend fun seed() {
        businessRepository.saveProfile(
            BusinessProfile(
                businessName = "Durban Pro Services",
                ownerName = "Sipho Ndlovu",
                phone = "031 555 0142",
                whatsappNumber = "0825550142",
                email = "info@durbanproservices.co.za",
                address = "12 Marine Parade, Durban, 4001",
                vatNumber = "4123456789",
                logoUri = null
            )
        )

        val now = System.currentTimeMillis()
        val customerId = customerRepository.upsert(
            Customer(
                name = "John Mthembu",
                phone = "0731234567",
                email = "john.mthembu@example.co.za",
                address = "8 Florida Road, Durban, 4001",
                createdAt = now
            )
        )

        val vatRate = AppPreferences.DEFAULT_VAT_RATE
        val items = listOf(
            LineItemInput(quantity = 1.0, unitPrice = 850.0),
            LineItemInput(quantity = 1.0, unitPrice = 250.0)
        )
        val totals = QuoteCalculator.calculate(
            items = items,
            vatEnabled = true,
            vatRatePercent = vatRate,
            discountType = DiscountType.PERCENT,
            discountValue = 0.0
        )

        val dayMillis = 24L * 60 * 60 * 1000
        val quote = Quote(
            quoteNumber = prefs.reserveNextQuoteNumber(),
            customerId = customerId,
            customerName = "John Mthembu",
            customerPhone = "0731234567",
            customerEmail = "john.mthembu@example.co.za",
            customerAddress = "8 Florida Road, Durban, 4001",
            quoteDate = now,
            validUntil = now + 7 * dayMillis,
            vatEnabled = true,
            vatRate = vatRate,
            discountType = DiscountType.PERCENT,
            discountValue = 0.0,
            subtotal = totals.subtotal,
            discountAmount = totals.discountAmount,
            vatAmount = totals.vatAmount,
            total = totals.total,
            notes = "Thank you for choosing Durban Pro Services.",
            paymentTerms = AppPreferences.DEFAULT_PAYMENT_TERMS,
            status = QuoteStatus.SENT,
            createdAt = now,
            updatedAt = now
        )

        val quoteItems = listOf(
            QuoteItem(
                quoteId = 0,
                description = "Plumbing repair",
                quantity = 1.0,
                unitPrice = 850.0,
                lineTotal = QuoteCalculator.lineTotal(1.0, 850.0),
                sortOrder = 0
            ),
            QuoteItem(
                quoteId = 0,
                description = "Call-out fee",
                quantity = 1.0,
                unitPrice = 250.0,
                lineTotal = QuoteCalculator.lineTotal(1.0, 250.0),
                sortOrder = 1
            )
        )

        quoteRepository.createQuote(quote, quoteItems)

        prefs.setBusinessSetupComplete(true)
        prefs.setOnboardingComplete(true)
    }
}
