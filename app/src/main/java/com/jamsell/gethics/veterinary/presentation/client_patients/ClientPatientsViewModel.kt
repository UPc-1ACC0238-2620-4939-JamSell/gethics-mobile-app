package com.jamsell.gethics.veterinary.presentation.client_patients

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.shared.common.Constants
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import com.jamsell.gethics.veterinary.data.ClientPatientsResponse
import com.jamsell.gethics.veterinary.data.repository.VeterinaryRepository
import kotlinx.coroutines.launch

class ClientPatientsViewModel(private val repository: VeterinaryRepository) : ViewModel() {

    private val _state = mutableStateOf(UIState<ClientPatientsResponse>())
    val state: State<UIState<ClientPatientsResponse>> get() = _state

    fun load(clientId: String) {
        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            _state.value = when (val result = repository.getClientPatients(clientId, Constants.DEV_VET_ID)) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo cargar los pacientes")
            }
        }
    }
}