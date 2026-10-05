package com.jamsell.gethics.veterinary.presentation.register_care

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsTextField
import java.util.UUID

@Composable
fun RegisterCareScreen(viewModel: RegisterCareViewModel, patientId: String, onSaved: () -> Unit) {
    val state = viewModel.state.value
    val requestId = rememberSaveable { UUID.randomUUID().toString() }
    var diagnosis by rememberSaveable { mutableStateOf("") }
    var treatment by rememberSaveable { mutableStateOf("") }
    var nextControl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.data) {
        if (state.data != null) onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Registrar atención", style = MaterialTheme.typography.titleLarge)

        GethicsTextField(
            value = diagnosis,
            onValueChange = { diagnosis = it },
            label = "Diagnóstico",
            modifier = Modifier.fillMaxWidth()
        )
        GethicsTextField(
            value = treatment,
            onValueChange = { treatment = it },
            label = "Tratamiento aplicado",
            modifier = Modifier.fillMaxWidth()
        )
        GethicsTextField(
            value = nextControl,
            onValueChange = { nextControl = it },
            label = "Próximo control (opcional)",
            modifier = Modifier.fillMaxWidth(),
            helperText = "Formato AAAA-MM-DD"
        )

        if (state.message.isNotEmpty()) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }

        GethicsButton(
            text = "Guardar atención",
            onClick = { viewModel.register(patientId, requestId, diagnosis, treatment, nextControl) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isLoading
        )
    }
}