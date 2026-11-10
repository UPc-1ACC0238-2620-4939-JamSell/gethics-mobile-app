package com.jamsell.gethics.livestock.presentation.animal_detail

import com.jamsell.gethics.livestock.presentation.LOAD_ANIMAL_ERROR
import com.jamsell.gethics.livestock.presentation.DEACTIVATE_ANIMAL_ERROR
import com.jamsell.gethics.livestock.presentation.SELL_ANIMAL_ERROR
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.shared.common.Constants
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

class AnimalDetailViewModel(
    private val repository: LivestockRepository,
    private val animalId: String,
    // Dueno de la sesion: el backend lo pide para vender un animal que no tiene granja (US-30).
    private val ownerId: String = Constants.DEFAULT_OWNER_ID,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    // data = animal; data null -> message es el error. Mientras recarga conserva el animal anterior para no parpadear.
    private val _state = mutableStateOf(UIState<Animal>(isLoading = true))
    val state: State<UIState<Animal>> get() = _state

    /** true cuando el animal viene del cache de Room porque no hubo conexion. */
    private val _offline = mutableStateOf(false)
    val offline: State<Boolean> get() = _offline

    /** US-36. Cuando se actualizaron por ultima vez los datos guardados (solo se muestra con el aviso sin conexion). */
    private val _lastUpdated = mutableStateOf<Long?>(null)
    val lastUpdated: State<Long?> get() = _lastUpdated

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
                }
                is Resource.Error -> {
                    val cached = result.data
                    _offline.value = cached != null
                    if (cached != null) _lastUpdated.value = repository.lastSyncMillis()
                    _state.value = if (cached != null) {
                        UIState(data = cached)
                    } else {
                        UIState(message = result.message ?: LOAD_ANIMAL_ERROR)
                    }
                }
            }
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
                is Resource.Error -> _deactivationError.value = result.message ?: DEACTIVATE_ANIMAL_ERROR
            }
            _deactivating.value = false
        }
    }

    fun dismissDeactivationError() {
        _deactivationError.value = null
    }

    // ---------------- US30: venta ----------------

    /** true mientras se muestra el formulario de venta (precio y fecha). */
    private val _sellingForm = mutableStateOf(false)
    val sellingForm: State<Boolean> get() = _sellingForm

    private val _saleErrors = mutableStateOf(SaleErrors())
    val saleErrors: State<SaleErrors> get() = _saleErrors

    private val _selling = mutableStateOf(false)
    val selling: State<Boolean> get() = _selling

    /** Mensaje del backend (404, 409...) o de conexion si la venta fallo; null si no hay error que mostrar. */
    private val _saleError = mutableStateOf<String?>(null)
    val saleError: State<String?> get() = _saleError

    /** true cuando la venta se registro: la pantalla vuelve a la lista, donde el animal ya no aparece como activo. */
    private val _sold = mutableStateOf(false)
    val sold: State<Boolean> get() = _sold

    /** Fecha que propone el formulario: hoy, en la zona horaria del dispositivo. */
    fun defaultSaleDate(): String = LocalDate.now(clock).toString()

    fun requestSale() {
        _saleError.value = null
        _saleErrors.value = SaleErrors()
        _sellingForm.value = true
    }

    /** Venta cancelada: se cierra el formulario y no se llama al backend. */
    fun cancelSale() {
        _sellingForm.value = false
        _saleErrors.value = SaleErrors()
    }

    /** Valida precio y fecha; si estan bien, registra la venta. Con errores el formulario sigue abierto. */
    fun confirmSale(priceText: String, dateText: String) {
        if (_selling.value) return
        val animal = _state.value.data ?: return
        val found = validateSale(priceText, dateText, LocalDate.now(clock))
        _saleErrors.value = found
        if (found.hasErrors) return
        val price = parsePrice(priceText) ?: return
        val date = parseSaleDate(dateText) ?: return

        _sellingForm.value = false
        _selling.value = true
        viewModelScope.launch {
            when (val result = repository.sellAnimal(animal, price, date.toString(), ownerId)) {
                is Resource.Success -> _sold.value = true
                is Resource.Error -> _saleError.value = result.message ?: SELL_ANIMAL_ERROR
            }
            _selling.value = false
        }
    }

    fun dismissSaleError() {
        _saleError.value = null
    }
}
