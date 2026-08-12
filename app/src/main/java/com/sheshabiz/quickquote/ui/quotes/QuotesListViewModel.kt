package com.sheshabiz.quickquote.ui.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.data.repository.QuoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class SortOption { NEWEST, OLDEST, HIGHEST, LOWEST }

data class QuotesListUiState(
    val quotes: List<Quote> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: QuoteStatus? = null,
    val sortOption: SortOption = SortOption.NEWEST,
    val isLoading: Boolean = true
)

class QuotesListViewModel(repository: QuoteRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<QuoteStatus?>(null)
    private val sortOption = MutableStateFlow(SortOption.NEWEST)

    val uiState: StateFlow<QuotesListUiState> = combine(
        repository.observeAll(),
        searchQuery,
        statusFilter,
        sortOption
    ) { quotes, query, filter, sort ->
        var result = quotes
        if (filter != null) result = result.filter { it.status == filter }
        if (query.isNotBlank()) {
            result = result.filter {
                it.customerName.contains(query, ignoreCase = true) || it.quoteNumber.contains(query, ignoreCase = true)
            }
        }
        result = when (sort) {
            SortOption.NEWEST -> result.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> result.sortedBy { it.createdAt }
            SortOption.HIGHEST -> result.sortedByDescending { it.total }
            SortOption.LOWEST -> result.sortedBy { it.total }
        }
        QuotesListUiState(
            quotes = result,
            searchQuery = query,
            statusFilter = filter,
            sortOption = sort,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuotesListUiState())

    fun onSearchChange(query: String) { searchQuery.value = query }
    fun onStatusFilterChange(status: QuoteStatus?) { statusFilter.value = status }
    fun onSortChange(sort: SortOption) { sortOption.value = sort }
}
