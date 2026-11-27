package com.jamsell.gethics.iot.presentation.readings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.iot.data.SensorReadingResponse
import com.jamsell.gethics.iot.data.repository.CONNECTION_ERROR
import com.jamsell.gethics.iot.data.repository.IotRepository
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

const val READINGS_PAGE_SIZE = 20

/** El backend identifica los dispositivos con UUID; cualquier otro valor no es un contexto de dispositivo valido. */
internal fun isValidDeviceId(id: String): Boolean =
    runCatching { UUID.fromString(id).toString() == id.lowercase() }.getOrDefault(false)

/**
 * US37 + US48: lecturas de un dispositivo, paginadas y filtradas por rango de dias (from/to opcionales e inclusivos).
 *
 * from/to son el borrador que edita el usuario; solo applyFilters/clearFilters los vuelven filtro aplicado
 * (appliedFrom/appliedTo) y relanzan la consulta desde la pagina 0. Las paginas siguientes usan el filtro aplicado.
 */
class DeviceReadingsViewModel(
    private val repository: IotRepository,
    deviceId: String
) : ViewModel() {

    var deviceId by mutableStateOf(deviceId)
        private set
    var readings by mutableStateOf<List<SensorReadingResponse>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    // true cuando ya se cargo la primera pagina; permite distinguir "cargando" de "sin lecturas"
    var loadedOnce by mutableStateOf(false)
        private set
    var hasMore by mutableStateOf(false)
        private set
    var totalElements by mutableStateOf(0L)
        private set
    var totalPages by mutableStateOf(0)
        private set

    // Error del servidor (o generico) y, aparte, error de red para que la pantalla lo muestre en el idioma actual
    var message by mutableStateOf("")
        private set
    var connectionError by mutableStateOf(false)
        private set

    var from by mutableStateOf<LocalDate?>(null)
        private set
    var to by mutableStateOf<LocalDate?>(null)
        private set
    var appliedFrom by mutableStateOf<LocalDate?>(null)
        private set
    var appliedTo by mutableStateOf<LocalDate?>(null)
        private set
    var rangeInvalid by mutableStateOf(false)
        private set

    val hasValidDevice: Boolean get() = isValidDeviceId(deviceId)
    val hasError: Boolean get() = message.isNotEmpty() || connectionError
    val hasActiveFilters: Boolean get() = appliedFrom != null || appliedTo != null

    private var nextPage = 0
    private var generation = 0 // cambia al cambiar dispositivo/filtros: las respuestas de una generacion anterior se descartan
    private var job: Job? = null

    init {
        reload()
    }

    /** Cambia de dispositivo: se descarta lo cargado, los filtros y cualquier consulta en vuelo. */
    fun selectDevice(id: String) {
        if (id == deviceId) return
        deviceId = id
        from = null
        to = null
        appliedFrom = null
        appliedTo = null
        rangeInvalid = false
        reload()
    }

    fun onFromChange(date: LocalDate?) {
        from = date
        rangeInvalid = false
    }

    fun onToChange(date: LocalDate?) {
        to = date
        rangeInvalid = false
    }

    /** Valida el rango y, si es valido, consulta desde la pagina 0. Un rango invertido no llega al backend. */
    fun applyFilters() {
        val start = from
        val end = to
        if (start != null && end != null && start.isAfter(end)) {
            rangeInvalid = true
            return
        }
        rangeInvalid = false
        applyDraft()
    }

    fun clearFilters() {
        from = null
        to = null
        rangeInvalid = false
        applyDraft()
    }

    fun loadMore() {
        if (isLoading || !hasMore) return
        load()
    }

    /** Repite la pagina que fallo (con los mismos filtros). */
    fun retry() {
        if (isLoading) return
        load()
    }

    private fun applyDraft() {
        // Mismo filtro y ya hay una consulta en curso: seria una solicitud duplicada
        if (from == appliedFrom && to == appliedTo && isLoading) return
        appliedFrom = from
        appliedTo = to
        reload()
    }

    private fun reload() {
        generation++
        job?.cancel()
        isLoading = false
        nextPage = 0
        readings = emptyList()
        hasMore = false
        loadedOnce = false
        totalElements = 0
        totalPages = 0
        message = ""
        connectionError = false
        load()
    }

    private fun load() {
        if (!hasValidDevice) return // sin dispositivo legitimo no se consulta nada
        isLoading = true
        message = ""
        connectionError = false
        val requestGeneration = generation
        val requestPage = nextPage
        val id = deviceId
        val start = appliedFrom
        val end = appliedTo
        job = viewModelScope.launch {
            val result = repository.getReadings(id, requestPage, READINGS_PAGE_SIZE, start, end)
            if (requestGeneration != generation) return@launch
            when (result) {
                is Resource.Success -> result.data?.let { page ->
                    readings = readings + page.content
                    hasMore = page.hasMore
                    totalElements = page.totalElements
                    totalPages = page.totalPages
                    nextPage = page.page + 1
                    loadedOnce = true
                }
                is Resource.Error -> {
                    hasMore = false
                    connectionError = result.message == CONNECTION_ERROR
                    message = if (connectionError) "" else result.message ?: "No se pudieron cargar las lecturas"
                }
            }
            isLoading = false
        }
    }
}
