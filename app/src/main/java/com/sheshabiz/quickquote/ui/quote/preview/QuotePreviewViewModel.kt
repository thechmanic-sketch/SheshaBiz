package com.sheshabiz.quickquote.ui.quote.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository
import com.sheshabiz.quickquote.domain.PdfGenerator
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
    private val preferences: AppPreferences,
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

    suspend fun generatePdfFile(): File? {
        val state = uiState.value
        val data = state.data ?: return null
        val profile = state.businessProfile ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { pdfGenerator.generate(profile, data) }.getOrNull()
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
}
