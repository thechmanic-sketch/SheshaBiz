package com.sheshabiz.quickquote.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.SaleRepository
import com.sheshabiz.quickquote.domain.PdfGenerator
import com.sheshabiz.quickquote.domain.model.SaleWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File

data class ReceiptPreviewUiState(
    val data: SaleWithItems? = null,
    val businessProfile: BusinessProfile? = null,
    val isLoading: Boolean = true
)

class ReceiptPreviewViewModel(
    saleId: Long,
    private val saleRepository: SaleRepository,
    private val businessRepository: BusinessRepository,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    val uiState: StateFlow<ReceiptPreviewUiState> = combine(
        saleRepository.observeSaleWithItems(saleId),
        businessRepository.observeProfile()
    ) { data, profile ->
        ReceiptPreviewUiState(data = data, businessProfile = profile, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReceiptPreviewUiState())

    suspend fun generatePdfFile(): File? {
        val state = uiState.value
        val data = state.data ?: return null
        val profile = state.businessProfile ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { pdfGenerator.generateForSale(profile, data) }.getOrNull()
        }
    }
}
