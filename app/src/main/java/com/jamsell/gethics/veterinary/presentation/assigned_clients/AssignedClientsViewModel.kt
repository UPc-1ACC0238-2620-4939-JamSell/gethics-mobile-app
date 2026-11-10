package com.jamsell.gethics.veterinary.presentation.assigned_clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Asegúrate de que esta ruta coincida exactamente con la ubicación de tu archivo VeterinaryService
import com.jamsell.gethics.veterinary.data.VeterinaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AssignedClientsViewModel(
    private val veterinaryService: VeterinaryService
) : ViewModel() {

    private val _uiState = MutableStateFlow<String?>(null)
    val uiState: StateFlow<String?> = _uiState

    fun unassignClient(assignmentId: UUID) {
        viewModelScope.launch {
            try {
                val response = veterinaryService.unassignClient(assignmentId)
                if (response.isSuccessful) {
                    _uiState.value = "Cliente desvinculado exitosamente"
                } else {
                    _uiState.value = "Error al desvincular: ${response.code()}"
                }
            } catch (e: Exception) {
                _uiState.value = "Excepción: ${e.localizedMessage}"
            }
        }
    }
}

