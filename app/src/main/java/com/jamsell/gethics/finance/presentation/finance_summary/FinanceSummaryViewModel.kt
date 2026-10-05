package com.jamsell.gethics.finance.presentation.finance_summary

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.finance.data.FinancialSummaryResponse
import com.jamsell.gethics.finance.data.repository.FinanceRepository
import com.jamsell.gethics.shared.common.Constants
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

class FinanceSummaryViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = mutableStateOf(UIState<FinancialSummaryResponse>())
    val state: State<UIState<FinancialSummaryResponse>> get() = _state

    fun load() {
        _state.value = UIState(isLoading = true, data = _state.value.data)
        viewModelScope.launch {
            _state.value = when (val result = repository.getSummary(Constants.DEV_OWNER_ID)) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar el balance")
            }
        }
    }
}