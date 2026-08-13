package com.sheshabiz.quickquote.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Sale
import com.sheshabiz.quickquote.data.repository.SaleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SalesHistoryUiState(
    val allSales: List<Sale> = emptyList(),
    val filteredSales: List<Sale> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class SalesHistoryViewModel(repository: SaleRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<SalesHistoryUiState> = combine(
        repository.observeAll(),
        searchQuery
    ) { sales, query ->
        val filtered = if (query.isBlank()) {
            sales
        } else {
            sales.filter {
                it.customerName.contains(query, ignoreCase = true) || it.saleNumber.contains(query, ignoreCase = true)
            }
        }
        SalesHistoryUiState(
            allSales = sales,
            filteredSales = filtered,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalesHistoryUiState())

    fun onSearchChange(query: String) {
        searchQuery.value = query
    }
}
