package com.sheshabiz.quickquote.ui.invoice.create

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.R
import com.sheshabiz.quickquote.data.db.entity.Customer
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.data.db.entity.Product
import com.sheshabiz.quickquote.data.repository.CustomerRepository
import com.sheshabiz.quickquote.data.repository.ProductRepository
import com.sheshabiz.quickquote.domain.CurrencyFormat
import com.sheshabiz.quickquote.ui.common.QQDateField
import com.sheshabiz.quickquote.ui.common.QQOutlinedButton
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextActionButton
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenHeader
import com.sheshabiz.quickquote.ui.common.SectionLabel
import com.sheshabiz.quickquote.ui.common.TrialLockedDialog
import com.sheshabiz.quickquote.ui.customers.CustomerPickerSheet
import com.sheshabiz.quickquote.ui.products.ProductPickerSheet

@Composable
fun CreateInvoiceScreen(
    viewModel: CreateInvoiceViewModel,
    customerRepository: CustomerRepository,
    productRepository: ProductRepository,
    isEditing: Boolean,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onNeedsSubscription: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val customers by customerRepository.observeAll().collectAsState(initial = emptyList<Customer>())
    val products by productRepository.observeAll().collectAsState(initial = emptyList<Product>())
    var showPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedInvoiceId) {
        state.savedInvoiceId?.let { onSaved(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = if (isEditing) stringResource(R.string.edit_invoice_title) else stringResource(R.string.create_invoice_title),
            onBack = onBack
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SectionLabel(stringResource(R.string.customer_details))

            if (state.customerId != null) {
                LinkedCustomerCard(
                    name = state.customerName,
                    phone = state.customerPhone,
                    onChange = { viewModel.onClearCustomer() }
                )
                Spacer(Modifier.height(14.dp))
            } else {
                QQOutlinedButton(
                    text = stringResource(R.string.select_existing_customer),
                    onClick = { showPicker = true }
                )
                Spacer(Modifier.height(14.dp))
                QQTextField(
                    value = state.customerName,
                    onValueChange = viewModel::onCustomerNameChange,
                    label = stringResource(R.string.customer_name),
                    isError = state.customerNameError != null,
                    errorText = state.customerNameError
                )
                Spacer(Modifier.height(14.dp))
                QQTextField(
                    value = state.customerPhone,
                    onValueChange = viewModel::onCustomerPhoneChange,
                    label = stringResource(R.string.customer_phone),
                    keyboardType = KeyboardType.Phone,
                    isError = state.customerPhoneError != null,
                    errorText = state.customerPhoneError
                )
                Spacer(Modifier.height(14.dp))
                QQTextField(
                    value = state.customerEmail,
                    onValueChange = viewModel::onCustomerEmailChange,
                    label = stringResource(R.string.customer_email),
                    keyboardType = KeyboardType.Email,
                    isError = state.customerEmailError != null,
                    errorText = state.customerEmailError
                )
                Spacer(Modifier.height(14.dp))
                QQTextField(
                    value = state.customerAddress,
                    onValueChange = viewModel::onCustomerAddressChange,
                    label = stringResource(R.string.customer_address),
                    singleLine = false,
                    minLines = 2
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QQDateField(
                    label = stringResource(R.string.invoice_date),
                    millis = state.invoiceDate,
                    onChange = viewModel::onInvoiceDateChange,
                    modifier = Modifier.weight(1f)
                )
                QQDateField(
                    label = stringResource(R.string.due_date),
                    millis = state.dueDate,
                    onChange = viewModel::onDueDateChange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.items))
            state.items.forEachIndexed { index, item ->
                ItemRow(
                    item = item,
                    showRemove = state.items.size > 1,
                    onDescriptionChange = { viewModel.updateItemDescription(item.key, it) },
                    onQuantityChange = { viewModel.updateItemQuantity(item.key, it) },
                    onUnitPriceChange = { viewModel.updateItemUnitPrice(item.key, it) },
                    onRemove = { viewModel.removeItem(item.key) }
                )
                if (index != state.items.lastIndex) Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                QQTextActionButton(text = stringResource(R.string.add_item), onClick = viewModel::addItem)
                QQTextActionButton(text = "From catalog", onClick = { showProductPicker = true })
            }
            if (state.itemsError != null) {
                Text(
                    text = state.itemsError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.vat_on_off), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = state.vatEnabled,
                    onCheckedChange = viewModel::onVatEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.discount))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DiscountTypeChip(
                    label = stringResource(R.string.discount_percent),
                    selected = state.discountType == DiscountType.PERCENT,
                    onClick = { viewModel.onDiscountTypeChange(DiscountType.PERCENT) },
                    modifier = Modifier.weight(1f)
                )
                DiscountTypeChip(
                    label = stringResource(R.string.discount_fixed),
                    selected = state.discountType == DiscountType.FIXED,
                    onClick = { viewModel.onDiscountTypeChange(DiscountType.FIXED) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            QQTextField(
                value = state.discountValueText,
                onValueChange = viewModel::onDiscountValueChange,
                label = if (state.discountType == DiscountType.PERCENT) "Discount %" else "Discount amount (R)",
                keyboardType = KeyboardType.Decimal
            )

            Spacer(Modifier.height(24.dp))
            TotalsSummary(
                subtotal = state.totals.subtotal,
                discount = state.totals.discountAmount,
                vat = state.totals.vatAmount,
                vatEnabled = state.vatEnabled,
                total = state.totals.total
            )

            Spacer(Modifier.height(24.dp))
            QQTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = stringResource(R.string.notes_optional),
                singleLine = false,
                minLines = 2
            )
            Spacer(Modifier.height(14.dp))
            QQTextField(
                value = state.paymentTerms,
                onValueChange = viewModel::onPaymentTermsChange,
                label = stringResource(R.string.payment_terms),
                singleLine = false,
                minLines = 2
            )

            Spacer(Modifier.height(28.dp))
            QQPrimaryButton(
                text = stringResource(R.string.save_invoice),
                onClick = viewModel::save,
                loading = state.isSaving
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        CustomerPickerSheet(
            customers = customers,
            onDismiss = { showPicker = false },
            onSelect = { viewModel.onCustomerSelected(it); showPicker = false },
            onAddNew = { viewModel.onClearCustomer(); showPicker = false }
        )
    }

    if (showProductPicker) {
        ProductPickerSheet(
            products = products,
            onDismiss = { showProductPicker = false },
            onSelect = { viewModel.addItemFromProduct(it); showProductPicker = false }
        )
    }

    if (state.lockedMessage != null) {
        TrialLockedDialog(onDismiss = viewModel::clearLockedMessage, onSubscribe = onNeedsSubscription, message = state.lockedMessage!!)
    }
}

@Composable
private fun LinkedCustomerCard(name: String, phone: String, onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        QQTextActionButton(text = "Change", onClick = onChange)
    }
}

@Composable
private fun ItemRow(
    item: InvoiceItemDraft,
    showRemove: Boolean,
    onDescriptionChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            QQTextField(
                value = item.description,
                onValueChange = onDescriptionChange,
                label = stringResource(R.string.description),
                isError = item.descriptionError != null,
                errorText = item.descriptionError,
                modifier = Modifier.weight(1f)
            )
            if (showRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.remove_item),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QQTextField(
                value = item.quantity,
                onValueChange = onQuantityChange,
                label = stringResource(R.string.quantity),
                keyboardType = KeyboardType.Decimal,
                isError = item.quantityError != null,
                errorText = item.quantityError,
                modifier = Modifier.weight(1f)
            )
            QQTextField(
                value = item.unitPrice,
                onValueChange = onUnitPriceChange,
                label = stringResource(R.string.unit_price),
                keyboardType = KeyboardType.Decimal,
                isError = item.priceError != null,
                errorText = item.priceError,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DiscountTypeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = label, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TotalsSummary(subtotal: Double, discount: Double, vat: Double, vatEnabled: Boolean, total: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        TotalRow(stringResource(R.string.subtotal), CurrencyFormat.format(subtotal))
        if (discount > 0) {
            Spacer(Modifier.height(8.dp))
            TotalRow(stringResource(R.string.discount), "-${CurrencyFormat.format(discount)}")
        }
        if (vatEnabled) {
            Spacer(Modifier.height(8.dp))
            TotalRow(stringResource(R.string.vat), CurrencyFormat.format(vat))
        }
        Spacer(Modifier.height(10.dp))
        Divider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                CurrencyFormat.format(total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
