package com.jamsell.gethics.veterinary.presentation.client_patients

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun ClientPatientsScreen(viewModel: ClientPatientsViewModel, clientId: Long, onAnimalClick: (String) -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Pacientes del cliente",
        userStory = "US18",
        description = "Animales (pacientes) de un cliente asignado."
    ) {
        GethicsButton(text = "Abrir animal de prueba (id 1)", onClick = { onAnimalClick("1") }, outlined = true)
    }
}
