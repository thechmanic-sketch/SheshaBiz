package com.sheshabiz.quickquote.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.data.db.entity.PaymentMethod
import com.sheshabiz.quickquote.data.db.entity.Product
import com.sheshabiz.quickquote.data.db.entity.Sale
import com.sheshabiz.quickquote.data.db.entity.SaleItem
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.repository.ProductRepository
import com.sheshabiz.quickquote.data.repository.SaleRepository
import com.sheshabiz.quickquote.domain.LineItemInput
import com.sheshabiz.quickquote.domain.QuoteCalculator
import com.sheshabiz.quickquote.domain.model.QuoteTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

private val cartKeyGenerator = AtomicLong(1)

data class CartLine(
    val key: Long = cartKeyGenerator.getAndIncrement(),
    val productId: Long?,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val availableStock: Double?
)

data class TillUiState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val cart: List<CartLine> = emptyList(),
    val customerId: Long? = null,
    val customerName: String = "",
    val vatEnabled: Boolean = true,
    val vatRate: Double = AppPreferences.DEFAULT_VAT_RATE,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isSaving: Boolean = false,
    val completedSaleId: Long? = null,
    val error: String? = null
) {
    val filteredProducts: List<Product>
        get() = if (searchQuery.isBlank()) products else products.filter { it.name.contains(searchQuery, ignoreCase = true) }

    val totals: QuoteTotals
        get() = QuoteCalculator.calculate(
            items = cart.map { LineItemInput(it.quantity, it.unitPrice) },
            vatEnabled = vatEnabled,
            vatRatePercent = vatRate,
            discountType = DiscountType.PERCENT,
            discountValue = 0.0
        )
}

class TillViewModel(
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TillUiState(vatEnabled = preferences.defaultVatEnabled, vatRate = preferences.vatRate)
    )
    val uiState: StateFlow<TillUiState> = _uiState

    init {
        viewModelScope.launch {
            productRepository.observeAll().collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun addProduct(product: Product) {
        _uiState.update { state ->
            val existing = state.cart.find { it.productId == product.id }
            if (existing != null) {
                val newQty = existing.quantity + 1
                if (product.trackStock && newQty > product.stockQuantity) {
                    return@update state.copy(error = "Only ${formatQty(product.stockQuantity)} left in stock.")
                }
                state.copy(cart = state.cart.map { if (it.key == existing.key) it.copy(quantity = newQty) else it }, error = null)
            } else {
                if (product.trackStock && product.stockQuantity <= 0) {
                    return@update state.copy(error = "${product.name} is out of stock.")
                }
                val line = CartLine(
                    productId = product.id,
                    description = product.name,
                    quantity = 1.0,
                    unitPrice = product.unitPrice,
                    availableStock = if (product.trackStock) product.stockQuantity else null
                )
                state.copy(cart = state.cart + line, error = null)
            }
        }
    }

    fun addCustomItem(description: String, quantity: Double, unitPrice: Double) {
        _uiState.update { state ->
            state.copy(
                cart = state.cart + CartLine(
                    productId = null,
                    description = description,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    availableStock = null
                ),
                error = null
            )
        }
    }

    fun incrementLine(key: Long) = updateLineQuantity(key, 1.0)
    fun decrementLine(key: Long) = updateLineQuantity(key, -1.0)

    private fun updateLineQuantity(key: Long, delta: Double) {
        _uiState.update { state ->
            val line = state.cart.find { it.key == key } ?: return@update state
            val newQty = line.quantity + delta
            if (newQty <= 0) {
                state.copy(cart = state.cart.filterNot { it.key == key })
            } else if (line.availableStock != null && newQty > line.availableStock) {
                state.copy(error = "Only ${formatQty(line.availableStock)} left in stock.")
            } else {
                state.copy(cart = state.cart.map { if (it.key == key) it.copy(quantity = newQty) else it }, error = null)
            }
        }
    }

    fun removeLine(key: Long) = _uiState.update { state -> state.copy(cart = state.cart.filterNot { it.key == key }) }

    fun onCustomerSelected(customer: Customer) =
        _uiState.update { it.copy(customerId = customer.id, customerName = customer.name) }

    fun onClearCustomer() = _uiState.update { it.copy(customerId = null, customerName = "") }

    /** Clears the one-shot navigation signal once the screen has acted on it, so re-entering
     * this (long-lived, tab-scoped) ViewModel's screen doesn't immediately re-navigate to the
     * same receipt again — which is what made the back button from Receipt look broken. */
    fun onSaleNavigationHandled() = _uiState.update { it.copy(completedSaleId = null) }

    fun onVatEnabledChange(enabled: Boolean) = _uiState.update { it.copy(vatEnabled = enabled) }
    fun onPaymentMethodChange(method: PaymentMethod) = _uiState.update { it.copy(paymentMethod = method) }
    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun completeSale() {
        val s = _uiState.value
        if (s.cart.isEmpty()) {
            _uiState.update { it.copy(error = "Add at least one item.") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val totals = s.totals
            val now = System.currentTimeMillis()
            val sale = Sale(
                saleNumber = preferences.reserveNextSaleNumber(),
                customerId = s.customerId,
                customerName = s.customerName.ifBlank { "Walk-in customer" },
                saleDate = now,
                vatEnabled = s.vatEnabled,
                vatRate = s.vatRate,
                discountType = DiscountType.PERCENT,
                discountValue = 0.0,
                subtotal = totals.subtotal,
                discountAmount = totals.discountAmount,
                vatAmount = totals.vatAmount,
                total = totals.total,
                paymentMethod = s.paymentMethod,
                notes = null,
                createdAt = now
            )
            val items = s.cart.mapIndexed { index, line ->
                SaleItem(
                    saleId = 0,
                    productId = line.productId,
                    description = line.description,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    lineTotal = QuoteCalculator.lineTotal(line.quantity, line.unitPrice),
                    sortOrder = index
                )
            }
            val saleId = saleRepository.completeSale(sale, items)
            _uiState.value = TillUiState(
                vatEnabled = preferences.defaultVatEnabled,
                vatRate = preferences.vatRate,
                completedSaleId = saleId
            )
        }
    }
}

private fun formatQty(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
