package com.jamsell.gethics.sanitary.presentation.schedule_vaccination

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.ScheduleSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Mismo texto que PastScheduledDateException del backend: la regla es suya, aqui solo se evita la llamada. */
const val PAST_SCHEDULED_DATE_ERROR = "La fecha programada no puede ser anterior a hoy."

/** La fecha programada puede ser hoy o posterior (@FutureOrPresent en ScheduleSanitaryEventResource). */
fun validateScheduledDate(date: LocalDate, today: LocalDate): String? =
    if (date.isBefore(today)) PAST_SCHEDULED_DATE_ERROR else null

/**
 * US13 (parte realizable): programa una vacunacion. El backend crea el evento SCHEDULED y su job genera el recordatorio
 * 3 dias antes; hoy ese envio solo se registra en el log del backend (no hay push real).
 */
class ScheduleVaccinationViewModel(
    private val repository: SanitaryRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    // data = true -> vacuna programada (la pantalla vuelve atras)
    private val _state = mutableStateOf(UIState<Boolean>())
    val state: State<UIState<Boolean>> get() = _state

    fun schedule(animalId: String, date: LocalDate, description: String) {
        validateScheduledDate(date, LocalDate.now(clock))?.let {
            _state.value = UIState(message = it)
            return
        }
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            val request = ScheduleSanitaryEventRequest(SanitaryEventType.VACCINATION, date.toString(), description.trim().ifEmpty { null })
            _state.value = when (val result = repository.scheduleEvent(animalId, request)) {
                is Resource.Success -> UIState(data = true)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo programar la vacuna")
            }
        }
    }
}
