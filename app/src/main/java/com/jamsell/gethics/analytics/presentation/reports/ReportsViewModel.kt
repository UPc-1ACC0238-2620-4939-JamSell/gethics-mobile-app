package com.jamsell.gethics.analytics.presentation.reports

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jamsell.gethics.analytics.data.repository.AnalyticsRepository
import com.jamsell.gethics.shared.common.UIState

class ReportsViewModel(private val repository: AnalyticsRepository) : ViewModel() {

    // TODO: cambiar Unit por el tipo real (ej. List<Animal>) y agregar las funciones de la pantalla
    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state
}
