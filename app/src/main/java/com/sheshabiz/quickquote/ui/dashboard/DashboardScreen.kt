package com.sheshabiz.quickquote.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.R
import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.prefs.SubscriptionState
import com.sheshabiz.quickquote.domain.CurrencyFormat
import com.sheshabiz.quickquote.domain.TrialGate
import com.sheshabiz.quickquote.ui.common.EmptyState
import com.sheshabiz.quickquote.ui.common.QQTextActionButton
import com.sheshabiz.quickquote.ui.common.QuoteRowItem
import com.sheshabiz.quickquote.ui.common.ScreenTitleHeader
import com.sheshabiz.quickquote.ui.theme.BrandGradientEnd
import com.sheshabiz.quickquote.ui.theme.BrandGradientStart
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    authPreferences: AuthPreferences,
    onNewQuote: () -> Unit,
    onNewInvoice: () -> Unit,
    onOpenTill: () -> Unit,
    onOpenQuote: (Long) -> Unit,
    onSeeAllQuotes: () -> Unit,
    onOpenMore: () -> Unit = {},
    onOpenInvoices: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val subscriptionState by authPreferences.subscriptionState.collectAsState()
    val validUntil by authPreferences.validUntil.collectAsState()
    val greeting = remember { greetingForCurrentTime() }
    var trialEndingBannerDismissed by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleHeader(
            title = state.businessName.ifBlank { stringResource(R.string.app_name) },
            subtitle = greeting,
            onMenuClick = onOpenMore,
            onSearchClick = onSeeAllQuotes,
            isDarkTheme = isDarkTheme,
            onThemeToggle = onToggleTheme,
            onNotificationsClick = onOpenInvoices,
            notificationCount = state.overdueInvoiceCount
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
        Spacer(Modifier.height(20.dp))

        val daysLeft = validUntil?.let { (it - System.currentTimeMillis()).toDouble() / TimeUnit.DAYS.toMillis(1) }
        when {
            subscriptionState == SubscriptionState.LAPSED -> {
                TrialBanner(
                    message = TrialGate.LOCKED_MESSAGE,
                    isPersistent = true,
                    onDismiss = {}
                )
                Spacer(Modifier.height(16.dp))
            }
            subscriptionState == SubscriptionState.TRIALING && daysLeft != null && daysLeft in 0.0..2.0 && !trialEndingBannerDismissed -> {
                TrialBanner(
                    message = "Your trial ends soon. WhatsApp or call 063 353 1662 to activate SheshaBiz.",
                    isPersistent = false,
                    onDismiss = { trialEndingBannerDismissed = true }
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        HeroCard(
            totalQuoted = state.totalQuoted,
            onNewQuote = onNewQuote,
            onNewInvoice = onNewInvoice,
            onOpenTill = onOpenTill
        )
        Spacer(Modifier.height(20.dp))

        StatsGrid(state = state)
        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.recent_quotes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (state.recentQuotes.isNotEmpty()) {
                QQTextActionButton(text = stringResource(R.string.see_all), onClick = onSeeAllQuotes)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (state.recentQuotes.isEmpty() && !state.isLoading) {
            EmptyState(
                title = stringResource(R.string.no_quotes_title),
                subtitle = stringResource(R.string.no_quotes_subtitle),
                actionLabel = stringResource(R.string.create_quote_cta),
                onAction = onNewQuote
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.recentQuotes.forEach { quote ->
                    QuoteRowItem(quote = quote, onClick = { onOpenQuote(quote.id) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun HeroCard(
    totalQuoted: Double,
    onNewQuote: () -> Unit,
    onNewInvoice: () -> Unit,
    onOpenTill: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(BrandGradientStart, BrandGradientEnd),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Text(
            text = "Total quoted",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = CurrencyFormat.format(totalQuoted),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeroAction(icon = Icons.Filled.ReceiptLong, label = stringResource(R.string.new_quote), onClick = onNewQuote)
            HeroAction(icon = Icons.Filled.RequestQuote, label = "New Invoice", onClick = onNewInvoice)
            HeroAction(icon = Icons.Filled.PointOfSale, label = "New Sale", onClick = onOpenTill)
        }
    }
}

@Composable
private fun HeroAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label.removePrefix("+ "),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatsGrid(state: DashboardUiState) {
    val stats = listOf(
        stringResource(R.string.stat_quotes_created) to state.quotesCreated.toString(),
        stringResource(R.string.stat_quotes_sent) to state.quotesSent.toString(),
        stringResource(R.string.stat_quotes_accepted) to state.quotesAccepted.toString(),
        stringResource(R.string.stat_total_quoted) to CurrencyFormat.format(state.totalQuoted)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in stats.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value) ->
                    StatCard(label = label, value = value, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** [isPersistent] (lapsed trial) hides the close button entirely — it's non-blocking (the
 * rest of the dashboard still works) but shouldn't be dismissible, so it keeps reminding the
 * owner on every visit until they pay. A still-[SubscriptionState.TRIALING] banner is just a
 * heads-up and can be dismissed for the session. */
@Composable
private fun TrialBanner(message: String, isPersistent: Boolean, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(14.dp))
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = if (isPersistent) 16.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        if (!isPersistent) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun greetingForCurrentTime(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}
