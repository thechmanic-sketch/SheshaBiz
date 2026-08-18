package com.sheshabiz.quickquote.ui.quote.preview

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.R
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.data.db.entity.Quote
import com.sheshabiz.quickquote.data.db.entity.QuoteItem
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.data.prefs.AppPreferences
import com.sheshabiz.quickquote.domain.CurrencyFormat
import com.sheshabiz.quickquote.domain.DownloadsSaver
import com.sheshabiz.quickquote.domain.WhatsAppShare
import com.sheshabiz.quickquote.ui.common.QQOutlinedButton
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextActionButton
import com.sheshabiz.quickquote.ui.common.ScreenHeader
import com.sheshabiz.quickquote.ui.common.SectionLabel
import com.sheshabiz.quickquote.ui.common.StatusChip
import com.sheshabiz.quickquote.ui.common.TrialLockedDialog
import com.sheshabiz.quickquote.ui.lock.PinConfirmDialog
import kotlinx.coroutines.launch

@Composable
fun QuotePreviewScreen(
    viewModel: QuotePreviewViewModel,
    preferences: AppPreferences,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDuplicated: (Long) -> Unit,
    onConvertedToInvoice: (Long) -> Unit,
    onDeleted: () -> Unit,
    onNeedsPinSetup: () -> Unit,
    onNeedsLogin: () -> Unit,
    onNeedsSubscription: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    val duplicatedId by viewModel.duplicatedId.collectAsState()
    val convertedInvoiceId by viewModel.convertedInvoiceId.collectAsState()
    val lockedReason by viewModel.lockedReason.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPinConfirm by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onDeleted() }
    LaunchedEffect(duplicatedId) { duplicatedId?.let(onDuplicated) }
    LaunchedEffect(convertedInvoiceId) { convertedInvoiceId?.let(onConvertedToInvoice) }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingDownload) {
            pendingDownload = false
            scope.launch {
                val file = viewModel.generatePdfFile()
                if (file != null && DownloadsSaver.saveToDownloads(context, file, file.name)) {
                    Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Couldn't save the PDF", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (!granted) {
            Toast.makeText(context, "Storage permission is needed to download.", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadPdf() {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        if (needsPermission) {
            pendingDownload = true
            requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            scope.launch {
                val file = viewModel.generatePdfFile()
                if (file != null && DownloadsSaver.saveToDownloads(context, file, file.name)) {
                    Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Couldn't save the PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun sharePdf() {
        scope.launch {
            val file = viewModel.generatePdfFile() ?: return@launch
            val quote = state.data?.quote ?: return@launch
            val businessName = state.businessProfile?.businessName.orEmpty()
            val uri = com.sheshabiz.quickquote.domain.PdfUriHelper.uriFor(context, file)
            WhatsAppShare.shareGeneric(context, uri, WhatsAppShare.buildMessage(businessName, quote))
        }
    }

    fun sendViaWhatsApp() {
        scope.launch {
            val file = viewModel.generatePdfFile() ?: return@launch
            val quote = state.data?.quote ?: return@launch
            val businessName = state.businessProfile?.businessName.orEmpty()
            val uri = com.sheshabiz.quickquote.domain.PdfUriHelper.uriFor(context, file)
            val opened = WhatsAppShare.sendViaWhatsApp(context, uri, WhatsAppShare.buildMessage(businessName, quote))
            if (!opened) {
                Toast.makeText(context, "WhatsApp isn't installed — opening share options.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun printPdf() {
        scope.launch {
            val file = viewModel.generatePdfFile() ?: return@launch
            val quote = state.data?.quote ?: return@launch
            com.sheshabiz.quickquote.domain.PrintHelper.printPdf(context, file, quote.quoteNumber)
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val data = state.data
    val profile = state.businessProfile
    if (data == null || profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Quote not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val quote = data.quote

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = quote.quoteNumber, onBack = onBack) {
            StatusChip(status = quote.status)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            QuoteDocumentPreview(profile = profile, quote = quote, items = data.items)
            Spacer(Modifier.height(28.dp))

            QQPrimaryButton(text = stringResource(R.string.send_via_whatsapp), onClick = ::sendViaWhatsApp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QQOutlinedButton(text = stringResource(R.string.share_pdf), onClick = ::sharePdf, modifier = Modifier.weight(1f))
                QQOutlinedButton(text = stringResource(R.string.download_pdf), onClick = ::downloadPdf, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            QQOutlinedButton(text = "Print", onClick = ::printPdf)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QQOutlinedButton(text = stringResource(R.string.edit), onClick = { onEdit(quote.id) }, modifier = Modifier.weight(1f))
                QQOutlinedButton(text = stringResource(R.string.duplicate), onClick = viewModel::duplicateQuote, modifier = Modifier.weight(1f))
            }
            if (quote.status != QuoteStatus.REJECTED) {
                Spacer(Modifier.height(10.dp))
                QQOutlinedButton(
                    text = stringResource(R.string.convert_to_invoice),
                    onClick = viewModel::convertToInvoice
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Status")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (quote.status != QuoteStatus.SENT) {
                    StatusActionChip(stringResource(R.string.mark_as_sent)) { viewModel.updateStatus(QuoteStatus.SENT) }
                }
                if (quote.status != QuoteStatus.ACCEPTED) {
                    StatusActionChip(stringResource(R.string.mark_as_accepted)) { viewModel.updateStatus(QuoteStatus.ACCEPTED) }
                }
                if (quote.status != QuoteStatus.REJECTED) {
                    StatusActionChip(stringResource(R.string.mark_as_rejected)) { viewModel.updateStatus(QuoteStatus.REJECTED) }
                }
            }

            Spacer(Modifier.height(20.dp))
            QQTextActionButton(
                text = stringResource(R.string.delete),
                onClick = { showDeleteConfirm = true }
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_quote_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    showPinConfirm = true
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showPinConfirm) {
        PinConfirmDialog(
            preferences = preferences,
            onConfirmed = {
                showPinConfirm = false
                viewModel.deleteQuote()
            },
            onDismiss = { showPinConfirm = false },
            onNeedsSetup = onNeedsPinSetup
        )
    }

    lockedReason?.let { reason ->
        TrialLockedDialog(
            reason = reason,
            onDismiss = viewModel::clearLockedReason,
            onNavigateToLogin = onNeedsLogin,
            onNavigateToSubscription = onNeedsSubscription
        )
    }
}

@Composable
private fun StatusActionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuoteDocumentPreview(profile: BusinessProfile, quote: Quote, items: List<QuoteItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        // Business header
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bitmap = remember(profile.logoUri) {
                profile.logoUri?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(profile.businessName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (profile.phone.isNotBlank()) Text(profile.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (profile.email.isNotBlank()) Text(profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (profile.address.isNotBlank()) Text(profile.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                profile.vatNumber?.takeIf { it.isNotBlank() }?.let {
                    Text("VAT: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                profile.registrationNumber?.takeIf { it.isNotBlank() }?.let {
                    Text("Reg No: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.quotation), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            MetaField("QUOTE NUMBER", quote.quoteNumber)
            MetaField("DATE", CurrencyFormat.formatDate(quote.quoteDate))
            MetaField("VALID UNTIL", CurrencyFormat.formatDate(quote.validUntil))
        }
        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        SectionLabel(stringResource(R.string.customer_label))
        Text(quote.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (quote.customerPhone.isNotBlank()) Text(quote.customerPhone, style = MaterialTheme.typography.bodySmall)
        quote.customerEmail?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        quote.customerAddress?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.items_label))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Description", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Qty", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Price", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Total", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline)
        items.sortedBy { it.sortOrder }.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(item.description, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium)
                Text(formatQty(item.quantity), modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodyMedium)
                Text(CurrencyFormat.format(item.unitPrice), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium)
                Text(CurrencyFormat.format(item.lineTotal), modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodyMedium)
            }
            Divider(color = MaterialTheme.colorScheme.outline)
        }

        Spacer(Modifier.height(16.dp))
        TotalLine(stringResource(R.string.subtotal), CurrencyFormat.format(quote.subtotal))
        if (quote.discountAmount > 0) {
            val label = if (quote.discountType == DiscountType.PERCENT) "Discount (${formatQty(quote.discountValue)}%)" else "Discount"
            TotalLine(label, "-${CurrencyFormat.format(quote.discountAmount)}")
        }
        if (quote.vatEnabled) {
            TotalLine("${stringResource(R.string.vat)} (${formatQty(quote.vatRate)}%)", CurrencyFormat.format(quote.vatAmount))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.total), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            Text(CurrencyFormat.format(quote.total), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }

        quote.paymentTerms?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(18.dp))
            SectionLabel(stringResource(R.string.payment_terms))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        quote.notes?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(14.dp))
            SectionLabel(stringResource(R.string.notes_optional))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        val bankingLines = listOfNotNull(
            profile.bankName?.takeIf { it.isNotBlank() }?.let { "Bank: $it" },
            profile.accountHolder?.takeIf { it.isNotBlank() }?.let { "Account holder: $it" },
            profile.accountNumber?.takeIf { it.isNotBlank() }?.let { "Account number: $it" },
            profile.branchCode?.takeIf { it.isNotBlank() }?.let { "Branch code: $it" },
            profile.accountType?.takeIf { it.isNotBlank() }?.let { "Account type: $it" }
        )
        if (bankingLines.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SectionLabel(stringResource(R.string.banking_details_optional))
            bankingLines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.thank_you_footer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun MetaField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TotalLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

private fun formatQty(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
