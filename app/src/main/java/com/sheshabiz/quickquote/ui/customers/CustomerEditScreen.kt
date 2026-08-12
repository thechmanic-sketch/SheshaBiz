package com.sheshabiz.quickquote.ui.customers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sheshabiz.quickquote.R
import com.sheshabiz.quickquote.ui.common.QQPrimaryButton
import com.sheshabiz.quickquote.ui.common.QQTextField
import com.sheshabiz.quickquote.ui.common.ScreenHeader

@Composable
fun CustomerEditScreen(
    viewModel: CustomerEditViewModel,
    isEditing: Boolean,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.savedCustomerId) {
        state.savedCustomerId?.let { onSaved(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = if (isEditing) stringResource(R.string.edit_customer) else stringResource(R.string.add_customer),
            onBack = onBack
        )
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))

            QQTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.customer_name),
                isError = state.nameError != null,
                errorText = state.nameError
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
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.customer_email),
                keyboardType = KeyboardType.Email,
                isError = state.emailError != null,
                errorText = state.emailError
            )
            Spacer(Modifier.height(14.dp))
            QQTextField(
                value = state.address,
                onValueChange = viewModel::onAddressChange,
                label = stringResource(R.string.customer_address),
                singleLine = false,
                minLines = 2
            )
            Spacer(Modifier.height(28.dp))

            QQPrimaryButton(
                text = stringResource(R.string.save_customer),
                onClick = viewModel::save,
                loading = state.isSaving
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
