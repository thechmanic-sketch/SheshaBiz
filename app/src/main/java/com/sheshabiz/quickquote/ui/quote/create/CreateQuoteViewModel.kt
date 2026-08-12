package com.sheshabiz.quickquote.ui.quote.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteItem
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository
import com.sheshabiz.quickquote.domain.LineItemInput
import com.sheshabiz.quickquote.domain.QuoteCalculator
import com.sheshabiz.quickquote.domain.Validators
import com.sheshabiz.quickquote.domain.model.QuoteTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

private val keyGenerator = AtomicLong(1)

data class QuoteItemDraft(
    val key: Long = keyGenerator.getAndIncrement(),
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val descriptionError: String? = null,
    val quantityError: String? = null,
    val priceError: String? = null
)

data class CreateQuoteUiState(
    val quoteId: Long = 0,
    val quoteNumber: String = "",
    val isEditing: Boolean = false,
    val originalStatus: QuoteStatus = QuoteStatus.DRAFT,
    val originalCreatedAt: Long = System.currentTimeMillis(),
    val customerId: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val customerNameError: String? = null,
    val customerPhoneError: String? = null,
    val customerEmailError: String? = null,
    val quoteDate: Long = System.currentTimeMillis(),
    val validUntil: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
    val items: List<QuoteItemDraft> = listOf(QuoteItemDraft()),
    val itemsError: String? = null,
    val vatEnabled: Boolean = true,
    val vatRate: Double = AppPreferences.DEFAULT_VAT_RATE,
    val discountType: DiscountType = DiscountType.PERCENT,
    val discountValueText: String = "",
    val notes: String = "",
    val paymentTerms: String = AppPreferences.DEFAULT_PAYMENT_TERMS,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val savedQuoteId: Long? = null
) {
    val totals: QuoteTotals
        get() = QuoteCalculator.calculate(
            items = items.map { LineItemInput(it.quantity.toDoubleOrNull() ?: 0.0, it.unitPrice.toDoubleOrNull() ?: 0.0) },
            vatEnabled = vatEnabled,
            vatRatePercent = vatRate,
            discountType = discountType,
            discountValue = discountValueText.toDoubleOrNull() ?: 0.0
        )
}

class CreateQuoteViewModel(
    private val quoteRepository: QuoteRepository,
    private val customerRepository: CustomerRepository,
    private val preferences: AppPreferences,
    existingQuoteId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CreateQuoteUiState(
            vatEnabled = preferences.defaultVatEnabled,
            vatRate = preferences.vatRate,
            paymentTerms = preferences.defaultPaymentTerms
        )
    )
    val uiState: StateFlow<CreateQuoteUiState> = _uiState

    init {
        if (existingQuoteId != null && existingQuoteId > 0) {
            loadExisting(existingQuoteId)
        }
    }

    private fun loadExisting(quoteId: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val quote = quoteRepository.getById(quoteId) ?: return@launch
            val items = quoteRepository.getItemsForQuote(quoteId)
            _uiState.update {
                it.copy(
                    quoteId = quote.id,
                    quoteNumber = quote.quoteNumber,
                    isEditing = true,
                    originalStatus = quote.status,
                    originalCreatedAt = quote.createdAt,
                    customerId = quote.customerId,
                    customerName = quote.customerName,
                    customerPhone = quote.customerPhone,
                    customerEmail = quote.customerEmail.orEmpty(),
                    customerAddress = quote.customerAddress.orEmpty(),
                    quoteDate = quote.quoteDate,
                    validUntil = quote.validUntil,
                    items = if (items.isEmpty()) listOf(QuoteItemDraft()) else items.sortedBy { i -> i.sortOrder }.map { item ->
                        QuoteItemDraft(
                            description = item.description,
                            quantity = formatNumber(item.quantity),
                            unitPrice = formatNumber(item.unitPrice)
                        )
                    },
                    vatEnabled = quote.vatEnabled,
                    vatRate = quote.vatRate,
                    discountType = quote.discountType,
                    discountValueText = if (quote.discountValue == 0.0) "" else formatNumber(quote.discountValue),
                    notes = quote.notes.orEmpty(),
                    paymentTerms = quote.paymentTerms.orEmpty(),
                    isLoading = false
                )
            }
        }
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    fun onCustomerSelected(customer: Customer) {
        _uiState.update {
            it.copy(
                customerId = customer.id,
                customerName = customer.name,
                customerPhone = customer.phone,
                customerEmail = customer.email.orEmpty(),
                customerAddress = customer.address.orEmpty(),
                customerNameError = null,
                customerPhoneError = null
            )
        }
    }

    fun onClearCustomer() {
        _uiState.update {
            it.copy(customerId = null, customerName = "", customerPhone = "", customerEmail = "", customerAddress = "")
        }
    }

    fun onCustomerNameChange(v: String) = _uiState.update { it.copy(customerName = v, customerNameError = null) }
    fun onCustomerPhoneChange(v: String) = _uiState.update { it.copy(customerPhone = v, customerPhoneError = null) }
    fun onCustomerEmailChange(v: String) = _uiState.update { it.copy(customerEmail = v, customerEmailError = null) }
    fun onCustomerAddressChange(v: String) = _uiState.update { it.copy(customerAddress = v) }

    fun onQuoteDateChange(millis: Long) = _uiState.update { it.copy(quoteDate = millis) }
    fun onValidUntilChange(millis: Long) = _uiState.update { it.copy(validUntil = millis) }

    fun addItem() = _uiState.update { it.copy(items = it.items + QuoteItemDraft(), itemsError = null) }

    fun removeItem(key: Long) = _uiState.update { state ->
        val remaining = state.items.filterNot { it.key == key }
        state.copy(items = remaining.ifEmpty { listOf(QuoteItemDraft()) })
    }

    fun updateItemDescription(key: Long, value: String) = updateItem(key) { it.copy(description = value, descriptionError = null) }
    fun updateItemQuantity(key: Long, value: String) = updateItem(key) { it.copy(quantity = value, quantityError = null) }
    fun updateItemUnitPrice(key: Long, value: String) = updateItem(key) { it.copy(unitPrice = value, priceError = null) }

    private fun updateItem(key: Long, transform: (QuoteItemDraft) -> QuoteItemDraft) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.key == key) transform(it) else it })
        }
    }

    fun onVatEnabledChange(enabled: Boolean) = _uiState.update { it.copy(vatEnabled = enabled) }
    fun onDiscountTypeChange(type: DiscountType) = _uiState.update { it.copy(discountType = type) }
    fun onDiscountValueChange(value: String) = _uiState.update { it.copy(discountValueText = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onPaymentTermsChange(value: String) = _uiState.update { it.copy(paymentTerms = value) }

    fun save() {
        val s = _uiState.value

        val customerNameError = if (Validators.isBlank(s.customerName)) "Customer name is required." else null
        val customerPhoneError = if (Validators.isBlank(s.customerPhone)) "Customer phone is required." else null
        val customerEmailError = if (!Validators.isValidEmail(s.customerEmail)) "Enter a valid email address." else null

        val validatedItems = s.items.map { item ->
            item.copy(
                descriptionError = if (Validators.isBlank(item.description)) "Required" else null,
                quantityError = if (!Validators.isValidQuantity(item.quantity)) "Invalid" else null,
                priceError = if (!Validators.isValidPrice(item.unitPrice)) "Invalid" else null
            )
        }
        val itemsError = if (validatedItems.isEmpty()) "Add at least one item." else null
        val hasItemErrors = validatedItems.any { it.descriptionError != null || it.quantityError != null || it.priceError != null }

        if (customerNameError != null || customerPhoneError != null || customerEmailError != null || hasItemErrors || itemsError != null) {
            _uiState.update {
                it.copy(
                    customerNameError = customerNameError,
                    customerPhoneError = customerPhoneError,
                    customerEmailError = customerEmailError,
                    items = validatedItems,
                    itemsError = itemsError
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            var customerId = s.customerId
            if (customerId == null) {
                customerId = customerRepository.upsert(
                    Customer(
                        name = s.customerName.trim(),
                        phone = s.customerPhone.trim(),
                        email = s.customerEmail.trim().ifBlank { null },
                        address = s.customerAddress.trim().ifBlank { null },
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            val lineItems = s.items.map { LineItemInput(it.quantity.toDouble(), it.unitPrice.toDouble()) }
            val totals = QuoteCalculator.calculate(
                items = lineItems,
                vatEnabled = s.vatEnabled,
                vatRatePercent = s.vatRate,
                discountType = s.discountType,
                discountValue = s.discountValueText.toDoubleOrNull() ?: 0.0
            )
            val now = System.currentTimeMillis()

            val quote = Quote(
                id = s.quoteId,
                quoteNumber = if (s.isEditing) s.quoteNumber else preferences.reserveNextQuoteNumber(),
                customerId = customerId,
                customerName = s.customerName.trim(),
                customerPhone = s.customerPhone.trim(),
                customerEmail = s.customerEmail.trim().ifBlank { null },
                customerAddress = s.customerAddress.trim().ifBlank { null },
                quoteDate = s.quoteDate,
                validUntil = s.validUntil,
                vatEnabled = s.vatEnabled,
                vatRate = s.vatRate,
                discountType = s.discountType,
                discountValue = s.discountValueText.toDoubleOrNull() ?: 0.0,
                subtotal = totals.subtotal,
                discountAmount = totals.discountAmount,
                vatAmount = totals.vatAmount,
                total = totals.total,
                notes = s.notes.trim().ifBlank { null },
                paymentTerms = s.paymentTerms.trim().ifBlank { null },
                status = s.originalStatus,
                createdAt = if (s.isEditing) s.originalCreatedAt else now,
                updatedAt = now
            )

            val items = s.items.mapIndexed { index, draft ->
                val qty = draft.quantity.toDouble()
                val price = draft.unitPrice.toDouble()
                QuoteItem(
                    quoteId = s.quoteId,
                    description = draft.description.trim(),
                    quantity = qty,
                    unitPrice = price,
                    lineTotal = QuoteCalculator.lineTotal(qty, price),
                    sortOrder = index
                )
            }

            val finalId = if (s.isEditing) {
                quoteRepository.updateQuote(quote, items)
                s.quoteId
            } else {
                quoteRepository.createQuote(quote, items)
            }

            _uiState.update { it.copy(isSaving = false, savedQuoteId = finalId) }
        }
    }
}
