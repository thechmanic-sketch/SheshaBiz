package com.sheshabiz.quickquote.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.PaymentMethod
import com.sheshabiz.quickquote.data.db.entity.Product
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.domain.CurrencyFormat
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextActionButton
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenTitleHeader
import com.sheshabiz.quickquote.ui.customers.CustomerPickerSheet

@Composable
fun TillScreen(
    viewModel: TillViewModel,
    customerRepository: CustomerRepository,
    onSaleCompleted: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val customers by customerRepository.observeAll().collectAsState(initial = emptyList<Customer>())
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCustomItemDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.completedSaleId) {
        state.completedSaleId?.let {
            onSaleCompleted(it)
            viewModel.onSaleNavigationHandled()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleHeader(title = "Till", subtitle = "New sale")

        Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(12.dp))
            QQTextField(value = state.searchQuery, onValueChange = viewModel::onSearchChange, label = "Search products")
            Spacer(Modifier.height(8.dp))
            QQTextActionButton(text = "+ Custom item", onClick = { showCustomItemDialog = true })

            if (state.products.isEmpty()) {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Text(
                        "No products in your catalog yet.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Add products from Products (under More), or tap \"+ Custom item\" above to sell something one-off.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(state.filteredProducts, key = { it.id }) { product ->
                        ProductTile(product = product, onClick = { viewModel.addProduct(product) })
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            if (state.error != null) {
                Snackbar(modifier = Modifier.padding(bottom = 12.dp)) { Text(state.error!!) }
            }

            if (state.cart.isNotEmpty()) {
                Column(modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    state.cart.forEach { line -> CartLineRow(line, viewModel) }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.customerName.isBlank()) "Walk-in customer" else state.customerName,
                    style = MaterialTheme.typography.bodyMedium
                )
                QQTextActionButton(text = "Change", onClick = { showCustomerPicker = true })
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.entries.forEach { method ->
                    PaymentChip(
                        label = method.name,
                        selected = state.paymentMethod == method,
                        onClick = { viewModel.onPaymentMethodChange(method) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    CurrencyFormat.format(state.totals.total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            QQPrimaryButton(text = "Complete Sale", onClick = viewModel::completeSale, loading = state.isSaving, enabled = state.cart.isNotEmpty())
        }
    }

    if (showCustomerPicker) {
        CustomerPickerSheet(
            customers = customers,
            onDismiss = { showCustomerPicker = false },
            onSelect = { viewModel.onCustomerSelected(it); showCustomerPicker = false },
            onAddNew = { viewModel.onClearCustomer(); showCustomerPicker = false }
        )
    }

    if (showCustomItemDialog) {
        CustomItemDialog(
            onDismiss = { showCustomItemDialog = false },
            onAdd = { description, qty, price ->
                viewModel.addCustomItem(description, qty, price)
                showCustomItemDialog = false
            }
        )
    }
}

@Composable
private fun ProductTile(product: Product, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.sheshabiz.quickquote.ui.products.ProductThumbnail(imageUri = product.imageUri, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(product.name, fontWeight = FontWeight.Medium)
                if (product.trackStock) {
                    Text(
                        "Stock: ${formatQty(product.stockQuantity)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(CurrencyFormat.format(product.unitPrice), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CartLineRow(line: CartLine, viewModel: TillViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(line.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                CurrencyFormat.format(line.unitPrice) + " each",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { viewModel.decrementLine(line.key) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
        }
        Text(formatQty(line.quantity), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { viewModel.incrementLine(line.key) }) {
            Icon(Icons.Filled.Add, contentDescription = "Increase")
        }
        Text(
            CurrencyFormat.format(line.quantity * line.unitPrice),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(70.dp)
        )
    }
}

@Composable
private fun PaymentChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CustomItemDialog(onDismiss: () -> Unit, onAdd: (String, Double, Double) -> Unit) {
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom item") },
        text = {
            Column {
                QQTextField(value = description, onValueChange = { description = it }, label = "Description")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QQTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = "Qty",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    QQTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = "Price",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val qty = quantity.toDoubleOrNull() ?: 1.0
                val unitPrice = price.toDoubleOrNull() ?: 0.0
                if (description.isNotBlank() && qty > 0) {
                    onAdd(description.trim(), qty, unitPrice)
                }
            }) { Text("Add") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatQty(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
