package com.sheshabiz.quickquote.ui.quote.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.Invoice
import com.sheshabiz.quickquote.data.db.entity.InvoiceItem
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.InvoiceRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository
import com.sheshabiz.quickquote.domain.PdfGenerator
import com.sheshabiz.quickquote.domain.TrialGate
import com.sheshabiz.quickquote.domain.model.QuoteWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class QuotePreviewUiState(
    val data: QuoteWithItems? = null,
    val businessProfile: BusinessProfile? = null,
    val isLoading: Boolean = true
)

class QuotePreviewViewModel(
    private val quoteId: Long,
    private val quoteRepository: QuoteRepository,
    private val businessRepository: BusinessRepository,
    private val invoiceRepository: InvoiceRepository,
    private val preferences: AppPreferences,
    private val authPreferences: AuthPreferences,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    val uiState: StateFlow<QuotePreviewUiState> = combine(
        quoteRepository.observeQuoteWithItems(quoteId),
        businessRepository.observeProfile()
    ) { data, profile ->
        QuotePreviewUiState(data = data, businessProfile = profile, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuotePreviewUiState())

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    private val _duplicatedId = MutableStateFlow<Long?>(null)
    val duplicatedId: StateFlow<Long?> = _duplicatedId

    private val _convertedInvoiceId = MutableStateFlow<Long?>(null)
    val convertedInvoiceId: StateFlow<Long?> = _convertedInvoiceId

    private val _lockedReason = MutableStateFlow<TrialGate.LockReason?>(null)
    val lockedReason: StateFlow<TrialGate.LockReason?> = _lockedReason
    fun clearLockedReason() { _lockedReason.value = null }

    suspend fun generatePdfFile(): File? {
        val state = uiState.value
        val data = state.data ?: return null
        val profile = state.businessProfile ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { pdfGenerator.generateForQuote(profile, data) }.getOrNull()
        }
    }

    fun updateStatus(status: QuoteStatus) {
        viewModelScope.launch {
            quoteRepository.updateStatus(quoteId, status, System.currentTimeMillis())
        }
    }

    fun deleteQuote() {
        viewModelScope.launch {
            uiState.value.data?.quote?.let { quote ->
                quoteRepository.deleteQuote(quote)
                _deleted.value = true
            }
        }
    }

    fun duplicateQuote() {
        val lockReason = TrialGate.lockReason(authPreferences)
        if (lockReason != null) {
            _lockedReason.value = lockReason
            return
        }
        viewModelScope.launch {
            val current = uiState.value.data ?: return@launch
            val now = System.currentTimeMillis()
            val newQuote = current.quote.copy(
                id = 0,
                quoteNumber = preferences.reserveNextQuoteNumber(),
                quoteDate = now,
                validUntil = now + 7L * 24 * 60 * 60 * 1000,
                status = QuoteStatus.DRAFT,
                createdAt = now,
                updatedAt = now
            )
            val newItems = current.items.map { it.copy(id = 0, quoteId = 0) }
            val newId = quoteRepository.createQuote(newQuote, newItems)
            _duplicatedId.value = newId
        }
    }

    fun convertToInvoice() {
        val lockReason = TrialGate.lockReason(authPreferences)
        if (lockReason != null) {
            _lockedReason.value = lockReason
            return
        }
        viewModelScope.launch {
            val current = uiState.value.data ?: return@launch
            val quote = current.quote
            val now = System.currentTimeMillis()
            val invoice = Invoice(
                invoiceNumber = preferences.reserveNextInvoiceNumber(),
                sourceQuoteId = quote.id,
                customerId = quote.customerId,
                customerName = quote.customerName,
                customerPhone = quote.customerPhone,
                customerEmail = quote.customerEmail,
                customerAddress = quote.customerAddress,
                invoiceDate = now,
                dueDate = now + 7L * 24 * 60 * 60 * 1000,
                vatEnabled = quote.vatEnabled,
                vatRate = quote.vatRate,
                discountType = quote.discountType,
                discountValue = quote.discountValue,
                subtotal = quote.subtotal,
                discountAmount = quote.discountAmount,
                vatAmount = quote.vatAmount,
                total = quote.total,
                notes = quote.notes,
                paymentTerms = quote.paymentTerms,
                status = InvoiceStatus.UNPAID,
                paidAt = null,
                createdAt = now,
                updatedAt = now
            )
            val items = current.items.map {
                InvoiceItem(
                    invoiceId = 0,
                    description = it.description,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    lineTotal = it.lineTotal,
                    sortOrder = it.sortOrder
                )
            }
            val newId = invoiceRepository.createInvoice(invoice, items)
            _convertedInvoiceId.value = newId
        }
    }
}
