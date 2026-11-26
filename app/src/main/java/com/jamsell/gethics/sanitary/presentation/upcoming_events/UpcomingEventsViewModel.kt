package com.jamsell.gethics.sanitary.presentation.upcoming_events

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.UpcomingSanitaryEventDto
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

/** US46: ventana de alerta por defecto, la misma que usa el backend si no se envia days. */
const val UPCOMING_WINDOW_DAYS = 7

/** US46: hasta cuantos dias antes un evento se marca como urgente (ambar). */
const val URGENT_WITHIN_DAYS = 2

/** US46: nivel de proximidad que la pantalla traduce a color y texto. */
enum class EventUrgency { OVERDUE, URGENT, UPCOMING }

fun urgencyOf(daysUntil: Long, overdue: Boolean): EventUrgency = when {
    overdue || daysUntil < 0 -> EventUrgency.OVERDUE
    daysUntil <= URGENT_WITHIN_DAYS -> EventUrgency.URGENT
    else -> EventUrgency.UPCOMING
}

/** Evento listo para mostrar: animalLabel es el nombre o arete del animal (null si no se pudo resolver). */
data class UpcomingEventItem(
    val id: String,
    val animalId: String,
    val animalLabel: String?,
    val type: SanitaryEventType?,
    val scheduledDate: LocalDate,
    val description: String?,
    val daysUntil: Long,
    val urgency: EventUrgency
)

/**
 * US46. loadAnimalNames: animalId -> nombre o arete. Es una funcion para no acoplar el ViewModel a livestock (en la app
 * la provee LivestockRepository); si falla, la lista se muestra igual sin el nombre del animal.
 */
class UpcomingEventsViewModel(
    private val repository: SanitaryRepository,
    private val loadAnimalNames: suspend () -> Map<String, String> = { emptyMap() }
) : ViewModel() {

    // data = eventos (vacia -> message es el aviso "sin alertas" del backend o "" para el fallback); data null -> error
    private val _state = mutableStateOf(UIState<List<UpcomingEventItem>>())
    val state: State<UIState<List<UpcomingEventItem>>> get() = _state

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.value = UIState(isLoading = true)
        loadJob = viewModelScope.launch {
            _state.value = when (val result = repository.getUpcomingEvents(UPCOMING_WINDOW_DAYS)) {
                is Resource.Success -> {
                    val response = result.data!!
                    val names = if (response.events.isEmpty()) emptyMap() else runCatching { loadAnimalNames() }.getOrDefault(emptyMap())
                    val items = response.events.map { it.toItem(names[it.animalId]) }
                    UIState(data = items, message = if (items.isEmpty()) response.message?.takeIf { it.isNotBlank() }.orEmpty() else "")
                }
                is Resource.Error -> UIState(message = result.message.orEmpty())
            }
        }
    }

    private fun UpcomingSanitaryEventDto.toItem(animalLabel: String?) = UpcomingEventItem(
        id = id,
        animalId = animalId,
        animalLabel = animalLabel,
        type = type,
        scheduledDate = LocalDate.parse(scheduledDate),
        description = description?.takeIf { it.isNotBlank() },
        daysUntil = daysUntil,
        urgency = urgencyOf(daysUntil, overdue)
    )
}
