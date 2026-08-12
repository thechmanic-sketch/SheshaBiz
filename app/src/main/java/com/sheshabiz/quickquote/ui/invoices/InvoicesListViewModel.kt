package com.sheshabiz.quickquote.ui.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Invoice
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus
import com.sheshabiz.quickquote.data.repository.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class InvoiceSortOption { NEWEST, OLDEST, HIGHEST, LOWEST }

enum class InvoiceFilter { ALL, UNPAID, OVERDUE, PAID, CANCELLED }

data class InvoicesListUiState(
    val invoices: List<Invoice> = emptyList(),
    val searchQuery: String = "",
    val filter: InvoiceFilter = InvoiceFilter.ALL,
    val sortOption: InvoiceSortOption = InvoiceSortOption.NEWEST,
    val isLoading: Boolean = true
)

class InvoicesListViewModel(repository: InvoiceRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val filter = MutableStateFlow(InvoiceFilter.ALL)
    private val sortOption = MutableStateFlow(InvoiceSortOption.NEWEST)

    val uiState: StateFlow<InvoicesListUiState> = combine(
        repository.observeAll(),
        searchQuery,
        filter,
        sortOption
    ) { invoices, query, currentFilter, sort ->
        val now = System.currentTimeMillis()
        var result = when (currentFilter) {
            InvoiceFilter.ALL -> invoices
            InvoiceFilter.UNPAID -> invoices.filter { it.status == InvoiceStatus.UNPAID }
            InvoiceFilter.OVERDUE -> invoices.filter { it.status == InvoiceStatus.UNPAID && it.dueDate < now }
            InvoiceFilter.PAID -> invoices.filter { it.status == InvoiceStatus.PAID }
            InvoiceFilter.CANCELLED -> invoices.filter { it.status == InvoiceStatus.CANCELLED }
        }
        if (query.isNotBlank()) {
            result = result.filter {
                it.customerName.contains(query, ignoreCase = true) || it.invoiceNumber.contains(query, ignoreCase = true)
            }
        }
        result = when (sort) {
            InvoiceSortOption.NEWEST -> result.sortedByDescending { it.createdAt }
            InvoiceSortOption.OLDEST -> result.sortedBy { it.createdAt }
            InvoiceSortOption.HIGHEST -> result.sortedByDescending { it.total }
            InvoiceSortOption.LOWEST -> result.sortedBy { it.total }
        }
        InvoicesListUiState(
            invoices = result,
            searchQuery = query,
            filter = currentFilter,
            sortOption = sort,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InvoicesListUiState())

    fun onSearchChange(query: String) { searchQuery.value = query }
    fun onFilterChange(newFilter: InvoiceFilter) { filter.value = newFilter }
    fun onSortChange(sort: InvoiceSortOption) { sortOption.value = sort }
}
