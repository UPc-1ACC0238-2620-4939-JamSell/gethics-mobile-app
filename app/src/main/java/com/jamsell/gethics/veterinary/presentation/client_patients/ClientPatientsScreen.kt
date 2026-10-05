package com.jamsell.gethics.veterinary.presentation.client_patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.shared.ui.components.GethicsButton

@Composable
fun ClientPatientsScreen(
    viewModel: ClientPatientsViewModel,
    clientId: String,
    onAnimalClick: (String) -> Unit,
    onRegisterCare: (String) -> Unit
) {
    val state = viewModel.state.value
    LaunchedEffect(clientId) { viewModel.load(clientId) }

    val data = state.data
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Pacientes", style = MaterialTheme.typography.titleLarge)

        when {
            state.isLoading && data == null -> CircularProgressIndicator()
            state.message.isNotEmpty() -> Text(state.message, color = MaterialTheme.colorScheme.error)
            data != null && data.patients.isEmpty() ->
                Text(data.message ?: "Este cliente no tiene animales registrados")
            data != null -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.patients, key = { it.patientId }) { patient ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(patient.name ?: "Sin nombre", style = MaterialTheme.typography.titleMedium)
                            Text("${patient.tag.orEmpty()} · ${patient.breed.orEmpty()}")
                            Text(
                                text = if (patient.totalClinicalEvents == 0) "Sin registros clínicos"
                                else "${patient.totalClinicalEvents} eventos · último: ${patient.lastEventType.orEmpty()} ${patient.lastEventDate.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            GethicsButton("Ver ficha", { onAnimalClick(patient.patientId) }, outlined = true)
                            GethicsButton("Registrar atención", { onRegisterCare(patient.patientId) })
                        }
                    }
                }
            }
        }
    }
}