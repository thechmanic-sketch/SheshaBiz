package com.sheshabiz.quickquote.ui.products

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Product
import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.repository.ProductRepository
import com.sheshabiz.quickquote.domain.TrialGate
import com.sheshabiz.quickquote.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductEditUiState(
    val productId: Long = 0,
    val name: String = "",
    val unitPriceText: String = "",
    val sku: String = "",
    val trackStock: Boolean = false,
    val stockQuantityText: String = "0",
    val lowStockThresholdText: String = "",
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val nameError: String? = null,
    val priceError: String? = null,
    val stockError: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val savedProductId: Long? = null,
    val deleted: Boolean = false,
    val lockedMessage: String? = null
)

class ProductEditViewModel(
    private val repository: ProductRepository,
    private val authPreferences: AuthPreferences,
    private val existingProductId: Long?,
    private val copyImageToInternalStorage: suspend (Uri) -> String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductEditUiState())
    val uiState: StateFlow<ProductEditUiState> = _uiState

    init {
        if (existingProductId != null && existingProductId > 0) {
            viewModelScope.launch {
                repository.getById(existingProductId)?.let { product ->
                    _uiState.update {
                        it.copy(
                            productId = product.id,
                            name = product.name,
                            unitPriceText = formatNumber(product.unitPrice),
                            sku = product.sku.orEmpty(),
                            trackStock = product.trackStock,
                            stockQuantityText = formatNumber(product.stockQuantity),
                            lowStockThresholdText = product.lowStockThreshold?.let { t -> formatNumber(t) }.orEmpty(),
                            imageUri = product.imageUri,
                            createdAt = product.createdAt
                        )
                    }
                }
            }
        }
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val savedPath = copyImageToInternalStorage(uri)
            if (savedPath != null) {
                _uiState.update { it.copy(imageUri = savedPath) }
            }
        }
    }

    fun onRemoveImage() = _uiState.update { it.copy(imageUri = null) }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null) }
    fun onUnitPriceChange(v: String) = _uiState.update { it.copy(unitPriceText = v, priceError = null) }
    fun onSkuChange(v: String) = _uiState.update { it.copy(sku = v) }
    fun onTrackStockChange(v: Boolean) = _uiState.update { it.copy(trackStock = v) }
    fun onStockQuantityChange(v: String) = _uiState.update { it.copy(stockQuantityText = v, stockError = null) }
    fun onLowStockThresholdChange(v: String) = _uiState.update { it.copy(lowStockThresholdText = v) }

    fun clearLockedMessage() = _uiState.update { it.copy(lockedMessage = null) }

    fun save() {
        if (TrialGate.isLocked(authPreferences)) {
            _uiState.update { it.copy(lockedMessage = TrialGate.LOCKED_MESSAGE) }
            return
        }

        val s = _uiState.value
        val nameError = if (Validators.isBlank(s.name)) "Product name is required." else null
        val priceError = if (!Validators.isValidPrice(s.unitPriceText)) "Enter a valid price." else null
        val stockError = if (s.trackStock && s.stockQuantityText.toDoubleOrNull() == null) "Enter a valid quantity." else null

        if (nameError != null || priceError != null || stockError != null) {
            _uiState.update { it.copy(nameError = nameError, priceError = priceError, stockError = stockError) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = repository.upsert(
                Product(
                    id = s.productId,
                    name = s.name.trim(),
                    unitPrice = s.unitPriceText.toDouble(),
                    sku = s.sku.trim().ifBlank { null },
                    trackStock = s.trackStock,
                    stockQuantity = if (s.trackStock) (s.stockQuantityText.toDoubleOrNull() ?: 0.0) else 0.0,
                    lowStockThreshold = if (s.trackStock) s.lowStockThresholdText.toDoubleOrNull() else null,
                    imageUri = s.imageUri,
                    createdAt = if (s.productId == 0L) now else s.createdAt,
                    updatedAt = now
                )
            )
            _uiState.update { it.copy(isSaving = false, savedProductId = id) }
        }
    }

    fun delete() {
        val s = _uiState.value
        if (s.productId == 0L) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            repository.getById(s.productId)?.let { repository.delete(it) }
            _uiState.update { it.copy(isDeleting = false, deleted = true) }
        }
    }
}
