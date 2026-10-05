package com.jamsell.gethics.iam.presentation.sign_up

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.iam.data.repository.AuthRepository
import com.jamsell.gethics.iam.domain.model.Role
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

class SignUpViewModel(private val repository: AuthRepository) : ViewModel() {

    // data = true -> cuenta creada (la pantalla vuelve al login)
    private val _state = mutableStateOf(UIState<Boolean>())
    val state: State<UIState<Boolean>> get() = _state

    /**
     * Sin validacion local: lo valida el backend. Sin rol elegido se envia "" y el backend responde 400.
     * role se envia como "GANADERO" o "VETERINARIO".
     */
    fun signUp(name: String, email: String, password: String, role: Role?) {
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val result = repository.signUp(name, email, password, role?.name.orEmpty())) {
                is Resource.Success -> UIState(data = true)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo crear la cuenta")
            }
        }
    }
}
