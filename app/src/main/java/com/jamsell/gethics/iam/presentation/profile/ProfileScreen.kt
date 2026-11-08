package com.jamsell.gethics.iam.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsTextField

/** US04: ver y editar el perfil (GET/PUT users/me). Solo se edita nombre y telefono; correo y rol son de lectura. */
@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onViewPlans: () -> Unit, onSignOut: () -> Unit) {
    val state = viewModel.state.value
    val saved = viewModel.saved.value
    val user = state.data

    // La clave es el id: los campos se rellenan cuando llega el perfil y no se pisan al guardar.
    var name by rememberSaveable(user?.id) { mutableStateOf(user?.name.orEmpty()) }
    var phone by rememberSaveable(user?.id) { mutableStateOf(user?.phone.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mi perfil",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (user == null) {
            if (state.isLoading) {
                Text(text = "Cargando tu perfil...", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                GethicsButton(
                    text = "Reintentar",
                    onClick = { viewModel.load() },
                    modifier = Modifier.fillMaxWidth(),
                    outlined = true
                )
            }
        } else {
            Text(
                text = "${user.email} · ${user.role.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            GethicsTextField(
                value = name,
                onValueChange = {
                    name = it
                    viewModel.clearSaved()
                },
                label = "Nombre",
                modifier = Modifier.fillMaxWidth()
            )
            GethicsTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    viewModel.clearSaved()
                },
                label = "Teléfono",
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Phone
            )

            if (state.message.isNotEmpty()) {
                Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            if (saved) {
                Text(text = "Perfil actualizado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            GethicsButton(
                text = "Guardar cambios",
                onClick = { viewModel.save(name, phone) },
                modifier = Modifier.fillMaxWidth(),
                isLoading = state.isLoading
            )
        }

        GethicsButton(text = "Ver planes", onClick = { onViewPlans() }, modifier = Modifier.fillMaxWidth(), outlined = true)
        GethicsButton(text = "Cerrar sesión", onClick = { onSignOut() }, modifier = Modifier.fillMaxWidth(), outlined = true)
    }
}
