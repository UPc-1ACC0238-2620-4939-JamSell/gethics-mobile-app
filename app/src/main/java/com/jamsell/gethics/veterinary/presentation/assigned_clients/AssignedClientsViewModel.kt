package com.jamsell.gethics.veterinary.presentation.assigned_clients

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jamsell.gethics.veterinary.data.repository.VeterinaryRepository
import com.jamsell.gethics.shared.common.UIState

class AssignedClientsViewModel(private val repository: VeterinaryRepository) : ViewModel() {

    // TODO: cambiar Unit por el tipo real (ej. List<Animal>) y agregar las funciones de la pantalla
    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state
}
