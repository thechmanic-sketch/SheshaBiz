package com.sheshabiz.quickquote.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomersUiState(
    val allCustomers: List<Customer> = emptyList(),
    val filteredCustomers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class CustomersViewModel(private val repository: CustomerRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<CustomersUiState> = combine(
        repository.observeAll(),
        searchQuery
    ) { customers, query ->
        val filtered = if (query.isBlank()) {
            customers
        } else {
            customers.filter {
                it.name.contains(query, ignoreCase = true) || it.phone.contains(query, ignoreCase = true)
            }
        }
        CustomersUiState(
            allCustomers = customers,
            filteredCustomers = filtered,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomersUiState())

    fun onSearchChange(query: String) {
        searchQuery.value = query
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch { repository.delete(customer) }
    }
}
