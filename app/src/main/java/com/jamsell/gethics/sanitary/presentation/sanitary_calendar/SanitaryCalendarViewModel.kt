package com.jamsell.gethics.sanitary.presentation.sanitary_calendar

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.sanitary.data.ScheduledEventDto
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth

/** Fallback si el backend no enviara message con la lista vacia (escenario 2). */
const val NO_PENDING_ACTIVITIES = "No hay actividades pendientes."

/** clock: el mes actual sale de la zona horaria del dispositivo; inyectable para tests. */
class SanitaryCalendarViewModel(
    private val repository: SanitaryRepository,
    clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _selectedMonth = mutableStateOf(YearMonth.now(clock))
    val selectedMonth: State<YearMonth> get() = _selectedMonth

    // data = eventos del mes (vacia -> message es el aviso de "sin actividades"); data null -> message es el error
    private val _state = mutableStateOf(UIState<List<ScheduledEventDto>>())
    val state: State<UIState<List<ScheduledEventDto>>> get() = _state

    private var loadJob: Job? = null

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
        _state.value = UIState(isLoading = true)
        loadJob = viewModelScope.launch {
            _state.value = when (val result = repository.getCalendar(period)) {
                is Resource.Success -> {
                    val calendar = result.data!!
                    val emptyMessage = calendar.message?.takeIf { it.isNotBlank() } ?: NO_PENDING_ACTIVITIES
                    UIState(data = calendar.events, message = if (calendar.events.isEmpty()) emptyMessage else "")
                }
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar el calendario")
            }
        }
    }
}
