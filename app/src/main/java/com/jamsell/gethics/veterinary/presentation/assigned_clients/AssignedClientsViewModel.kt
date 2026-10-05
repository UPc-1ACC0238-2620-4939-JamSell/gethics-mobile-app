package com.jamsell.gethics.veterinary.presentation.assigned_clients

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.shared.common.Constants
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import com.jamsell.gethics.veterinary.data.AssignedClientsResponse
import com.jamsell.gethics.veterinary.data.repository.VeterinaryRepository
import kotlinx.coroutines.launch

class AssignedClientsViewModel(private val repository: VeterinaryRepository) : ViewModel() {

    private val _state = mutableStateOf(UIState<AssignedClientsResponse>())
    val state: State<UIState<AssignedClientsResponse>> get() = _state

    fun load() {
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val result = repository.getAssignedClients(Constants.DEV_VET_ID)) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar tus clientes")
            }
        }
    }
}