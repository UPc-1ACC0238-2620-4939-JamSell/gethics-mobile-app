package com.jamsell.gethics.livestock.presentation.animal_register

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

const val REQUIRED_FIELD_ERROR = "Este campo es obligatorio"
const val FUTURE_BIRTH_DATE_ERROR = "La fecha de nacimiento no puede ser futura"
const val INVALID_WEIGHT_ERROR = "Ingresa un peso mayor a 0"

/** Razas disponibles en el selector (el backend aun no define un catalogo). */
val ANIMAL_BREEDS = listOf("Holstein", "Jersey", "Angus", "Brahman", "Brown Swiss", "Hereford", "Gyr", "Otra")

/** Errores por campo del formulario; null = campo valido. */
data class AnimalFormErrors(
    val tag: String? = null,
    val breed: String? = null,
    val birthDate: String? = null,
    val weight: String? = null
) {
    val hasErrors get() = tag != null || breed != null || birthDate != null || weight != null
}

/** El peso es opcional: vacio es valido; si se escribe debe ser un numero mayor a 0. */
fun parseWeight(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

fun validateAnimalForm(
    tag: String,
    breed: String,
    birthDate: LocalDate?,
    weight: String,
    today: LocalDate = LocalDate.now()
) = AnimalFormErrors(
    tag = if (tag.isBlank()) REQUIRED_FIELD_ERROR else null,
    breed = if (breed.isBlank()) REQUIRED_FIELD_ERROR else null,
    birthDate = when {
        birthDate == null -> REQUIRED_FIELD_ERROR
        birthDate.isAfter(today) -> FUTURE_BIRTH_DATE_ERROR
        else -> null
    },
    weight = if (weight.isNotBlank() && parseWeight(weight) == null) INVALID_WEIGHT_ERROR else null
)

/** clock: el "hoy" sale de la zona horaria del dispositivo; inyectable para tests. */
class AnimalRegisterViewModel(
    private val repository: LivestockRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    // data = true -> animal guardado
    private val _state = mutableStateOf(UIState<Boolean>())
    val state: State<UIState<Boolean>> get() = _state

    private val _errors = mutableStateOf(AnimalFormErrors())
    val errors: State<AnimalFormErrors> get() = _errors

    fun save(tag: String, breed: String, birthDate: LocalDate?, weight: String, photoUri: String?) {
        val found = validateAnimalForm(tag, breed, birthDate, weight, LocalDate.now(clock))
        _errors.value = found
        if (found.hasErrors || birthDate == null) return

        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            val result = repository.registerAnimal(tag.trim(), breed, birthDate.toString(), parseWeight(weight), photoUri)
            _state.value = when (result) {
                is Resource.Success -> UIState(data = true)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo registrar el animal")
            }
        }
    }
}
