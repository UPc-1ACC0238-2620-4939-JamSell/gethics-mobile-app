package com.jamsell.gethics.iot.data.repository

import com.jamsell.gethics.iot.data.DeviceResponse
import com.jamsell.gethics.iot.data.IotService
import com.jamsell.gethics.iot.data.LinkDeviceRequest
import com.jamsell.gethics.iot.data.ReadingsPageResponse
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class IotRepository(private val service: IotService) {

    suspend fun linkDevice(ownerId: String, code: String): Resource<DeviceResponse> =
        try {
            val response = service.linkDevice(LinkDeviceRequest(ownerId, code.trim()))
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    suspend fun getDevices(ownerId: String): Resource<List<DeviceResponse>> =
        try {
            val response = service.getDevices(ownerId)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** US37. Una pagina de lecturas del dispositivo. */
    suspend fun getReadings(deviceId: String, page: Int, size: Int): Resource<ReadingsPageResponse> =
        try {
            val response = service.getReadings(deviceId, page, size)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}
