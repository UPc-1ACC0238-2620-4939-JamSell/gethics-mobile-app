package com.jamsell.gethics.livestock.presentation.animal_detail

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AnimalDetailViewModel(
    private val repository: LivestockRepository,
    private val animalId: String
) : ViewModel() {

    // data = animal; data null -> message es el error. Mientras recarga conserva el animal anterior para no parpadear.
    private val _state = mutableStateOf(UIState<Animal>(isLoading = true))
    val state: State<UIState<Animal>> get() = _state

    /** true cuando el animal viene del cache de Room porque no hubo conexion. */
    private val _offline = mutableStateOf(false)
    val offline: State<Boolean> get() = _offline

    private var loadJob: Job? = null

    /** La pantalla lo llama al reanudarse: asi vuelve a cargar al regresar de editar. */
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UIState(isLoading = true, data = _state.value.data)
            when (val result = repository.getAnimal(animalId)) {
                is Resource.Success -> {
                    _offline.value = false
                    _state.value = UIState(data = result.data)
                }
                is Resource.Error -> {
                    val cached = result.data
                    _offline.value = cached != null
                    _state.value = if (cached != null) {
                        UIState(data = cached)
                    } else {
                        UIState(message = result.message ?: "No se pudo cargar el animal")
                    }
                }
            }
        }
    }
}
