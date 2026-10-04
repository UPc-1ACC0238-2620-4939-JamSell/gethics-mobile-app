package com.jamsell.gethics.sanitary.presentation.sanitary_calendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun SanitaryCalendarScreen(viewModel: SanitaryCalendarViewModel, onRegisterEvent: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Calendario de salud",
        userStory = "US12",
        description = "Chequeos, vacunas y tratamientos programados."
    ) {
        GethicsButton(text = "Registrar evento", onClick = { onRegisterEvent() }, outlined = true)
    }
}
