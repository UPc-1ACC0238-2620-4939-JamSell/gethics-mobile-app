package com.jamsell.gethics.finance.presentation.finance_summary

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.finance.data.FinancialSummaryResponse
import com.jamsell.gethics.finance.data.repository.FinanceRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale

/** "S/ 1,234.50". Mismo formato en cualquier idioma del telefono. */
fun formatMoney(amount: Double): String = String.format(Locale.US, "S/ %,.2f", amount)

/** US16: resumen financiero por mes o de todo el historial. */
class FinanceSummaryViewModel(
    private val repository: FinanceRepository,
    private val ownerId: String
) : ViewModel() {

    private val _actionState = mutableStateOf<String?>(null)
    val actionState: State<String?> get() = _actionState

    // null = todo el historial
    private val _period = mutableStateOf<YearMonth?>(YearMonth.now())
    val period: State<YearMonth?> get() = _period

    // data = resumen; data null -> message es el error
    private val _state = mutableStateOf(UIState<FinancialSummaryResponse>())
    val state: State<UIState<FinancialSummaryResponse>> get() = _state

    private var loadJob: Job? = null

    fun previousMonth() = changePeriod { it.minusMonths(1) }

    fun nextMonth() = changePeriod { it.plusMonths(1) }

    /** Alterna entre "un mes" (el actual) y "todo el historial". */
    fun toggleAll() {
        _period.value = if (_period.value == null) YearMonth.now() else null
        load()
    }

    private fun changePeriod(change: (YearMonth) -> YearMonth) {
        val current = _period.value ?: return
        _period.value = change(current)
        load()
    }

    /** US31: Anula un movimiento financiero por su ID y recarga el resumen */
    fun voidMovement(movementId: Long) {
        viewModelScope.launch {
            _actionState.value = null
            try {
                // Llamamos al repositorio que sí maneja el Resource
                val result = repository.voidFinancialMovement(movementId)

                when (result) {
                    is Resource.Success -> {
                        _actionState.value = "Movimiento financiero anulado exitosamente"
                        // Recargamos el resumen financiero para que refleje el cambio de inmediato
                        load()
                    }
                    is Resource.Error -> {
                        _actionState.value = result.message ?: "Error al anular movimiento"
                    }
                }
            } catch (e: Exception) {
                _actionState.value = "Excepción: ${e.localizedMessage}"
            }
        }
    }

    fun load() {
        loadJob?.cancel() // evita que la respuesta de un mes anterior pise la del mes elegido
        val period = _period.value
        _state.value = UIState(isLoading = true)
        loadJob = viewModelScope.launch {
            _state.value = when (val result = repository.getSummary(ownerId, period?.atDay(1), period?.atEndOfMonth())) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar el resumen")
            }
        }
    }
}