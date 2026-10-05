package com.jamsell.gethics.iam.presentation.edit_profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.jamsell.gethics.iam.data.UpdateProfileRequest
import com.jamsell.gethics.iam.data.UserProfileDto
import com.jamsell.gethics.iam.data.repository.UserRepository
import com.jamsell.gethics.shared.common.Constants
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _state = mutableStateOf(UIState<UserProfileDto>())
    val state: State<UIState<UserProfileDto>> get() = _state

    private val _isSaved = mutableStateOf(false)
    val isSaved: State<Boolean> get() = _isSaved

    fun loadProfile() {
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            when (val result = repository.getUserProfile(Constants.DEV_OWNER_ID)) {
                is Resource.Success -> _state.value = UIState(data = result.data)
                is Resource.Error -> _state.value = UIState(message = result.message ?: "Error de carga")
            }
        }
    }

    fun updateProfile(fullName: String, phone: String, address: String) {
        if (fullName.isBlank()) {
            _state.value = _state.value.copy(message = "El nombre no puede estar vacío")
            return
        }

        _state.value = _state.value.copy(isLoading = true, message = "")
        viewModelScope.launch {
            val request = UpdateProfileRequest(fullName.trim(), phone.trim(), address.trim())
            when (val result = repository.updateProfile(Constants.DEV_OWNER_ID, request)) {
                is Resource.Success -> {
                    _state.value = UIState(data = result.data)
                    _isSaved.value = true
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = result.message ?: "No se pudo actualizar"
                    )
                }
            }
        }
    }
}