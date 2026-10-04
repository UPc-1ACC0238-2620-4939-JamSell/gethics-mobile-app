package com.jamsell.gethics.sanitary.presentation.register_event

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun RegisterEventScreen(viewModel: RegisterEventViewModel, onSaved: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Registrar evento sanitario",
        userStory = "US11",
        description = "Registrar vacuna, desparasitacion, chequeo o signos vitales para un animal."
    ) {
        GethicsButton(text = "Guardar (simulado)", onClick = { onSaved() }, outlined = true)
    }
}
