package com.jamsell.gethics.livestock.presentation.animal_detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun AnimalDetailScreen(viewModel: AnimalDetailViewModel, animalId: Long, onOpenClinicalHistory: (Long) -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Detalle del animal",
        userStory = "Livestock",
        description = "Datos del animal + accesos a historial clinico y eventos sanitarios."
    ) {
        GethicsButton(text = "Ver historial clinico", onClick = { onOpenClinicalHistory(animalId) }, outlined = true)
    }
}
