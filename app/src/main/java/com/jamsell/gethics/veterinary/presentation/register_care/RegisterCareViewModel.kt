package com.jamsell.gethics.veterinary.presentation.register_care

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.shared.common.Constants
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import com.jamsell.gethics.veterinary.data.RegisterCareRequest
import com.jamsell.gethics.veterinary.data.repository.VeterinaryRepository
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.launch

class RegisterCareViewModel(private val repository: VeterinaryRepository) : ViewModel() {

    private val _state = mutableStateOf(UIState<Unit>())
    val state: State<UIState<Unit>> get() = _state

    /** requestId lo genera la pantalla una sola vez: reintentar con el mismo id no duplica la atencion. */
    fun register(patientId: String, requestId: String, diagnosis: String, treatment: String, nextControl: String) {
        if (diagnosis.isBlank() || treatment.isBlank()) {
            _state.value = UIState(message = "Ingresa el diagnóstico y el tratamiento")
            return
        }
        val nextDate = nextControl.trim().takeIf { it.isNotEmpty() }?.let {
            try {
                LocalDate.parse(it).toString()
            } catch (e: DateTimeParseException) {
                _state.value = UIState(message = "Fecha inválida. Usa el formato AAAA-MM-DD")
                return
            }
        }

        _state.value = UIState(isLoading = true)
        viewModelScope.launch {
            val request = RegisterCareRequest(
                clientRequestId = requestId,
                veterinarianId = Constants.DEV_VET_ID, // TODO: usuario real cuando iam devuelva UUID
                diagnosis = diagnosis.trim(),
                treatment = treatment.trim(),
                nextControlDate = nextDate,
                occurredAt = null
            )
            _state.value = when (val result = repository.registerCare(patientId, request)) {
                is Resource.Success -> UIState(data = Unit)
                is Resource.Error -> UIState(message = result.message ?: "No se pudo registrar la atención")
            }
        }
    }
}