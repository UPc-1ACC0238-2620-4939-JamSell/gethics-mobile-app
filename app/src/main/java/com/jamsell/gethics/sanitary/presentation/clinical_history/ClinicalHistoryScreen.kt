package com.jamsell.gethics.sanitary.presentation.clinical_history

import androidx.compose.runtime.Composable
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun ClinicalHistoryScreen(viewModel: ClinicalHistoryViewModel, animalId: Long) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Historial clinico",
        userStory = "US14",
        description = "Linea de tiempo de eventos sanitarios de un animal. Opcion de exportar."
    ) {
    }
}
