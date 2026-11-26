package com.jamsell.gethics.sanitary.presentation.sanitary_calendar

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.sanitary.data.CompleteSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.ScheduledEventDto
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import com.jamsell.gethics.sanitary.presentation.register_event.toOccurredAt
import com.jamsell.gethics.sanitary.presentation.register_event.validateEventDate
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/** clock: el mes actual sale de la zona horaria del dispositivo; inyectable para tests. */
class SanitaryCalendarViewModel(
    private val repository: SanitaryRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _selectedMonth = mutableStateOf(YearMonth.now(clock))
    val selectedMonth: State<YearMonth> get() = _selectedMonth

    // data = eventos del mes (vacia -> message es el aviso de "sin actividades"); data null -> message es el error
    private val _state = mutableStateOf(UIState<List<ScheduledEventDto>>())
    val state: State<UIState<List<ScheduledEventDto>>> get() = _state

    private var loadJob: Job? = null

    // US13: registrar como aplicada una vacunacion programada (true mientras la peticion esta en curso)
    private val _completing = mutableStateOf(false)
    val completing: State<Boolean> get() = _completing

    // US13: validacion local o message del backend al registrar como aplicada ("" = sin mensaje)
    private val _actionMessage = mutableStateOf("")
    val actionMessage: State<String> get() = _actionMessage

    // US33: cancelar un evento programado (true mientras la peticion esta en curso)
    private val _cancelling = mutableStateOf(false)
    val cancelling: State<Boolean> get() = _cancelling

    // US47: reprogramar un evento programado (true mientras la peticion esta en curso)
    private val _rescheduling = mutableStateOf(false)
    val rescheduling: State<Boolean> get() = _rescheduling

    // US47: error a mostrar DENTRO del dialogo de reprogramacion (null = sin error); el dialogo no se cierra
    private val _rescheduleError = mutableStateOf<RescheduleError?>(null)
    val rescheduleError: State<RescheduleError?> get() = _rescheduleError

    init {
        load()
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
        load()
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        load()
    }

    fun load() {
        loadJob?.cancel() // evita que la respuesta de un mes anterior pise la del mes seleccionado
        val period = _selectedMonth.value
        _actionMessage.value = ""
        _state.value = UIState(isLoading = true)
        loadJob = viewModelScope.launch {
            _state.value = when (val result = repository.getCalendar(period)) {
                is Resource.Success -> {
                    val calendar = result.data!!
                    // Sin message del backend queda "" y la pantalla muestra sanitary_calendar_empty en el idioma de la app
                    val emptyMessage = calendar.message?.takeIf { it.isNotBlank() }.orEmpty()
                    UIState(data = calendar.events, message = if (calendar.events.isEmpty()) emptyMessage else "")
                }
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar el calendario")
            }
        }
    }

    /**
     * US13: registra como aplicada (SCHEDULED -> COMPLETED) una vacunacion del calendario y recarga el mes: el backend solo
     * devuelve eventos SCHEDULED, asi que la vacuna desaparece de la lista sin estados locales. Misma regla de fecha que US11.
     */
    fun completeVaccination(event: ScheduledEventDto, appliedOn: LocalDate) {
        validateEventDate(appliedOn, LocalDate.now(clock))?.let {
            _actionMessage.value = it
            return
        }
        _actionMessage.value = ""
        _completing.value = true
        viewModelScope.launch {
            val request = CompleteSanitaryEventRequest(occurredAt = toOccurredAt(appliedOn), description = null)
            val result = repository.completeEvent(event.animalId, event.id, request)
            _completing.value = false
            when (result) {
                is Resource.Success -> load()
                is Resource.Error -> _actionMessage.value = result.message ?: "No se pudo registrar la vacuna como aplicada"
            }
        }
    }

    fun clearRescheduleError() {
        _rescheduleError.value = null
    }

    /**
     * US47: cambia la fecha (y, si se escriben, las notas) de un evento programado. Solo si el backend lo acepta se llama
     * onDone (la pantalla cierra el dialogo) y se recarga el mes; si falla, el error queda en rescheduleError y el dialogo
     * sigue abierto con lo que el usuario ingreso. Una sola reprogramacion a la vez.
     */
    fun rescheduleEvent(event: ScheduledEventDto, newDate: LocalDate, notes: String, onDone: () -> Unit) {
        if (_rescheduling.value) return
        if (newDate.isBefore(LocalDate.now(clock))) {
            _rescheduleError.value = RescheduleError.PastDate
            return
        }
        _rescheduleError.value = null
        _rescheduling.value = true
        viewModelScope.launch {
            val description = notes.trim().takeIf { it.isNotEmpty() }
            val result = repository.rescheduleEvent(event.animalId, event.id, newDate.toString(), description)
            _rescheduling.value = false
            when (result) {
                is Resource.Success -> {
                    onDone()
                    load()
                }
                is Resource.Error -> _rescheduleError.value = RescheduleError.Server(result.message.orEmpty())
            }
        }
    }

    /**
     * US33: cancela un evento programado y recarga el mes. Sin estados locales: si el backend deja de devolverlo como
     * SCHEDULED, desaparece del calendario. Una sola cancelacion a la vez (ignora toques repetidos).
     */
    fun cancelEvent(event: ScheduledEventDto) {
        if (_cancelling.value) return
        _actionMessage.value = ""
        _cancelling.value = true
        viewModelScope.launch {
            val result = repository.cancelEvent(event.animalId, event.id)
            _cancelling.value = false
            when (result) {
                is Resource.Success -> load()
                is Resource.Error -> _actionMessage.value = result.message ?: "No se pudo cancelar el evento"
            }
        }
    }
}

/** US47: PastDate se traduce en la pantalla; Server trae el message del backend (o vacio para el texto generico). */
sealed interface RescheduleError {
    data object PastDate : RescheduleError
    data class Server(val message: String) : RescheduleError
}
