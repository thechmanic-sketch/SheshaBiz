package com.sheshabiz.quickquote.ui.businesssetup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BusinessSetupUiState(
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val address: String = "",
    val vatNumber: String = "",
    val logoUri: String? = null,
    val businessNameError: String? = null,
    val ownerNameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class BusinessSetupViewModel(
    private val businessRepository: BusinessRepository,
    private val preferences: AppPreferences,
    private val copyLogoToInternalStorage: suspend (Uri) -> String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessSetupUiState())
    val uiState: StateFlow<BusinessSetupUiState> = _uiState

    init {
        viewModelScope.launch {
            businessRepository.getProfile()?.let { profile ->
                _uiState.update {
                    it.copy(
                        businessName = profile.businessName,
                        ownerName = profile.ownerName,
                        phone = profile.phone,
                        whatsappNumber = profile.whatsappNumber,
                        email = profile.email,
                        address = profile.address,
                        vatNumber = profile.vatNumber.orEmpty(),
                        logoUri = profile.logoUri
                    )
                }
            }
        }
    }

    fun onBusinessNameChange(v: String) = _uiState.update { it.copy(businessName = v, businessNameError = null) }
    fun onOwnerNameChange(v: String) = _uiState.update { it.copy(ownerName = v, ownerNameError = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v, phoneError = null) }
    fun onWhatsappChange(v: String) = _uiState.update { it.copy(whatsappNumber = v) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v) }
    fun onVatNumberChange(v: String) = _uiState.update { it.copy(vatNumber = v) }

    fun onLogoPicked(uri: Uri) {
        viewModelScope.launch {
            val savedPath = copyLogoToInternalStorage(uri)
            if (savedPath != null) {
                _uiState.update { it.copy(logoUri = savedPath) }
            }
        }
    }

    fun onRemoveLogo() = _uiState.update { it.copy(logoUri = null) }

    fun save() {
        val s = _uiState.value
        val businessNameError = if (Validators.isBlank(s.businessName)) "Business name is required." else null
        val ownerNameError = if (Validators.isBlank(s.ownerName)) "Owner name is required." else null
        val phoneError = when {
            Validators.isBlank(s.phone) -> "Phone number is required."
            !Validators.isValidPhone(s.phone) -> "Enter a valid phone number."
            else -> null
        }
        val emailError = if (!Validators.isValidEmail(s.email)) "Enter a valid email address." else null

        if (listOf(businessNameError, ownerNameError, phoneError, emailError).any { it != null }) {
            _uiState.update {
                it.copy(
                    businessNameError = businessNameError,
                    ownerNameError = ownerNameError,
                    phoneError = phoneError,
                    emailError = emailError
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            businessRepository.saveProfile(
                BusinessProfile(
                    businessName = s.businessName.trim(),
                    ownerName = s.ownerName.trim(),
                    phone = s.phone.trim(),
                    whatsappNumber = s.whatsappNumber.trim().ifBlank { s.phone.trim() },
                    email = s.email.trim(),
                    address = s.address.trim(),
                    vatNumber = s.vatNumber.trim().ifBlank { null },
                    logoUri = s.logoUri
                )
            )
            preferences.setBusinessSetupComplete(true)
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
