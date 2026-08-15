package com.sheshabiz.quickquote.ui.reports

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.domain.CurrencyFormat
import com.sheshabiz.quickquote.domain.PrintHelper
import com.sheshabiz.quickquote.ui.common.QQOutlinedButton
import com.sheshabiz.quickquote.ui.common.SectionLabel
import com.sheshabiz.quickquote.ui.common.ScreenTitleHeader
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun printReport() {
        scope.launch {
            val file = viewModel.generatePdfFile()
            if (file != null) {
                PrintHelper.printPdf(context, file, "Report — ${state.period.label}")
            } else {
                Toast.makeText(
                    context,
                    "Set up your business profile first (Settings > Business profile).",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        ScreenTitleHeader(title = "Reports", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReportPeriod.entries) { period ->
                    FilterChip(
                        selected = state.period == period,
                        onClick = { viewModel.onPeriodChange(period) },
                        label = { Text(period.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            QQOutlinedButton(text = "Print report", onClick = ::printReport)
            Spacer(Modifier.height(24.dp))

            SectionLabel("Sales (POS)")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportStatCard(
                    value = CurrencyFormat.format(state.salesTotal),
                    label = "Total sales",
                    modifier = Modifier.weight(1f)
                )
                ReportStatCard(
                    value = state.salesCount.toString(),
                    label = "Sales made",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(24.dp))

            SectionLabel("Quotations")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportStatCard(
                    value = state.quotesCreated.toString(),
                    label = "Quotes created",
                    modifier = Modifier.weight(1f)
                )
                ReportStatCard(
                    value = state.quotesAccepted.toString(),
                    label = "Quotes accepted",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            ReportStatCard(
                value = CurrencyFormat.format(state.quotesTotal),
                label = "Total quoted",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            SectionLabel("Invoices")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportStatCard(
                    value = CurrencyFormat.format(state.invoicesPaidTotal),
                    label = "Paid (${state.invoicesPaidCount})",
                    modifier = Modifier.weight(1f)
                )
                ReportStatCard(
                    value = CurrencyFormat.format(state.invoicesUnpaidTotal),
                    label = "Unpaid (${state.invoicesUnpaidCount})",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            ReportStatCard(
                value = state.invoicesOverdueCount.toString(),
                label = "Overdue invoices",
                modifier = Modifier.fillMaxWidth(),
                emphasizeAsWarning = state.invoicesOverdueCount > 0
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ReportStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    emphasizeAsWarning: Boolean = false
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (emphasizeAsWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
