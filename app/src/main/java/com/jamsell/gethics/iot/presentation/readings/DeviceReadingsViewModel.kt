package com.jamsell.gethics.iot.presentation.readings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamsell.gethics.iot.data.SensorReadingResponse
import com.jamsell.gethics.iot.data.repository.IotRepository
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.launch

const val READINGS_PAGE_SIZE = 20

/** US37: lecturas recientes de un dispositivo, paginadas (se cargan mas al llegar al final de la lista). */
class DeviceReadingsViewModel(
    private val repository: IotRepository,
    private val deviceId: String
) : ViewModel() {

    private val _readings = mutableStateOf<List<SensorReadingResponse>>(emptyList())
    val readings: State<List<SensorReadingResponse>> get() = _readings

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> get() = _isLoading

    // true cuando ya se cargo al menos una pagina; permite distinguir "cargando" de "sin lecturas"
    private val _loadedOnce = mutableStateOf(false)
    val loadedOnce: State<Boolean> get() = _loadedOnce

    private val _hasMore = mutableStateOf(false)
    val hasMore: State<Boolean> get() = _hasMore

    private val _message = mutableStateOf("")
    val message: State<String> get() = _message

    private var nextPage = 0

    init {
        loadMore()
    }

    fun loadMore() {
        if (_isLoading.value) return
        _isLoading.value = true
        _message.value = ""
        viewModelScope.launch {
            when (val result = repository.getReadings(deviceId, nextPage, READINGS_PAGE_SIZE)) {
                is Resource.Success -> {
                    val page = result.data
                    if (page != null) {
                        _readings.value = _readings.value + page.content
                        _hasMore.value = page.hasMore
                        nextPage = page.page + 1
                        _loadedOnce.value = true
                    }
                }
                is Resource.Error -> {
                    _hasMore.value = false
                    _message.value = result.message ?: "No se pudieron cargar las lecturas"
                }
            }
            _isLoading.value = false
        }
    }

    fun retry() {
        _hasMore.value = true
        loadMore()
    }
}
