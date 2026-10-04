package com.jamsell.gethics.livestock.presentation.animal_register

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun AnimalRegisterScreen(viewModel: AnimalRegisterViewModel, onSaved: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Registrar animal",
        userStory = "Livestock",
        description = "Formulario para agregar un animal con informacion basica (nombre, arete, raza, foto)."
    ) {
        GethicsButton(text = "Guardar (simulado)", onClick = { onSaved() }, outlined = true)
    }
}
