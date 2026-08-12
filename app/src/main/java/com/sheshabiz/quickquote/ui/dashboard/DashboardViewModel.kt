package com.sheshabiz.quickquote.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val businessName: String = "",
    val quotesCreated: Int = 0,
    val quotesSent: Int = 0,
    val quotesAccepted: Int = 0,
    val totalQuoted: Double = 0.0,
    val recentQuotes: List<Quote> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    businessRepository: BusinessRepository,
    quoteRepository: QuoteRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        businessRepository.observeProfile(),
        quoteRepository.observeAll()
    ) { profile, quotes ->
        DashboardUiState(
            businessName = profile?.businessName.orEmpty(),
            quotesCreated = quotes.size,
            quotesSent = quotes.count { it.status != QuoteStatus.DRAFT },
            quotesAccepted = quotes.count { it.status == QuoteStatus.ACCEPTED },
            totalQuoted = quotes.sumOf { it.total },
            recentQuotes = quotes.take(5),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
