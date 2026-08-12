package com.sheshabiz.quickquote.ui.businesssetup

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.R
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextActionButton
import com.sheshabiz.quickquote.ui.common.QQTextField

@Composable
fun BusinessSetupScreen(
    viewModel: BusinessSetupViewModel,
    isEditing: Boolean,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onLogoPicked(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (isEditing) 0.dp else 20.dp, vertical = if (isEditing) 0.dp else 24.dp)
    ) {
        if (isEditing) {
            com.sheshabiz.quickquote.ui.common.ScreenHeader(
                title = stringResource(R.string.settings_business_profile),
                onBack = onBack
            )
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = if (isEditing) 16.dp else 0.dp)) {
        if (!isEditing) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }

        LogoPicker(
            logoPath = state.logoUri,
            onPick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemove = viewModel::onRemoveLogo
        )
        Spacer(Modifier.height(24.dp))

        QQTextField(
            value = state.businessName,
            onValueChange = viewModel::onBusinessNameChange,
            label = stringResource(R.string.business_name),
            isError = state.businessNameError != null,
            errorText = state.businessNameError
        )
        Spacer(Modifier.height(14.dp))
        QQTextField(
            value = state.ownerName,
            onValueChange = viewModel::onOwnerNameChange,
            label = stringResource(R.string.owner_name),
            isError = state.ownerNameError != null,
            errorText = state.ownerNameError
        )
        Spacer(Modifier.height(14.dp))
        QQTextField(
            value = state.phone,
            onValueChange = viewModel::onPhoneChange,
            label = stringResource(R.string.phone),
            keyboardType = KeyboardType.Phone,
            isError = state.phoneError != null,
            errorText = state.phoneError
        )
        Spacer(Modifier.height(14.dp))
        QQTextField(
            value = state.whatsappNumber,
            onValueChange = viewModel::onWhatsappChange,
            label = stringResource(R.string.whatsapp_number),
            keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(14.dp))
        QQTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.email),
            keyboardType = KeyboardType.Email,
            isError = state.emailError != null,
            errorText = state.emailError
        )
        Spacer(Modifier.height(14.dp))
        QQTextField(
            value = state.address,
            onValueChange = viewModel::onAddressChange,
            label = stringResource(R.string.business_address),
            singleLine = false,
            minLines = 2
        )
        Spacer(Modifier.height(14.dp))
        QQTextField(
            value = state.vatNumber,
            onValueChange = viewModel::onVatNumberChange,
            label = stringResource(R.string.vat_number_optional)
        )
        Spacer(Modifier.height(28.dp))

        QQPrimaryButton(
            text = if (isEditing) "Save" else stringResource(R.string.continue_label),
            onClick = viewModel::save,
            loading = state.isSaving
        )
        Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LogoPicker(logoPath: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    val bitmap = remember(logoPath) {
        logoPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            QQTextActionButton(
                text = if (logoPath == null) stringResource(R.string.add_logo) else stringResource(R.string.change_logo),
                onClick = onPick
            )
            if (logoPath != null) {
                QQTextActionButton(text = stringResource(R.string.remove_logo), onClick = onRemove)
            }
        }
    }
}
