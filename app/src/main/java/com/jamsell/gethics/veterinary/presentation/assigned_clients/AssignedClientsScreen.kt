package com.jamsell.gethics.veterinary.presentation.assigned_clients

import androidx.compose.foundation.clickable
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

@Composable
fun AssignedClientsScreen(viewModel: AssignedClientsViewModel, onClientClick: (String) -> Unit) {
    val state = viewModel.state.value
    LaunchedEffect(Unit) { viewModel.load() }

    val data = state.data
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Mis clientes", style = MaterialTheme.typography.titleLarge)

        when {
            state.isLoading && data == null -> CircularProgressIndicator()
            state.message.isNotEmpty() -> Text(state.message, color = MaterialTheme.colorScheme.error)
            data != null && data.clients.isEmpty() ->
                Text(data.message ?: "No hay clientes vinculados")
            data != null -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.clients, key = { it.assignmentId }) { client ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onClientClick(client.clientId) }
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Cliente ${client.clientId.take(8)}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = client.location ?: "Ubicación no disponible",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}