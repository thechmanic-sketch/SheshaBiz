package com.sheshabiz.quickquote.ui.quotes

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.ui.common.EmptyState
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.QuoteRowItem
import com.sheshabiz.quickquote.ui.common.ScreenTitleHeader

@Composable
fun QuotesListScreen(
    viewModel: QuotesListViewModel,
    onOpenQuote: (Long) -> Unit,
    onNewQuote: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleHeader(title = stringResource(R.string.nav_quotes))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
        Spacer(Modifier.height(16.dp))

        QQTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchChange,
            label = stringResource(R.string.search_quotes)
        )
        Spacer(Modifier.height(12.dp))

        FilterRow(
            selected = state.statusFilter,
            onSelect = viewModel::onStatusFilterChange,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SortMenu(selected = state.sortOption, onSelect = viewModel::onSortChange)
        }
        Spacer(Modifier.height(12.dp))

        if (state.quotes.isEmpty() && !state.isLoading) {
            EmptyState(
                title = stringResource(R.string.no_quotes_title),
                subtitle = stringResource(R.string.no_quotes_subtitle),
                actionLabel = stringResource(R.string.create_quote_cta),
                onAction = onNewQuote
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.quotes, key = { it.id }) { quote ->
                    QuoteRowItem(quote = quote, onClick = { onOpenQuote(quote.id) })
                }
            }
        }
        }
    }
}

@Composable
private fun FilterRow(
    selected: QuoteStatus?,
    onSelect: (QuoteStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val options: List<Pair<String, QuoteStatus?>> = listOf(
        stringResource(R.string.status_all) to null,
        stringResource(R.string.status_draft) to QuoteStatus.DRAFT,
        stringResource(R.string.status_sent) to QuoteStatus.SENT,
        stringResource(R.string.status_accepted) to QuoteStatus.ACCEPTED,
        stringResource(R.string.status_rejected) to QuoteStatus.REJECTED
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        items(options) { (label, status) ->
            val isSelected = selected == status
            Box(
                modifier = Modifier
                    .clickable { onSelect(status) }
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
private fun SortMenu(selected: SortOption, onSelect: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        SortOption.NEWEST to stringResource(R.string.sort_newest),
        SortOption.OLDEST to stringResource(R.string.sort_oldest),
        SortOption.HIGHEST to stringResource(R.string.sort_highest),
        SortOption.LOWEST to stringResource(R.string.sort_lowest)
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
