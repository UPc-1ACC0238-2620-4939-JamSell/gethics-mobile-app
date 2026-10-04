package com.jamsell.gethics.livestock.presentation.animal_register

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.shared.common.UIState

class AnimalRegisterViewModel(private val repository: LivestockRepository) : ViewModel() {

    // TODO: cambiar Unit por el tipo real (ej. List<Animal>) y agregar las funciones de la pantalla
    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state
}
