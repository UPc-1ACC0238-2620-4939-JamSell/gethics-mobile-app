package com.jamsell.gethics.livestock.presentation.farm_edit

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.livestock.data.repository.FarmRepository
import com.jamsell.gethics.livestock.domain.model.Farm
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

/** Mensajes de validacion (US-32); mismas reglas del backend. La pantalla los traduce al idioma de la app. */
const val FARM_NAME_REQUIRED_ERROR = "El nombre de la granja es obligatorio"
const val FARM_NAME_TOO_LONG_ERROR = "El nombre no puede superar 100 caracteres"
const val FARM_LOCATION_REQUIRED_ERROR = "La ubicación es obligatoria"
const val FARM_LOCATION_TOO_LONG_ERROR = "La ubicación no puede superar 200 caracteres"
const val FARM_SIZE_INVALID_ERROR = "El tamaño debe ser mayor a 0 (hasta 2 decimales)"

data class FarmEditErrors(val name: String? = null, val location: String? = null, val size: String? = null) {
    val hasErrors get() = name != null || location != null || size != null
}

/** Tamano vacio = sin registrar (valido); si se escribe debe ser > 0 con hasta 7 enteros y 2 decimales. */
fun parseSize(text: String): Double? {
    val normalized = text.trim().replace(',', '.')
    if (!Regex("""\d{1,7}(\.\d{1,2})?""").matches(normalized)) return null
    return normalized.toDoubleOrNull()?.takeIf { it > 0 }
}

fun validateFarm(name: String, location: String, sizeText: String) = FarmEditErrors(
    name = when {
        name.isBlank() -> FARM_NAME_REQUIRED_ERROR
        name.trim().length > 100 -> FARM_NAME_TOO_LONG_ERROR
        else -> null
    },
    location = when {
        location.isBlank() -> FARM_LOCATION_REQUIRED_ERROR
        location.trim().length > 200 -> FARM_LOCATION_TOO_LONG_ERROR
        else -> null
    },
    size = if (sizeText.isNotBlank() && parseSize(sizeText) == null) FARM_SIZE_INVALID_ERROR else null
)

class FarmEditViewModel(
    private val repository: FarmRepository,
    private val farmId: String,
    private val ownerId: String
) : ViewModel() {

    // Granja cargada para precargar el formulario; data null -> message es el error
    private val _farm = mutableStateOf(UIState<Farm>(isLoading = true))
    val farm: State<UIState<Farm>> get() = _farm

    // data = true -> cambios guardados
    private val _saveState = mutableStateOf(UIState<Boolean>())
    val saveState: State<UIState<Boolean>> get() = _saveState

    private val _errors = mutableStateOf(FarmEditErrors())
    val errors: State<FarmEditErrors> get() = _errors

    init {
        load()
    }

    fun load() {
        _farm.value = UIState(isLoading = true)
        viewModelScope.launch {
            _farm.value = when (val result = repository.getFarm(ownerId, farmId)) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar la granja")
            }
        }
    }

    /** Escenario 1: guarda los cambios. El "Cancelar" solo vuelve atras. 409 (nombre repetido) se muestra con el texto del backend. */
    fun save(name: String, location: String, sizeText: String) {
        if (_saveState.value.isLoading) return
        val found = validateFarm(name, location, sizeText)
        _errors.value = found
        if (found.hasErrors) return

        _saveState.value = UIState(isLoading = true)
        viewModelScope.launch {
            val size = sizeText.takeIf { it.isNotBlank() }?.let(::parseSize)
            _saveState.value = when (val result = repository.updateFarm(farmId, name.trim(), location.trim(), size)) {
                is Resource.Success -> UIState(data = true)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo guardar la granja")
            }
        }
    }
}
