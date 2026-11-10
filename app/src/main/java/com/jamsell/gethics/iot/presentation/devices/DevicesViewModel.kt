package com.jamsell.gethics.iot.presentation.devices

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.iot.data.DeviceResponse
import com.jamsell.gethics.iot.data.isValidDeviceCode
import com.jamsell.gethics.iot.data.repository.IotRepository
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.common.UIState
import kotlinx.coroutines.launch

/** US23: vincular un dispositivo IoT y listar los vinculados con su estado de conexion. */
class DevicesViewModel(
    private val repository: IotRepository,
    private val ownerId: String
) : ViewModel() {

    private val _state = mutableStateOf(UIState<List<DeviceResponse>>())
    val state: State<UIState<List<DeviceResponse>>> get() = _state

    private val _code = mutableStateOf("")
    val code: State<String> get() = _code

    // true = el codigo esta vacio o es muy largo (la pantalla muestra el aviso en el idioma actual)
    private val _codeError = mutableStateOf(false)
    val codeError: State<Boolean> get() = _codeError

    private val _isLinking = mutableStateOf(false)
    val isLinking: State<Boolean> get() = _isLinking

    // Error devuelto por el servidor al vincular (por ejemplo, codigo ya vinculado)
    private val _linkMessage = mutableStateOf("")
    val linkMessage: State<String> get() = _linkMessage

    init {
        load()
    }

    fun onCodeChange(value: String) {
        _code.value = value
        _codeError.value = false
        _linkMessage.value = ""
    }

    fun load() {
        _state.value = UIState(isLoading = true, data = _state.value.data)
        viewModelScope.launch {
            _state.value = when (val result = repository.getDevices(ownerId)) {
                is Resource.Success -> UIState(data = result.data)
                is Resource.Error -> UIState(message = result.message ?: "No se pudieron cargar los dispositivos")
            }
        }
    }

    fun link() {
        if (!isValidDeviceCode(_code.value)) {
            _codeError.value = true
            return
        }
        _isLinking.value = true
        _linkMessage.value = ""
        viewModelScope.launch {
            when (val result = repository.linkDevice(ownerId, _code.value)) {
                is Resource.Success -> {
                    _code.value = ""
                    load()
                }
                is Resource.Error -> _linkMessage.value = result.message ?: "No se pudo vincular el dispositivo"
            }
            _isLinking.value = false
        }
    }
}
