package com.jamsell.gethics.notifications.presentation.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.notifications.data.repository.NotificationRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

/** US22: activar o desactivar las notificaciones push. data = pushEnabled; message = error (data null -> no cargo). */
class NotificationSettingsViewModel(
    private val repository: NotificationRepository,
    private val userId: String
) : ViewModel() {

    private val _state = mutableStateOf(UIState<Boolean>())
    val state: State<UIState<Boolean>> get() = _state

    fun load() {
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val result = repository.getPreference(userId)) {
                is Resource.Success -> UIState(data = result.data?.pushEnabled)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar la preferencia")
            }
        }
    }

    /** Cambia el switch al instante; si el backend falla, vuelve al valor anterior y muestra el error. */
    fun setPushEnabled(enabled: Boolean) {
        val previous = _state.value.data
        _state.value = UIState(data = enabled)
        viewModelScope.launch {
            _state.value = when (val result = repository.setPreference(userId, enabled)) {
                is Resource.Success -> UIState(data = result.data?.pushEnabled ?: enabled)
                is Resource.Error -> UIState(data = previous, message = result.message ?: "No se pudo guardar el cambio")
            }
        }
    }
}
