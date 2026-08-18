package com.sheshabiz.quickquote.ui.products

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextActionButton
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenHeader
import com.sheshabiz.quickquote.ui.common.TrialLockedDialog
import com.sheshabiz.quickquote.ui.lock.PinConfirmDialog

@Composable
fun ProductEditScreen(
    viewModel: ProductEditViewModel,
    preferences: AppPreferences,
    isEditing: Boolean,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onDeleted: () -> Unit,
    onNeedsPinSetup: () -> Unit,
    onNeedsLogin: () -> Unit,
    onNeedsSubscription: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPinConfirm by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onImagePicked(it) } }

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

            ProductImagePicker(
                imagePath = state.imageUri,
                onPick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = viewModel::onRemoveImage
            )
            Spacer(Modifier.height(20.dp))

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
                    showPinConfirm = true
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showPinConfirm) {
        PinConfirmDialog(
            preferences = preferences,
            onConfirmed = {
                showPinConfirm = false
                viewModel.delete()
            },
            onDismiss = { showPinConfirm = false },
            onNeedsSetup = onNeedsPinSetup
        )
    }

    state.lockedReason?.let { reason ->
        TrialLockedDialog(
            reason = reason,
            onDismiss = viewModel::clearLockedReason,
            onNavigateToLogin = onNeedsLogin,
            onNavigateToSubscription = onNeedsSubscription
        )
    }
}

@Composable
private fun ProductImagePicker(imagePath: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    val bitmap = remember(imagePath) {
        imagePath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            QQTextActionButton(
                text = if (imagePath == null) "Add photo" else "Change photo",
                onClick = onPick
            )
            if (imagePath != null) {
                QQTextActionButton(text = "Remove photo", onClick = onRemove)
            }
        }
    }
}
