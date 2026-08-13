package com.sheshabiz.quickquote.ui.invoice.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.data.db.entity.Invoice
import com.sheshabiz.quickquote.data.db.entity.InvoiceItem
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus
import com.sheshabiz.quickquote.data.db.entity.Product
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.data.repository.InvoiceRepository
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

data class InvoiceItemDraft(
    val key: Long = keyGenerator.getAndIncrement(),
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val descriptionError: String? = null,
    val quantityError: String? = null,
    val priceError: String? = null
)

data class CreateInvoiceUiState(
    val invoiceId: Long = 0,
    val invoiceNumber: String = "",
    val isEditing: Boolean = false,
    val originalStatus: InvoiceStatus = InvoiceStatus.UNPAID,
    val originalPaidAt: Long? = null,
    val originalCreatedAt: Long = System.currentTimeMillis(),
    val sourceQuoteId: Long? = null,
    val customerId: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val customerNameError: String? = null,
    val customerPhoneError: String? = null,
    val customerEmailError: String? = null,
    val invoiceDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
    val items: List<InvoiceItemDraft> = listOf(InvoiceItemDraft()),
    val itemsError: String? = null,
    val vatEnabled: Boolean = true,
    val vatRate: Double = AppPreferences.DEFAULT_VAT_RATE,
    val discountType: DiscountType = DiscountType.PERCENT,
    val discountValueText: String = "",
    val notes: String = "",
    val paymentTerms: String = AppPreferences.DEFAULT_PAYMENT_TERMS,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val savedInvoiceId: Long? = null
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

class CreateInvoiceViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val preferences: AppPreferences,
    existingInvoiceId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CreateInvoiceUiState(
            vatEnabled = preferences.defaultVatEnabled,
            vatRate = preferences.vatRate,
            paymentTerms = preferences.defaultPaymentTerms
        )
    )
    val uiState: StateFlow<CreateInvoiceUiState> = _uiState

    init {
        if (existingInvoiceId != null && existingInvoiceId > 0) {
            loadExisting(existingInvoiceId)
        }
    }

    private fun loadExisting(invoiceId: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val invoice = invoiceRepository.getById(invoiceId) ?: return@launch
            val items = invoiceRepository.getItemsForInvoice(invoiceId)
            _uiState.update {
                it.copy(
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    isEditing = true,
                    originalStatus = invoice.status,
                    originalPaidAt = invoice.paidAt,
                    originalCreatedAt = invoice.createdAt,
                    sourceQuoteId = invoice.sourceQuoteId,
                    customerId = invoice.customerId,
                    customerName = invoice.customerName,
                    customerPhone = invoice.customerPhone,
                    customerEmail = invoice.customerEmail.orEmpty(),
                    customerAddress = invoice.customerAddress.orEmpty(),
                    invoiceDate = invoice.invoiceDate,
                    dueDate = invoice.dueDate,
                    items = if (items.isEmpty()) listOf(InvoiceItemDraft()) else items.sortedBy { i -> i.sortOrder }.map { item ->
                        InvoiceItemDraft(
                            description = item.description,
                            quantity = formatNumber(item.quantity),
                            unitPrice = formatNumber(item.unitPrice)
                        )
                    },
                    vatEnabled = invoice.vatEnabled,
                    vatRate = invoice.vatRate,
                    discountType = invoice.discountType,
                    discountValueText = if (invoice.discountValue == 0.0) "" else formatNumber(invoice.discountValue),
                    notes = invoice.notes.orEmpty(),
                    paymentTerms = invoice.paymentTerms.orEmpty(),
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

    fun onInvoiceDateChange(millis: Long) = _uiState.update { it.copy(invoiceDate = millis) }
    fun onDueDateChange(millis: Long) = _uiState.update { it.copy(dueDate = millis) }

    fun addItem() = _uiState.update { it.copy(items = it.items + InvoiceItemDraft(), itemsError = null) }

    fun addItemFromProduct(product: Product) = _uiState.update { state ->
        val newItem = InvoiceItemDraft(
            description = product.name,
            quantity = "1",
            unitPrice = formatNumber(product.unitPrice)
        )
        val onlyEmptyItem = state.items.size == 1 &&
            state.items[0].description.isBlank() && state.items[0].unitPrice.isBlank()
        state.copy(
            items = if (onlyEmptyItem) listOf(newItem) else state.items + newItem,
            itemsError = null
        )
    }

    fun removeItem(key: Long) = _uiState.update { state ->
        val remaining = state.items.filterNot { it.key == key }
        state.copy(items = remaining.ifEmpty { listOf(InvoiceItemDraft()) })
    }

    fun updateItemDescription(key: Long, value: String) = updateItem(key) { it.copy(description = value, descriptionError = null) }
    fun updateItemQuantity(key: Long, value: String) = updateItem(key) { it.copy(quantity = value, quantityError = null) }
    fun updateItemUnitPrice(key: Long, value: String) = updateItem(key) { it.copy(unitPrice = value, priceError = null) }

    private fun updateItem(key: Long, transform: (InvoiceItemDraft) -> InvoiceItemDraft) {
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
        val customerPhoneError = when {
            Validators.isBlank(s.customerPhone) -> "Customer phone is required."
            !Validators.isValidPhone(s.customerPhone) -> "Enter a valid phone number."
            else -> null
        }
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

            val invoice = Invoice(
                id = s.invoiceId,
                invoiceNumber = if (s.isEditing) s.invoiceNumber else preferences.reserveNextInvoiceNumber(),
                sourceQuoteId = s.sourceQuoteId,
                customerId = customerId,
                customerName = s.customerName.trim(),
                customerPhone = s.customerPhone.trim(),
                customerEmail = s.customerEmail.trim().ifBlank { null },
                customerAddress = s.customerAddress.trim().ifBlank { null },
                invoiceDate = s.invoiceDate,
                dueDate = s.dueDate,
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
                paidAt = s.originalPaidAt,
                createdAt = if (s.isEditing) s.originalCreatedAt else now,
                updatedAt = now
            )

            val items = s.items.mapIndexed { index, draft ->
                val qty = draft.quantity.toDouble()
                val price = draft.unitPrice.toDouble()
                InvoiceItem(
                    invoiceId = s.invoiceId,
                    description = draft.description.trim(),
                    quantity = qty,
                    unitPrice = price,
                    lineTotal = QuoteCalculator.lineTotal(qty, price),
                    sortOrder = index
                )
            }

            val finalId = if (s.isEditing) {
                invoiceRepository.updateInvoice(invoice, items)
                s.invoiceId
            } else {
                invoiceRepository.createInvoice(invoice, items)
            }

            _uiState.update { it.copy(isSaving = false, savedInvoiceId = finalId) }
        }
    }
}
