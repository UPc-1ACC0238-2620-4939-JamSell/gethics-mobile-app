package com.jamsell.gethics.livestock.presentation.animal_list

import com.jamsell.gethics.livestock.presentation.LOAD_INVENTORY_ERROR
import com.jamsell.gethics.livestock.presentation.QR_LOOKUP_ERROR
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

/** Mensaje cuando se busca por QR sin escribir ni escanear un codigo; la pantalla lo traduce. */
const val QR_REQUIRED_ERROR = "Escanea o escribe el código QR"

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

    // ---------------- US29: buscar un animal por su QR ----------------

    /** true mientras se muestra el dialogo "Buscar por QR". */
    private val _qrDialog = mutableStateOf(false)
    val qrDialog: State<Boolean> get() = _qrDialog

    private val _qrLooking = mutableStateOf(false)
    val qrLooking: State<Boolean> get() = _qrLooking

    /** Mensaje si el codigo esta vacio, no existe (404) o no hubo conexion ni copia guardada; null si no hay error. */
    private val _qrError = mutableStateOf<String?>(null)
    val qrError: State<String?> get() = _qrError

    /** Id del animal encontrado: la pantalla abre su ficha y llama a consumeQrResult(). */
    private val _qrResult = mutableStateOf<String?>(null)
    val qrResult: State<String?> get() = _qrResult

    fun openQrDialog() {
        _qrError.value = null
        _qrDialog.value = true
    }

    fun closeQrDialog() {
        _qrDialog.value = false
        _qrError.value = null
    }

    /** Busca el animal de ese codigo. Sin conexion, si el animal esta guardado en el telefono, se abre igual (US-36). */
    fun lookUpQr(code: String) {
        if (_qrLooking.value) return
        if (code.isBlank()) {
            _qrError.value = QR_REQUIRED_ERROR
            return
        }
        _qrError.value = null
        _qrLooking.value = true
        viewModelScope.launch {
            when (val result = repository.getAnimalByQr(code)) {
                is Resource.Success -> found(result.data?.id)
                is Resource.Error -> {
                    val cached = result.data
                    if (cached != null) found(cached.id) else _qrError.value = result.message ?: QR_LOOKUP_ERROR
                }
            }
            _qrLooking.value = false
        }
    }

    private fun found(id: String?) {
        _qrDialog.value = false
        _qrResult.value = id
    }

    fun consumeQrResult() {
        _qrResult.value = null
    }

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
                        _state.value = UIState(message = result.message ?: LOAD_INVENTORY_ERROR)
                    }
                }
            }
        }
    }

    /** US-36. Cuando se actualizaron por ultima vez los datos guardados (solo se muestra con el aviso sin conexion). */
    private val _lastUpdated = mutableStateOf<Long?>(null)
    val lastUpdated: State<Long?> get() = _lastUpdated

    private fun show(animals: List<Animal>, query: String, status: AnimalStatus, offline: Boolean) {
        _offline.value = offline
        if (offline) _lastUpdated.value = repository.lastSyncMillis()
        if (query.isBlank() && status == AnimalStatus.ACTIVE) _total.value = animals.size
        _state.value = UIState(data = animals, message = if (animals.isEmpty()) emptyMessage(query) else "")
    }
}
