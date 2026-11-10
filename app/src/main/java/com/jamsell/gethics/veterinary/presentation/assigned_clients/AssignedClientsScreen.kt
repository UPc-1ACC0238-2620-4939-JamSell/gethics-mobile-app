package com.jamsell.gethics.veterinary.presentation.assigned_clients

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder
import java.util.UUID

@Composable
fun AssignedClientsScreen(
    viewModel: AssignedClientsViewModel,
    onClientClick: (Long) -> Unit
) {
    // Observamos el estado del ViewModel (por ejemplo, mensajes de éxito o error)
    val uiState by viewModel.uiState.collectAsState()

    ScreenPlaceholder(
        title = "Clientes asignados",
        userStory = "US17 / US34",
        description = "Lista de ganaderos asignados al veterinario y gestión de vínculos."
    ) {
        GethicsButton(
            text = "Abrir cliente de prueba (id 1)",
            onClick = { onClientClick(1L) },
            outlined = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Botón de ejemplo para probar la desvinculación (US-34)
        GethicsButton(
            text = "Desvincular cliente de prueba",
            onClick = {
                // Aquí pasarías el ID de la asignación real de tu lista
                val sampleAssignmentId = UUID.randomUUID()
                viewModel.unassignClient(sampleAssignmentId)
            },
            outlined = false
        )

        // Mostrar retroalimentación si el ViewModel actualiza el estado
        uiState?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Text(text = message)
        }
    }
}

