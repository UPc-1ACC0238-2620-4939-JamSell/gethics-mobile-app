package com.jamsell.gethics.iam.presentation.sign_in

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.iam.data.repository.AuthRepository
import com.jamsell.gethics.iam.domain.model.Role
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

class SignInViewModel(private val repository: AuthRepository) : ViewModel() {

    // data = rol del usuario autenticado -> la pantalla navega a su panel principal
    private val _state = mutableStateOf(UIState<Role>())
    val state: State<UIState<Role>> get() = _state

    /** Sin validacion local: campos vacios los rechaza el backend con su propio mensaje. */
    fun signIn(email: String, password: String) {
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val result = repository.signIn(email, password)) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo iniciar sesión")
            }
        }
    }
}
