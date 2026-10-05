package com.jamsell.gethics.sanitary.presentation.clinical_history

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.sanitary.data.ClinicalHistoryEventDto
import com.jamsell.gethics.sanitary.data.SanitaryEventStatus
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Fallback si el backend no enviara message con la lista vacia (escenario 2). */
const val NO_RECORDS = "Sin registros."

/**
 * Fecha visible del evento, con la misma regla que usa el backend para ordenar:
 * COMPLETED -> occurredAt (aunque conserve scheduledDate); SCHEDULED/CANCELLED -> scheduledDate.
 * null si no hay fecha utilizable: no se inventa ninguna.
 */
fun effectiveDate(event: ClinicalHistoryEventDto): LocalDate? = when (event.status) {
    SanitaryEventStatus.COMPLETED -> event.occurredAt?.let { LocalDateTime.parse(it).toLocalDate() }
    SanitaryEventStatus.SCHEDULED, SanitaryEventStatus.CANCELLED -> event.scheduledDate?.let(LocalDate::parse)
    null -> null
}

class ClinicalHistoryViewModel(
    private val repository: SanitaryRepository,
    private val animalId: String
) : ViewModel() {

    // data = eventos en el orden del backend (vacia -> message es "Sin registros."); data null -> message es el error
    private val _state = mutableStateOf(UIState<List<ClinicalHistoryEventDto>>())
    val state: State<UIState<List<ClinicalHistoryEventDto>>> get() = _state

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.value = UIState(isLoading = true)
        loadJob = viewModelScope.launch {
            _state.value = when (val result = repository.getClinicalHistory(animalId)) {
                is Resource.Success -> {
                    val history = result.data!!
                    val emptyMessage = history.message?.takeIf { it.isNotBlank() } ?: NO_RECORDS
                    UIState(data = history.events, message = if (history.events.isEmpty()) emptyMessage else "")
                }
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar el historial")
            }
        }
    }
}
