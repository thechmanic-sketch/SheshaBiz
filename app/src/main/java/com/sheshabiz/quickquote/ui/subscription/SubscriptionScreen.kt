package com.sheshabiz.quickquote.ui.subscription

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.ScreenTitleHeader
import com.sheshabiz.quickquote.ui.common.SectionLabel

/** International-format WhatsApp number for the business (063 353 1662). */
private const val WHATSAPP_NUMBER = "27633531662"

/** Static EFT banking details for subscription payments — separate from the business's own
 * bank details in [com.sheshabiz.quickquote.data.db.entity.BusinessProfile], which are used
 * on the business's own invoices, not here. */
private const val EFT_BANK = "ABSA Bank"
private const val EFT_ACCOUNT_NUMBER = "9401 3815 23"
private const val EFT_BRANCH_CODE = "632005"

/**
 * Customer-facing subscription flow: pick a plan, see the EFT payment details, then notify the
 * business owner on WhatsApp once paid. There is no admin capability here and this screen never
 * calls any activation RPC — an admin confirms the payment and flips the account's status from
 * the separate, web-only admin tool.
 */
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    authPreferences: AuthPreferences,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isLoggedIn by authPreferences.isLoggedIn.collectAsState()
    val loggedInEmail by authPreferences.loggedInEmail.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleHeader(
            title = "Subscription",
            onBack = {
                if (isLoggedIn && state.step == SubscriptionStep.PAY_CONFIRM) {
                    viewModel.backToPlanPicker()
                } else {
                    onBack()
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            when {
                !isLoggedIn -> LoggedOutContent(onNavigateToLogin = onNavigateToLogin)
                state.step == SubscriptionStep.PICK_PLAN -> PlanPickerContent(onSelectPlan = viewModel::selectPlan)
                else -> {
                    val plan = state.selectedPlan
                    if (plan != null) {
                        PayConfirmContent(
                            plan = plan,
                            businessName = state.businessName,
                            email = loggedInEmail.orEmpty(),
                            onNotifyWhatsApp = { message ->
                                val encoded = Uri.encode(message)
                                val url = "https://wa.me/$WHATSAPP_NUMBER?text=$encoded"
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }.onFailure {
                                    Toast.makeText(context, "Couldn't open WhatsApp.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        // Shouldn't happen — PAY_CONFIRM is only entered together with a
                        // selected plan — but fall back to the picker rather than crash.
                        PlanPickerContent(onSelectPlan = viewModel::selectPlan)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoggedOutContent(onNavigateToLogin: () -> Unit) {
    Text(
        "You need an account first",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Log in or create a free account to see subscription plans and pay.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(24.dp))
    QQPrimaryButton(text = "Log in / Create account", onClick = onNavigateToLogin)
}

@Composable
private fun PlanPickerContent(onSelectPlan: (SubscriptionPlan) -> Unit) {
    Text(
        "Choose a plan",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Pick the plan that works for you. You'll pay via EFT on the next step.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(20.dp))

    SubscriptionPlan.entries.forEach { plan ->
        PlanCard(plan = plan, onClick = { onSelectPlan(plan) })
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PlanCard(plan: SubscriptionPlan, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(plan.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(plan.periodLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            plan.priceLabel,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PayConfirmContent(
    plan: SubscriptionPlan,
    businessName: String,
    email: String,
    onNotifyWhatsApp: (String) -> Unit
) {
    val reference = businessName.ifBlank { "your business name" }

    Text(
        "Pay via EFT",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "You selected ${plan.displayName} — ${plan.priceLabel} (${plan.periodLabel}).",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(20.dp))

    SectionLabel("Banking details")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(vertical = 4.dp)
    ) {
        EftDetailRow(label = "Bank", value = EFT_BANK)
        Divider(color = MaterialTheme.colorScheme.outline)
        EftDetailRow(label = "Account Number", value = EFT_ACCOUNT_NUMBER)
        Divider(color = MaterialTheme.colorScheme.outline)
        EftDetailRow(label = "Branch Code", value = EFT_BRANCH_CODE)
        Divider(color = MaterialTheme.colorScheme.outline)
        EftDetailRow(label = "Reference", value = reference)
    }
    Spacer(Modifier.height(28.dp))

    QQPrimaryButton(
        text = "I've paid — Notify us on WhatsApp",
        onClick = {
            val message = "Hi, I've paid for the ${plan.whatsappLabel} plan for $reference. My account email is $email."
            onNotifyWhatsApp(message)
        }
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "We'll activate your account once we've confirmed the payment — this isn't instant.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EftDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
