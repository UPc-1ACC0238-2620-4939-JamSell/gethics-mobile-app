package com.jamsell.gethics.veterinary.presentation.assigned_clients

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun AssignedClientsScreen(viewModel: AssignedClientsViewModel, onClientClick: (Long) -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Clientes asignados",
        userStory = "US17",
        description = "Lista de ganaderos asignados al veterinario."
    ) {
        GethicsButton(text = "Abrir cliente de prueba (id 1)", onClick = { onClientClick(1L) }, outlined = true)
    }
}
