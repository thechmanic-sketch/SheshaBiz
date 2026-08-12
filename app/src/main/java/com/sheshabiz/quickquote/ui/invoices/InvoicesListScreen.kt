package com.sheshabiz.quickquote.ui.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.R
import com.sheshabiz.quickquote.ui.common.EmptyState
import com.sheshabiz.quickquote.ui.common.InvoiceRowItem
import com.sheshabiz.quickquote.ui.common.QQTextField

@Composable
fun InvoicesListScreen(
    viewModel: InvoicesListViewModel,
    onOpenInvoice: (Long) -> Unit,
    onNewInvoice: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewInvoice, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_invoice), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.nav_invoices),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            QQTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = stringResource(R.string.search_invoices)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterRow(
                    selected = state.filter,
                    onSelect = viewModel::onFilterChange,
                    modifier = Modifier.weight(1f)
                )
                SortMenu(selected = state.sortOption, onSelect = viewModel::onSortChange)
            }
            Spacer(Modifier.height(16.dp))

            if (state.invoices.isEmpty() && !state.isLoading) {
                EmptyState(
                    title = stringResource(R.string.no_invoices_title),
                    subtitle = stringResource(R.string.no_invoices_subtitle),
                    actionLabel = stringResource(R.string.create_invoice_cta),
                    onAction = onNewInvoice
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(state.invoices, key = { it.id }) { invoice ->
                        InvoiceRowItem(invoice = invoice, onClick = { onOpenInvoice(invoice.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: InvoiceFilter,
    onSelect: (InvoiceFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val options: List<Pair<String, InvoiceFilter>> = listOf(
        stringResource(R.string.status_all) to InvoiceFilter.ALL,
        stringResource(R.string.invoice_status_unpaid) to InvoiceFilter.UNPAID,
        stringResource(R.string.invoice_status_overdue) to InvoiceFilter.OVERDUE,
        stringResource(R.string.invoice_status_paid) to InvoiceFilter.PAID,
        stringResource(R.string.invoice_status_cancelled) to InvoiceFilter.CANCELLED
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        items(options) { (label, filterOption) ->
            val isSelected = selected == filterOption
            Box(
                modifier = Modifier
                    .clickable { onSelect(filterOption) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun SortMenu(selected: InvoiceSortOption, onSelect: (InvoiceSortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        InvoiceSortOption.NEWEST to stringResource(R.string.sort_newest),
        InvoiceSortOption.OLDEST to stringResource(R.string.sort_oldest),
        InvoiceSortOption.HIGHEST to stringResource(R.string.sort_highest),
        InvoiceSortOption.LOWEST to stringResource(R.string.sort_lowest)
    )
    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(labels[selected].orEmpty(), style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEach { (option, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onSelect(option)
                    expanded = false
                })
            }
        }
    }
}
