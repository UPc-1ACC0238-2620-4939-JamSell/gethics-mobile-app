package com.jamsell.gethics.finance.presentation.finance_summary

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jamsell.gethics.finance.data.repository.FinanceRepository
import com.jamsell.gethics.shared.common.UIState

class FinanceSummaryViewModel(private val repository: FinanceRepository) : ViewModel() {

    // TODO: cambiar Unit por el tipo real (ej. List<Animal>) y agregar las funciones de la pantalla
    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state
}
