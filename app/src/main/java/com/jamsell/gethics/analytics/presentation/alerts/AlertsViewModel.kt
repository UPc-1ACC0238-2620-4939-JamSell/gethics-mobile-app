package com.jamsell.gethics.analytics.presentation.alerts

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.analytics.data.AlertDto
import com.jamsell.gethics.analytics.data.repository.AlertRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * US21. [ownerId] llega desde la navegacion.
 * ownerId se obtiene desde la sesión activa mediante SessionStorage.
 */
class AlertsViewModel(
    private val repository: AlertRepository,
    private val ownerId: String
) : ViewModel() {

    // data = alertas en el orden del backend (vacia -> message es el empty state); data null -> message es el error.
    // message vacio = sin texto del backend: AlertsScreen muestra el fallback localizado (alerts_empty / alerts_load_error).
    private val _state = mutableStateOf(UIState<List<AlertDto>>())
    val state: State<UIState<List<AlertDto>>> get() = _state

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.value = UIState(isLoading = true)
        loadJob = viewModelScope.launch {
            _state.value = when (val result = repository.getAlerts(ownerId)) {
                is Resource.Success -> {
                    val response = result.data!!
                    UIState(data = response.alerts, message = if (response.alerts.isEmpty()) response.message.orEmpty() else "")
                }
                is Resource.Error -> UIState(message = result.message.orEmpty())
            }
        }
    }
}
