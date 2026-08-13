package com.sheshabiz.quickquote.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextActionButton
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenHeader

@Composable
fun ProductEditScreen(
    viewModel: ProductEditViewModel,
    isEditing: Boolean,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedProductId) {
        state.savedProductId?.let { onSaved(it) }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = if (isEditing) "Edit Product" else "Add Product",
            onBack = onBack
        )
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))

            QQTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Name",
                isError = state.nameError != null,
                errorText = state.nameError
            )
            Spacer(Modifier.height(14.dp))
            QQTextField(
                value = state.unitPriceText,
                onValueChange = viewModel::onUnitPriceChange,
                label = "Unit price",
                keyboardType = KeyboardType.Decimal,
                isError = state.priceError != null,
                errorText = state.priceError
            )
            Spacer(Modifier.height(14.dp))
            QQTextField(
                value = state.sku,
                onValueChange = viewModel::onSkuChange,
                label = "SKU / barcode (optional)"
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Track stock (POS product)", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = state.trackStock,
                    onCheckedChange = viewModel::onTrackStockChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            if (state.trackStock) {
                Spacer(Modifier.height(14.dp))
                QQTextField(
                    value = state.stockQuantityText,
                    onValueChange = viewModel::onStockQuantityChange,
                    label = "Stock quantity",
                    keyboardType = KeyboardType.Decimal,
                    isError = state.stockError != null,
                    errorText = state.stockError
                )
                Spacer(Modifier.height(14.dp))
                QQTextField(
                    value = state.lowStockThresholdText,
                    onValueChange = viewModel::onLowStockThresholdChange,
                    label = "Low stock warning at (optional)",
                    keyboardType = KeyboardType.Decimal
                )
            }

            Spacer(Modifier.height(28.dp))
            QQPrimaryButton(
                text = "Save",
                onClick = viewModel::save,
                loading = state.isSaving
            )

            if (isEditing) {
                Spacer(Modifier.height(16.dp))
                QQTextActionButton(text = "Delete product", onClick = { showDeleteConfirm = true })
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete") },
            text = { Text("Delete this product? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
