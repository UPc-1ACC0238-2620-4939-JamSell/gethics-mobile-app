package com.jamsell.gethics.sanitary.presentation.sanitary_calendar

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import com.jamsell.gethics.shared.common.UIState

class SanitaryCalendarViewModel(private val repository: SanitaryRepository) : ViewModel() {

    // TODO: cambiar Unit por el tipo real (ej. List<Animal>) y agregar las funciones de la pantalla
    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state
}
