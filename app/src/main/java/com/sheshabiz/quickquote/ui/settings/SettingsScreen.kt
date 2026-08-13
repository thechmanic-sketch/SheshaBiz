package com.sheshabiz.quickquote.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.BuildConfig
import com.sheshabiz.quickquote.R
import com.sheshabiz.quickquote.ui.common.QQOutlinedButton
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenTitleHeader
import com.sheshabiz.quickquote.ui.common.SectionLabel
import com.sheshabiz.quickquote.ui.theme.AppThemeMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onEditBusinessProfile: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportData()
                if (json != null) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    }
                    Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImportUri = uri }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleHeader(title = stringResource(R.string.settings_title))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
        Spacer(Modifier.height(20.dp))

        SettingsCard {
            SettingsRow(
                title = stringResource(R.string.settings_business_profile),
                subtitle = state.businessProfile?.businessName ?: "Not set up",
                onClick = onEditBusinessProfile
            )
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.settings_vat))
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_vat_enabled_default))
                Switch(checked = state.vatEnabledDefault, onCheckedChange = viewModel::onVatDefaultChange)
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                QQTextField(
                    value = state.vatRate,
                    onValueChange = viewModel::onVatRateChange,
                    label = stringResource(R.string.settings_default_vat_rate),
                    keyboardType = KeyboardType.Decimal
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.settings_numbering))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                QQTextField(
                    value = state.quotePrefix,
                    onValueChange = viewModel::onPrefixChange,
                    label = stringResource(R.string.settings_prefix)
                )
                Spacer(Modifier.height(14.dp))
                QQTextField(
                    value = state.invoicePrefix,
                    onValueChange = viewModel::onInvoicePrefixChange,
                    label = stringResource(R.string.settings_invoice_prefix)
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.settings_payment_terms))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                QQTextField(
                    value = state.paymentTerms,
                    onValueChange = viewModel::onPaymentTermsChange,
                    label = stringResource(R.string.settings_payment_terms),
                    singleLine = false,
                    minLines = 2
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.settings_currency))
        SettingsCard {
            SettingsRow(title = "ZAR (R)", subtitle = "South African Rand", onClick = null)
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.settings_appearance))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                ThemeOption(stringResource(R.string.theme_system), state.themeMode == AppThemeMode.SYSTEM) {
                    viewModel.onThemeModeChange(AppThemeMode.SYSTEM)
                }
                ThemeOption(stringResource(R.string.theme_light), state.themeMode == AppThemeMode.LIGHT) {
                    viewModel.onThemeModeChange(AppThemeMode.LIGHT)
                }
                ThemeOption(stringResource(R.string.theme_dark), state.themeMode == AppThemeMode.DARK) {
                    viewModel.onThemeModeChange(AppThemeMode.DARK)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.settings_data))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QQOutlinedButton(
                text = stringResource(R.string.settings_export),
                onClick = {
                    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    exportLauncher.launch("quickquote-backup-$stamp.json")
                },
                modifier = Modifier.weight(1f)
            )
            QQOutlinedButton(
                text = stringResource(R.string.settings_import),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.settings_about))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        }
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Import data") },
            text = { Text("This replaces all current customers and quotes on this device with the contents of the backup file. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri
                    pendingImportUri = null
                    if (uri != null) {
                        scope.launch {
                            val text = runCatching {
                                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            }.getOrNull()
                            val ok = text != null && viewModel.importData(text)
                            Toast.makeText(
                                context,
                                if (ok) "Data imported" else "Import failed — check the file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) { Text("Import", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        androidx.compose.material3.RadioButton(selected = selected, onClick = onClick)
    }
}
