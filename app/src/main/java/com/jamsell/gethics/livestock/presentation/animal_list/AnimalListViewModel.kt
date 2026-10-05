package com.jamsell.gethics.livestock.presentation.animal_list

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.livestock.domain.model.AnimalStatus
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Mensajes con la lista vacia: los mismos textos que envia el backend (escenario 2: "sin resultados"). */
const val NO_ANIMALS = "No hay animales registrados."
const val NO_RESULTS = "Sin resultados."

/** Espera tras la ultima tecla antes de consultar: evita una peticion por letra. */
const val SEARCH_DEBOUNCE_MS = 300L

fun emptyMessage(query: String): String = if (query.isBlank()) NO_ANIMALS else NO_RESULTS

class AnimalListViewModel(private val repository: LivestockRepository) : ViewModel() {

    // data = animales (vacia -> message es "Sin resultados." o "No hay animales registrados."); data null -> message es el error
    private val _state = mutableStateOf(UIState<List<Animal>>(isLoading = true))
    val state: State<UIState<List<Animal>>> get() = _state

    private val _query = mutableStateOf("")
    val query: State<String> get() = _query

    private val _status = mutableStateOf(AnimalStatus.ACTIVE)
    val status: State<AnimalStatus> get() = _status

    /** true cuando la lista viene del cache de Room porque no hubo conexion. */
    private val _offline = mutableStateOf(false)
    val offline: State<Boolean> get() = _offline

    /** Total de animales activos del hato; null hasta conocerlo. Solo se actualiza con la lista completa (sin busqueda). */
    private val _total = mutableStateOf<Int?>(null)
    val total: State<Int?> get() = _total

    private var loadJob: Job? = null

    fun onQueryChange(text: String) {
        _query.value = text
        load(SEARCH_DEBOUNCE_MS)
    }

    fun onStatusChange(status: AnimalStatus) {
        if (status == _status.value) return
        _status.value = status
        load()
    }

    /** Consulta con la busqueda y el estado actuales. Mientras carga conserva la lista anterior para no parpadear. */
    fun load(debounceMs: Long = 0) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            val query = _query.value
            val status = _status.value
            _state.value = UIState(isLoading = true, data = _state.value.data)
            when (val result = repository.getAnimals(query, status)) {
                is Resource.Success -> show(result.data.orEmpty(), query, status, offline = false)
                is Resource.Error -> {
                    val cached = result.data
                    if (cached != null) {
                        show(cached, query, status, offline = true)
                    } else {
                        _offline.value = false
                        _state.value = UIState(message = result.message ?: "No se pudo cargar el inventario")
                    }
                }
            }
        }
    }

    private fun show(animals: List<Animal>, query: String, status: AnimalStatus, offline: Boolean) {
        _offline.value = offline
        if (query.isBlank() && status == AnimalStatus.ACTIVE) _total.value = animals.size
        _state.value = UIState(data = animals, message = if (animals.isEmpty()) emptyMessage(query) else "")
    }
}
