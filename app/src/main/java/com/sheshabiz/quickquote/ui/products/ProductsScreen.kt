package com.sheshabiz.quickquote.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.data.db.entity.Product
import com.sheshabiz.quickquote.domain.CurrencyFormat
import com.sheshabiz.quickquote.ui.common.EmptyState
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenTitleHeader

@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel,
    onAddProduct: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleHeader(title = "Products", onBack = onBack)

        Scaffold(
            modifier = Modifier.weight(1f),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                FloatingActionButton(onClick = onAddProduct, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Filled.Add, contentDescription = "Add product", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                if (state.allProducts.isNotEmpty()) {
                    QQTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchChange,
                        label = "Search products"
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (state.allProducts.isEmpty() && !state.isLoading) {
                    EmptyState(
                        title = "No products yet",
                        subtitle = "Add items or services to quickly reuse them in quotes, invoices, and POS sales.",
                        actionLabel = "Add Product",
                        onAction = onAddProduct
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(state.filteredProducts, key = { it.id }) { product ->
                            ProductRow(product = product, onClick = { onOpenProduct(product.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductThumbnail(imageUri = product.imageUri)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            if (product.trackStock) {
                val isLow = product.lowStockThreshold != null && product.stockQuantity <= product.lowStockThreshold
                Text(
                    text = "Stock: ${formatQty(product.stockQuantity)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Service — no stock tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(CurrencyFormat.format(product.unitPrice), fontWeight = FontWeight.SemiBold)
    }
}

private fun formatQty(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
