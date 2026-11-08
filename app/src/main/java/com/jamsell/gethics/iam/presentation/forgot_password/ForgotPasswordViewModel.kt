package com.jamsell.gethics.iam.presentation.forgot_password

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.iam.data.repository.AuthRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(private val repository: AuthRepository) : ViewModel() {

    // data = mensaje del backend -> el enlace ya se pidio; la pantalla lo muestra y ofrece ir a "Ya tengo mi código"
    private val _state = mutableStateOf(UIState<String>())
    val state: State<UIState<String>> get() = _state

    /** Sin validacion local: el formato del correo lo valida el backend. */
    fun sendLink(email: String) {
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val result = repository.forgotPassword(email.trim())) {
                is Resource.Success -> UIState(data = result.data.orEmpty())
                is Resource.Error -> UIState(message = result.message ?: "No se pudo enviar el enlace")
            }
        }
    }
}
