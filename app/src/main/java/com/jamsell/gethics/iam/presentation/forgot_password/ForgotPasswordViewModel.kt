package com.jamsell.gethics.iam.presentation.forgot_password

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.iam.data.repository.AuthRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state

    fun sendRecoveryCode(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = UIState(message = "Ingresa un correo electrónico válido")
            return
        }

        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            when (val result = repository.forgotPassword(email.trim())) {
                is Resource.Success -> {
                    _state.value = UIState(data = Unit)
                }
                is Resource.Error -> {
                    _state.value = UIState(message = result.message ?: "Ocurrió un error")
                }
            }
        }
    }
}