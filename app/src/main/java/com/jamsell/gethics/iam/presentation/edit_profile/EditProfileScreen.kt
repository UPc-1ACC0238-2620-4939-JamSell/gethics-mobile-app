package com.jamsell.gethics.iam.presentation.edit_profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsTextField

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onProfileUpdated: () -> Unit
) {
    val state = viewModel.state.value
    val isSaved = viewModel.isSaved.value

    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(state.data) {
        state.data?.let {
            fullName = it.fullName
            phone = it.phone.orEmpty()
            address = it.address.orEmpty()
        }
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onProfileUpdated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Editar Perfil",
            style = MaterialTheme.typography.titleLarge
        )

        if (state.isLoading && state.data == null) {
            CircularProgressIndicator()
        } else {
            GethicsTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Nombre completo",
                modifier = Modifier.fillMaxWidth()
            )

            GethicsTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Teléfono",
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Phone
            )

            GethicsTextField(
                value = address,
                onValueChange = { address = it },
                label = "Dirección / Ubicación",
                modifier = Modifier.fillMaxWidth()
            )

            if (state.message.isNotEmpty()) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            GethicsButton(
                text = "Guardar cambios",
                onClick = { viewModel.updateProfile(fullName, phone, address) },
                modifier = Modifier.fillMaxWidth(),
                isLoading = state.isLoading
            )
        }
    }
}