package com.jamsell.gethics.sanitary.presentation.register_event

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val FUTURE_DATE_ERROR = "La fecha del evento no puede ser futura"

/** Escenario 2 (US11): un evento ya ocurrido no puede tener fecha posterior a hoy. Devuelve el error o null. */
fun validateEventDate(date: LocalDate, today: LocalDate = LocalDate.now()): String? =
    if (date.isAfter(today)) FUTURE_DATE_ERROR else null

private val OCCURRED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

/**
 * La UI solo pide fecha: se envia a las 00:00:00 porque nunca queda "en el futuro" para el dia de hoy,
 * compare el backend contra la fecha o contra la fecha-hora actual.
 */
fun toOccurredAt(date: LocalDate): String = date.atStartOfDay().format(OCCURRED_AT_FORMAT)

/** clock: el "hoy" sale de la zona horaria del dispositivo; inyectable para tests. */
class RegisterEventViewModel(
    private val repository: SanitaryRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    // data = true -> evento guardado
    private val _state = mutableStateOf(UIState<Boolean>())
    val state: State<UIState<Boolean>> get() = _state

    fun save(animalId: String, type: SanitaryEventType, date: LocalDate, description: String) {
        validateEventDate(date, LocalDate.now(clock))?.let {
            _state.value = UIState(message = it)
            return
        }
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            val request = RegisterSanitaryEventRequest(type, toOccurredAt(date), description.trim().ifEmpty { null })
            _state.value = when (val result = repository.registerEvent(animalId, request)) {
                is Resource.Success -> UIState(data = true)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo registrar el evento")
            }
        }
    }
}
