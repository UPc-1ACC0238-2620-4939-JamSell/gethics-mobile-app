package com.jamsell.gethics.livestock.presentation.animal_detail

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.livestock.data.repository.FarmRepository
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.livestock.domain.model.Farm
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AnimalDetailViewModel(
    private val repository: LivestockRepository,
    private val animalId: String,
    private val farmRepository: FarmRepository? = null
) : ViewModel() {

    // data = animal; data null -> message es el error. Mientras recarga conserva el animal anterior para no parpadear.
    private val _state = mutableStateOf(UIState<Animal>(isLoading = true))
    val state: State<UIState<Animal>> get() = _state

    /** true cuando el animal viene del cache de Room porque no hubo conexion. */
    private val _offline = mutableStateOf(false)
    val offline: State<Boolean> get() = _offline

    /** Granja asignada al animal; null mientras carga, si no tiene granja o si no se pudo traer (sin conexion). */
    private val _farm = mutableStateOf<Farm?>(null)
    val farm: State<Farm?> get() = _farm

    private var loadJob: Job? = null

    // ---------------- US08: baja logica ----------------

    /** true mientras se muestra el dialogo "Dar de baja este animal?" (confirmar / cancelar). */
    private val _confirmingDeactivation = mutableStateOf(false)
    val confirmingDeactivation: State<Boolean> get() = _confirmingDeactivation

    private val _deactivating = mutableStateOf(false)
    val deactivating: State<Boolean> get() = _deactivating

    /** Mensaje del backend (404, 409...) o de conexion si la baja fallo; null si no hay error que mostrar. */
    private val _deactivationError = mutableStateOf<String?>(null)
    val deactivationError: State<String?> get() = _deactivationError

    /** true cuando la baja se confirmo: la pantalla vuelve a la lista, donde el animal ya no aparece. */
    private val _deactivated = mutableStateOf(false)
    val deactivated: State<Boolean> get() = _deactivated

    /** La pantalla lo llama al reanudarse: asi vuelve a cargar al regresar de editar. */
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UIState(isLoading = true, data = _state.value.data)
            when (val result = repository.getAnimal(animalId)) {
                is Resource.Success -> {
                    _offline.value = false
                    _state.value = UIState(data = result.data)
                    loadFarm(result.data?.farmId)
                }
                is Resource.Error -> {
                    val cached = result.data
                    _offline.value = cached != null
                    _state.value = if (cached != null) {
                        UIState(data = cached)
                    } else {
                        UIState(message = result.message ?: "No se pudo cargar el animal")
                    }
                    loadFarm(cached?.farmId)
                }
            }
        }
    }

    /** Trae la granja despues de mostrar el animal, para que la ficha no espere por ella. Un fallo solo deja el dato vacio. */
    private suspend fun loadFarm(farmId: String?) {
        val repository = farmRepository
        if (farmId == null || repository == null) {
            _farm.value = null
            return
        }
        if (_farm.value?.id == farmId) return
        _farm.value = when (val result = repository.getFarm(farmId)) {
            is Resource.Success -> result.data
            is Resource.Error -> null
        }
    }

    /** Boton "Dar de baja": solo abre la confirmacion, no cambia nada todavia. */
    fun requestDeactivation() {
        _deactivationError.value = null
        _confirmingDeactivation.value = true
    }

    /** Escenario 2, baja cancelada: se cierra el dialogo y el animal queda sin cambios (no se llama al backend). */
    fun cancelDeactivation() {
        _confirmingDeactivation.value = false
    }

    /** Escenario 1, baja confirmada: llama al backend; si sale bien, el animal pasa a INACTIVE y se avisa a la pantalla. */
    fun confirmDeactivation() {
        if (_deactivating.value) return
        _confirmingDeactivation.value = false
        _deactivating.value = true
        viewModelScope.launch {
            when (val result = repository.deactivateAnimal(animalId)) {
                is Resource.Success -> _deactivated.value = true
                is Resource.Error -> _deactivationError.value = result.message ?: "No se pudo dar de baja el animal"
            }
            _deactivating.value = false
        }
    }

    fun dismissDeactivationError() {
        _deactivationError.value = null
    }
}
