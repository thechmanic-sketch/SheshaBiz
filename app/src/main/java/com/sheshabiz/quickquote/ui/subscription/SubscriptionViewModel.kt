package com.sheshabiz.quickquote.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Display-only pricing tiers. This screen never calls any activation RPC — Android has no
 * admin capability — it only helps the customer pay via EFT and notify the business owner on
 * WhatsApp; the owner activates the account manually from the (separate, web-only) admin tool. */
enum class SubscriptionPlan(val displayName: String, val priceLabel: String, val periodLabel: String) {
    MONTHLY("Monthly", "R30", "per month"),
    THREE_MONTHS("3 Months", "R70", "total, every 3 months"),
    SIX_MONTHS("6 Months", "R120", "total, every 6 months"),
    ONE_YEAR("1 Year", "R200", "total, per year");

    /** Short "<name> (R<price>)" form used in the WhatsApp confirmation message. */
    val whatsappLabel: String get() = "$displayName ($priceLabel)"
}

enum class SubscriptionStep { PICK_PLAN, PAY_CONFIRM }

data class SubscriptionUiState(
    val step: SubscriptionStep = SubscriptionStep.PICK_PLAN,
    val selectedPlan: SubscriptionPlan? = null,
    val businessName: String = ""
)

class SubscriptionViewModel(
    private val businessRepository: BusinessRepository,
    /** [SubscriptionPlan.name] of a plan already chosen elsewhere (e.g. on the signup
     * "how would you like to start?" step) — when present and valid, the screen opens straight
     * on [SubscriptionStep.PAY_CONFIRM] for that plan instead of making the user pick again. */
    preselectedPlanId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        preselectedPlanId
            ?.let { id -> runCatching { SubscriptionPlan.valueOf(id) }.getOrNull() }
            ?.let { plan -> SubscriptionUiState(step = SubscriptionStep.PAY_CONFIRM, selectedPlan = plan) }
            ?: SubscriptionUiState()
    )
    val uiState: StateFlow<SubscriptionUiState> = _uiState

    init {
        viewModelScope.launch {
            val name = businessRepository.getProfile()?.businessName.orEmpty()
            _uiState.update { it.copy(businessName = name) }
        }
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _uiState.update { it.copy(selectedPlan = plan, step = SubscriptionStep.PAY_CONFIRM) }
    }

    /** Step-2 → step-1 in-screen back action (distinct from the screen's nav back). */
    fun backToPlanPicker() {
        _uiState.update { it.copy(step = SubscriptionStep.PICK_PLAN) }
    }
}
