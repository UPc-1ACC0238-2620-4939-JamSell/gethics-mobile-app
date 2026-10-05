package com.jamsell.gethics.livestock.presentation.animal_edit

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.livestock.presentation.animal_register.FUTURE_BIRTH_DATE_ERROR
import com.jamsell.gethics.livestock.presentation.animal_register.REQUIRED_FIELD_ERROR
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Errores por campo del formulario de edicion; null = campo valido. El arete no se edita, asi que no se valida. */
data class AnimalEditErrors(val breed: String? = null, val birthDate: String? = null) {
    val hasErrors get() = breed != null || birthDate != null
}

fun validateAnimalEdit(breed: String, birthDate: LocalDate?, today: LocalDate = LocalDate.now()) = AnimalEditErrors(
    breed = if (breed.isBlank()) REQUIRED_FIELD_ERROR else null,
    birthDate = when {
        birthDate == null -> REQUIRED_FIELD_ERROR
        birthDate.isAfter(today) -> FUTURE_BIRTH_DATE_ERROR
        else -> null
    }
)

/** clock: el "hoy" sale de la zona horaria del dispositivo; inyectable para tests. */
class AnimalEditViewModel(
    private val repository: LivestockRepository,
    private val animalId: String,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    // Animal cargado para precargar el formulario; data null -> message es el error
    private val _animal = mutableStateOf(UIState<Animal>(isLoading = true))
    val animal: State<UIState<Animal>> get() = _animal

    // data = true -> cambios guardados
    private val _saveState = mutableStateOf(UIState<Boolean>())
    val saveState: State<UIState<Boolean>> get() = _saveState

    private val _errors = mutableStateOf(AnimalEditErrors())
    val errors: State<AnimalEditErrors> get() = _errors

    init {
        load()
    }

    fun load() {
        _animal.value = UIState(isLoading = true)
        viewModelScope.launch {
            _animal.value = when (val result = repository.getAnimal(animalId)) {
                is Resource.Success -> UIState(data = result.data)
                // Aun con cache se bloquea la edicion sin conexion: guardar necesita al backend.
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar el animal")
            }
        }
    }

    /** Escenario 1: guarda los cambios. El "Cancelar" del escenario 2 no llega aqui: la pantalla solo vuelve atras. */
    fun save(name: String, breed: String, sex: String?, birthDate: LocalDate?) {
        val current = _animal.value.data ?: return
        val found = validateAnimalEdit(breed, birthDate, LocalDate.now(clock))
        _errors.value = found
        if (found.hasErrors || birthDate == null) return

        _saveState.value = UIState(isLoading = true)
        viewModelScope.launch {
            val result = repository.updateAnimal(current, name.trim().ifEmpty { null }, breed.trim(), sex, birthDate.toString())
            _saveState.value = when (result) {
                is Resource.Success -> UIState(data = true)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo guardar los cambios")
            }
        }
    }
}
