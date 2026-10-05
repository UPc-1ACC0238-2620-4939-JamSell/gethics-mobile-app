package com.jamsell.gethics.sanitary.presentation.register_event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsTextField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterEventScreen(viewModel: RegisterEventViewModel, animalId: String, onSaved: () -> Unit) {
    val state = viewModel.state.value
    var type by rememberSaveable { mutableStateOf(SanitaryEventType.VACCINATION) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var description by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.data) {
        if (state.data == true) onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Registrar evento sanitario",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(text = "Tipo de evento", style = MaterialTheme.typography.titleSmall)
        Column(Modifier.selectableGroup()) {
            SanitaryEventType.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = type == option, onClick = { type = option }, role = Role.RadioButton)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = type == option, onClick = null)
                    Text(text = option.label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Text(text = "Fecha del evento", style = MaterialTheme.typography.titleSmall)
        GethicsButton(text = date.toString(), onClick = { showDatePicker = true }, outlined = true)

        GethicsTextField(
            value = description,
            onValueChange = { description = it },
            label = "Observaciones (opcional)",
            modifier = Modifier.fillMaxWidth()
        )

        if (state.message.isNotEmpty()) {
            Text(text = state.message, color = MaterialTheme.colorScheme.error)
        }

        GethicsButton(
            text = "Guardar",
            onClick = { viewModel.save(animalId, type, date, description) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isLoading
        )
    }

    if (showDatePicker) {
        // El DatePicker trabaja en milisegundos UTC: convertir con UTC para no correr la fecha un dia.
        // Se permiten fechas futuras a proposito: la validacion ocurre al guardar (escenario 2).
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
