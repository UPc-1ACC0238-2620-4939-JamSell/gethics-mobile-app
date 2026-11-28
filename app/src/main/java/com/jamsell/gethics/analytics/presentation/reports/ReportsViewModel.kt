package com.jamsell.gethics.analytics.presentation.reports

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.analytics.data.ExportedReport
import com.jamsell.gethics.analytics.data.LivestockReportResponse
import com.jamsell.gethics.analytics.data.normalizeCategory
import com.jamsell.gethics.analytics.data.repository.AnalyticsRepository
import com.jamsell.gethics.finance.data.FinancialTransactionType
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

/** El rango es valido si la fecha inicial no es posterior a la final. */
fun isValidRange(from: LocalDate, to: LocalDate): Boolean = !from.isAfter(to)

/**
 * US49. Los filtros con los que se pidio el reporte que se esta viendo. Son los unicos que usa Exportar, asi el CSV
 * siempre corresponde al reporte visible. type null = todos; category null = sin filtro (nunca vacia).
 */
data class ReportFilters(
    val from: LocalDate,
    val to: LocalDate,
    val type: FinancialTransactionType? = null,
    val category: String? = null
) {
    /** Filtros de US49 (tipo o categoria); las fechas son parte del reporte base, no un filtro "activo". */
    val hasActiveFilters: Boolean get() = type != null || category != null
}

/**
 * US20 + US39 + US49: reporte del ganado por rango de fechas (por defecto, el mes actual), con filtros opcionales de
 * tipo de movimiento y categoria, y su exportacion en CSV.
 *
 * from/to/type/category son el borrador que edita el usuario. Solo applyFilters/clearFilters los vuelven
 * [appliedFilters], que es lo que se consulta y lo que exporta Exportar: cambiar un control sin pulsar Aplicar no
 * cambia ni el reporte ni la exportacion.
 *
 * ownerId lo decide quien crea el ViewModel (hoy SessionStorage.getOwnerId(), que cae en DEFAULT_OWNER_ID mientras IAM
 * no entregue el UUID real); este ViewModel no lo valida ni lo inventa.
 */
class ReportsViewModel(
    private val repository: AnalyticsRepository,
    private val ownerId: String
) : ViewModel() {

    private val _from = mutableStateOf(LocalDate.now().withDayOfMonth(1))
    val from: State<LocalDate> get() = _from

    private val _to = mutableStateOf(LocalDate.now())
    val to: State<LocalDate> get() = _to

    // Borrador de los filtros de US49 (no tienen efecto hasta Aplicar)
    private val _type = mutableStateOf<FinancialTransactionType?>(null)
    val type: State<FinancialTransactionType?> get() = _type

    private val _category = mutableStateOf("")
    val category: State<String> get() = _category

    // Filtros del reporte visible; null hasta la primera consulta
    private val _applied = mutableStateOf<ReportFilters?>(null)
    val appliedFilters: State<ReportFilters?> get() = _applied

    // true = la fecha inicial es posterior a la final (la pantalla muestra el aviso en el idioma actual)
    private val _rangeError = mutableStateOf(false)
    val rangeError: State<Boolean> get() = _rangeError

    // data = reporte; data null -> message es el error
    private val _state = mutableStateOf(UIState<LivestockReportResponse>())
    val state: State<UIState<LivestockReportResponse>> get() = _state

    private var loadJob: Job? = null
    private var generation = 0 // cambia con cada consulta nueva: las respuestas de una anterior se descartan

    fun setFrom(date: LocalDate) {
        _from.value = date
    }

    fun setTo(date: LocalDate) {
        _to.value = date
    }

    fun onTypeChange(type: FinancialTransactionType?) {
        _type.value = type
    }

    fun onCategoryChange(text: String) {
        _category.value = text
    }

    /** Valida el rango del borrador y, si es valido, lo aplica: consulta el reporte con esos filtros. */
    fun applyFilters() {
        if (!isValidRange(_from.value, _to.value)) {
            _rangeError.value = true
            return
        }
        _rangeError.value = false
        load(ReportFilters(_from.value, _to.value, _type.value, normalizeCategory(_category.value)))
    }

    /**
     * Quita tipo y categoria (borrador y aplicados) y vuelve a consultar. Las fechas se conservan: las del reporte
     * visible si ya habia uno, o las del borrador si todavia no.
     */
    fun clearFilters() {
        _type.value = null
        _category.value = ""
        val applied = _applied.value ?: return applyFilters()
        _rangeError.value = false
        load(applied.copy(type = null, category = null))
    }

    private fun load(filters: ReportFilters) {
        // Mismos filtros y ya hay una consulta en curso: seria una solicitud duplicada
        if (filters == _applied.value && _state.value.isLoading) return
        generation++
        val mine = generation
        loadJob?.cancel()
        exportJob?.cancel()
        _export.value = UIState()
        _exportedFileName.value = null
        _applied.value = filters
        _state.value = UIState(isLoading = true)
        loadJob = viewModelScope.launch {
            val result = repository.getReport(ownerId, filters.from, filters.to, filters.type, filters.category)
            if (mine != generation) return@launch
            _state.value = when (result) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo generar el reporte")
            }
        }
    }

    // US39: exportacion en CSV. data = archivo listo para compartir; message = aviso (por ejemplo, sin informacion)
    private val _export = mutableStateOf(UIState<ExportedReport>())
    val exportState: State<UIState<ExportedReport>> get() = _export

    // Nombre del ultimo archivo que se entrego para compartir; null si no hubo o se aplicaron otros filtros
    private val _exportedFileName = mutableStateOf<String?>(null)
    val exportedFileName: State<String?> get() = _exportedFileName

    private var exportJob: Job? = null

    /** Exporta con EXACTAMENTE los filtros aplicados (no con el borrador): el CSV es el del reporte que se ve. */
    fun exportReport() {
        if (_export.value.isLoading) return
        val filters = _applied.value ?: return
        val mine = generation
        _exportedFileName.value = null
        _export.value = UIState(isLoading = true)
        exportJob = viewModelScope.launch {
            val result = repository.exportReport(ownerId, filters.from, filters.to, filters.type, filters.category)
            if (mine != generation) return@launch
            _export.value = when (result) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo exportar el reporte")
            }
        }
    }

    /** La pantalla ya compartio el archivo: se limpia para no volver a abrirlo al recomponer. */
    fun consumeExport() {
        _exportedFileName.value = _export.value.data?.fileName
        _export.value = UIState()
    }
}
