package com.sheshabiz.quickquote.ui.pos

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.Sale
import com.sheshabiz.quickquote.data.db.entity.SaleItem
import com.sheshabiz.quickquote.domain.CurrencyFormat
import com.sheshabiz.quickquote.domain.DownloadsSaver
import com.sheshabiz.quickquote.domain.PdfUriHelper
import com.sheshabiz.quickquote.domain.PrintHelper
import com.sheshabiz.quickquote.domain.WhatsAppShare
import com.sheshabiz.quickquote.ui.common.QQOutlinedButton
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.ScreenHeader
import com.sheshabiz.quickquote.ui.common.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun ReceiptPreviewScreen(
    viewModel: ReceiptPreviewViewModel,
    onBack: () -> Unit,
    onNewSale: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingDownload by remember { mutableStateOf(false) }

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
            val sale = state.data?.sale ?: return@launch
            val businessName = state.businessProfile?.businessName.orEmpty()
            val uri = PdfUriHelper.uriFor(context, file)
            WhatsAppShare.shareGeneric(context, uri, WhatsAppShare.buildMessage(businessName, sale))
        }
    }

    fun sendViaWhatsApp() {
        scope.launch {
            val file = viewModel.generatePdfFile() ?: return@launch
            val sale = state.data?.sale ?: return@launch
            val businessName = state.businessProfile?.businessName.orEmpty()
            val uri = PdfUriHelper.uriFor(context, file)
            val opened = WhatsAppShare.sendViaWhatsApp(context, uri, WhatsAppShare.buildMessage(businessName, sale))
            if (!opened) {
                Toast.makeText(context, "WhatsApp isn't installed — opening share options.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun printPdf() {
        scope.launch {
            val file = viewModel.generatePdfFile() ?: return@launch
            val sale = state.data?.sale ?: return@launch
            PrintHelper.printPdf(context, file, sale.saleNumber)
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
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Receipt", onBack = onBack)
            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = when {
                        data == null -> "This sale couldn't be found (id: ${state.saleId}). Check Sales history (under More) to see if it was actually saved."
                        else -> "Your business profile isn't set up yet, so a receipt can't be generated. Go to Settings > Business profile to set it up, then try again."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }
    val sale = data.sale

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = sale.saleNumber, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            ReceiptDocumentPreview(profile = profile, sale = sale, items = data.items)
            Spacer(Modifier.height(28.dp))

            QQPrimaryButton(text = "Send via WhatsApp", onClick = ::sendViaWhatsApp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QQOutlinedButton(text = "Share PDF", onClick = ::sharePdf, modifier = Modifier.weight(1f))
                QQOutlinedButton(text = "Download PDF", onClick = ::downloadPdf, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            QQOutlinedButton(text = "Print", onClick = ::printPdf)
            Spacer(Modifier.height(10.dp))
            QQPrimaryButton(text = "New Sale", onClick = onNewSale)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReceiptDocumentPreview(profile: BusinessProfile, sale: Sale, items: List<SaleItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bitmap = remember(profile.logoUri) {
                profile.logoUri?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(profile.businessName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (profile.phone.isNotBlank()) Text(profile.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (profile.address.isNotBlank()) Text(profile.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        Text("RECEIPT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            MetaField("RECEIPT NUMBER", sale.saleNumber)
            MetaField("DATE", CurrencyFormat.formatDate(sale.saleDate))
            MetaField("PAYMENT", sale.paymentMethod.name)
        }
        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        Text(sale.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(20.dp))
        SectionLabel("ITEMS")
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
        TotalLine("Subtotal", CurrencyFormat.format(sale.subtotal))
        if (sale.vatEnabled) {
            TotalLine("VAT (${formatQty(sale.vatRate)}%)", CurrencyFormat.format(sale.vatAmount))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TOTAL", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            Text(CurrencyFormat.format(sale.total), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Thank you for your business.",
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

private fun formatQty(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
