package com.jamsell.gethics.livestock.presentation.animal_list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun AnimalListScreen(viewModel: AnimalListViewModel, onAnimalClick: (Long) -> Unit, onAddAnimal: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Inventario de ganado",
        userStory = "Livestock",
        description = "Lista de animales del hato con busqueda. Usar AnimalCard (ya creado) y estados LoadingBox/EmptyState/ErrorMessage. Offline-first con Room."
    ) {
        GethicsButton(text = "Registrar animal", onClick = { onAddAnimal() }, outlined = true)
        GethicsButton(text = "Abrir animal de prueba (id 1)", onClick = { onAnimalClick(1L) }, outlined = true)
    }
}
