package com.sheshabiz.quickquote.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Product
import com.sheshabiz.quickquote.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductsUiState(
    val allProducts: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class ProductsViewModel(private val repository: ProductRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ProductsUiState> = combine(
        repository.observeAll(),
        searchQuery
    ) { products, query ->
        val filtered = if (query.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(query, ignoreCase = true) || it.sku?.contains(query, ignoreCase = true) == true
            }
        }
        ProductsUiState(
            allProducts = products,
            filteredProducts = filtered,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductsUiState())

    fun onSearchChange(query: String) {
        searchQuery.value = query
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { repository.delete(product) }
    }
}
