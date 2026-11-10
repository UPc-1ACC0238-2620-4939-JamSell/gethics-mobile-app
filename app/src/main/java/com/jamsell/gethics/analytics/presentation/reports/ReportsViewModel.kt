package com.jamsell.gethics.analytics.presentation.reports

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.analytics.data.ExportedReport
import com.jamsell.gethics.analytics.data.LivestockReportResponse
import com.jamsell.gethics.analytics.data.repository.AnalyticsRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

/** El rango es valido si la fecha inicial no es posterior a la final. */
fun isValidRange(from: LocalDate, to: LocalDate): Boolean = !from.isAfter(to)

/** US20: reporte del ganado por rango de fechas (por defecto, el mes actual). */
class ReportsViewModel(
    private val repository: AnalyticsRepository,
    private val ownerId: String
) : ViewModel() {

    private val _from = mutableStateOf(LocalDate.now().withDayOfMonth(1))
    val from: State<LocalDate> get() = _from

    private val _to = mutableStateOf(LocalDate.now())
    val to: State<LocalDate> get() = _to

    // true = la fecha inicial es posterior a la final (la pantalla muestra el aviso en el idioma actual)
    private val _rangeError = mutableStateOf(false)
    val rangeError: State<Boolean> get() = _rangeError

    // data = reporte; data null -> message es el error
    private val _state = mutableStateOf(UIState<LivestockReportResponse>())
    val state: State<UIState<LivestockReportResponse>> get() = _state

    private var loadJob: Job? = null

    fun setFrom(date: LocalDate) {
        _from.value = date
    }

    fun setTo(date: LocalDate) {
        _to.value = date
    }

    fun generate() {
        if (!isValidRange(_from.value, _to.value)) {
            _rangeError.value = true
            return
        }
        _rangeError.value = false
        loadJob?.cancel()
        val from = _from.value
        val to = _to.value
        _state.value = UIState(isLoading = true)
        loadJob = viewModelScope.launch {
            _state.value = when (val result = repository.getReport(ownerId, from, to)) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo generar el reporte")
            }
        }
    }

    // US39: exportacion en CSV. data = archivo listo para compartir; message = aviso (por ejemplo, sin informacion)
    private val _export = mutableStateOf(UIState<ExportedReport>())
    val exportState: State<UIState<ExportedReport>> get() = _export

    fun exportReport() {
        if (_export.value.isLoading) return
        if (!isValidRange(_from.value, _to.value)) {
            _rangeError.value = true
            return
        }
        _rangeError.value = false
        val from = _from.value
        val to = _to.value
        _export.value = UIState(isLoading = true)
        viewModelScope.launch {
            _export.value = when (val result = repository.exportReport(ownerId, from, to)) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo exportar el reporte")
            }
        }
    }

    /** La pantalla ya compartio el archivo: se limpia para no volver a abrirlo al recomponer. */
    fun consumeExport() {
        _export.value = UIState()
    }
}
