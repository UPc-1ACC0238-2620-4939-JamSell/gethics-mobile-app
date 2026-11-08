package com.jamsell.gethics.iam.presentation.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.iam.data.UserResponse
import com.jamsell.gethics.iam.data.repository.UserRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: UserRepository) : ViewModel() {

    // data = perfil actual; message = error de carga o de guardado
    private val _state = mutableStateOf(UIState<UserResponse>(isLoading = true))
    val state: State<UIState<UserResponse>> get() = _state

    // true tras guardar con exito; la pantalla lo muestra y lo limpia al volver a editar
    private val _saved = mutableStateOf(false)
    val saved: State<Boolean> get() = _saved

    init {
        load()
    }

    fun load() {
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val result = repository.getProfile()) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar tu perfil")
            }
        }
    }

    fun clearSaved() {
        _saved.value = false
    }

    /** Si falla, se conserva el perfil que ya se mostraba y solo se agrega el mensaje de error. */
    fun save(name: String, phone: String) {
        val current = _state.value.data
        _saved.value = false
        _state.value = UIState(isLoading = true, data = current)
        viewModelScope.launch {
            when (val result = repository.updateProfile(name.trim(), phone)) {
                is Resource.Success -> {
                    _state.value = UIState(data = result.data)
                    _saved.value = true
                }
                is Resource.Error -> {
                    _state.value = UIState(data = current, message = result.message ?: "No se pudo guardar el perfil")
                }
            }
        }
    }
}
