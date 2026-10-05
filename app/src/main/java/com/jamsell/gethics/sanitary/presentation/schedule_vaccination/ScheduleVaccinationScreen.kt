package com.jamsell.gethics.sanitary.presentation.schedule_vaccination

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.sanitary.presentation.register_event.FormBottomBar
import com.jamsell.gethics.sanitary.presentation.register_event.FormDateField
import com.jamsell.gethics.sanitary.presentation.register_event.FormHeader
import com.jamsell.gethics.sanitary.presentation.register_event.FormHorizontalPadding
import com.jamsell.gethics.sanitary.presentation.register_event.FormLabel
import com.jamsell.gethics.sanitary.presentation.register_event.FormMessage
import com.jamsell.gethics.sanitary.presentation.register_event.FormPrimaryButton
import com.jamsell.gethics.sanitary.presentation.register_event.FormTextField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Mismo lenguaje visual que US11 (formulario 12:196). El tipo es siempre VACCINATION: no hay selector. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleVaccinationScreen(viewModel: ScheduleVaccinationViewModel, animalId: String, onScheduled: () -> Unit, onBack: () -> Unit) {
    val state = viewModel.state.value
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var description by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.data) {
        if (state.data == true) onScheduled()
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        FormHeader(title = "Programar vacuna", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FormHorizontalPadding)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            FormLabel("Fecha programada")
            Spacer(Modifier.height(8.dp))
            FormDateField(date = date, label = "Fecha programada", onClick = { showDatePicker = true })

            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                FormLabel("Observaciones (opcional)")
                Spacer(Modifier.height(8.dp))
                FormTextField(value = description, onValueChange = { description = it })
            }

            if (state.message.isNotEmpty()) FormMessage(state.message)
        }

        FormBottomBar {
            FormPrimaryButton(
                text = "Programar vacuna",
                onClick = { viewModel.schedule(animalId, date, description) },
                isLoading = state.isLoading
            )
        }
    }

    if (showDatePicker) {
        // El DatePicker trabaja en milisegundos UTC. Solo hoy o fechas futuras: el backend rechaza fechas pasadas.
        val today = LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)

                override fun isSelectableYear(year: Int) = year >= today.year
            }
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
