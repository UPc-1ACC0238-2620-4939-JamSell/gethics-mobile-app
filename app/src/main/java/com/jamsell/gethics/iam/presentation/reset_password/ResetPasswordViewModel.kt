package com.jamsell.gethics.iam.presentation.reset_password

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.iam.data.repository.AuthRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

class ResetPasswordViewModel(private val repository: AuthRepository) : ViewModel() {

    // data = true -> contraseña cambiada (la pantalla vuelve al login)
    private val _state = mutableStateOf(UIState<Boolean>())
    val state: State<UIState<Boolean>> get() = _state

    /** La coincidencia con la confirmacion se revisa en la pantalla; token y longitud los valida el backend. */
    fun resetPassword(token: String, newPassword: String) {
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val result = repository.resetPassword(token, newPassword)) {
                is Resource.Success -> UIState(data = true)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cambiar la contraseña")
            }
        }
    }
}
