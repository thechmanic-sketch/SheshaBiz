package com.sheshabiz.quickquote.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerEditUiState(
    val customerId: Long = 0,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val nameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val isSaving: Boolean = false,
    val savedCustomerId: Long? = null
)

class CustomerEditViewModel(
    private val repository: CustomerRepository,
    private val existingCustomerId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerEditUiState())
    val uiState: StateFlow<CustomerEditUiState> = _uiState

    init {
        if (existingCustomerId != null && existingCustomerId > 0) {
            viewModelScope.launch {
                repository.getById(existingCustomerId)?.let { customer ->
                    _uiState.update {
                        it.copy(
                            customerId = customer.id,
                            name = customer.name,
                            phone = customer.phone,
                            email = customer.email.orEmpty(),
                            address = customer.address.orEmpty(),
                            createdAt = customer.createdAt
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v, phoneError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v) }

    fun save() {
        val s = _uiState.value
        val nameError = if (Validators.isBlank(s.name)) "Customer name is required." else null
        val phoneError = if (Validators.isBlank(s.phone)) "Phone number is required." else null
        val emailError = if (!Validators.isValidEmail(s.email)) "Enter a valid email address." else null

        if (listOf(nameError, phoneError, emailError).any { it != null }) {
            _uiState.update { it.copy(nameError = nameError, phoneError = phoneError, emailError = emailError) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val id = repository.upsert(
                Customer(
                    id = s.customerId,
                    name = s.name.trim(),
                    phone = s.phone.trim(),
                    email = s.email.trim().ifBlank { null },
                    address = s.address.trim().ifBlank { null },
                    createdAt = s.createdAt
                )
            )
            _uiState.update { it.copy(isSaving = false, savedCustomerId = id) }
        }
    }
}
