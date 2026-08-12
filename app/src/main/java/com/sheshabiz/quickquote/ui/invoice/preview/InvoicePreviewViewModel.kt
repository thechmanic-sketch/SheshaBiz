package com.sheshabiz.quickquote.ui.invoice.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.InvoiceRepository
import com.sheshabiz.quickquote.domain.PdfGenerator
import com.sheshabiz.quickquote.domain.model.InvoiceWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class InvoicePreviewUiState(
    val data: InvoiceWithItems? = null,
    val businessProfile: BusinessProfile? = null,
    val isLoading: Boolean = true
)

class InvoicePreviewViewModel(
    private val invoiceId: Long,
    private val invoiceRepository: InvoiceRepository,
    private val businessRepository: BusinessRepository,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    val uiState: StateFlow<InvoicePreviewUiState> = combine(
        invoiceRepository.observeInvoiceWithItems(invoiceId),
        businessRepository.observeProfile()
    ) { data, profile ->
        InvoicePreviewUiState(data = data, businessProfile = profile, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InvoicePreviewUiState())

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    suspend fun generatePdfFile(): File? {
        val state = uiState.value
        val data = state.data ?: return null
        val profile = state.businessProfile ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { pdfGenerator.generateForInvoice(profile, data) }.getOrNull()
        }
    }

    fun markAsPaid() {
        viewModelScope.launch {
            invoiceRepository.updateStatus(invoiceId, InvoiceStatus.PAID, System.currentTimeMillis(), System.currentTimeMillis())
        }
    }

    fun markAsUnpaid() {
        viewModelScope.launch {
            invoiceRepository.updateStatus(invoiceId, InvoiceStatus.UNPAID, null, System.currentTimeMillis())
        }
    }

    fun deleteInvoice() {
        viewModelScope.launch {
            uiState.value.data?.invoice?.let { invoice ->
                invoiceRepository.deleteInvoice(invoice)
                _deleted.value = true
            }
        }
    }
}
