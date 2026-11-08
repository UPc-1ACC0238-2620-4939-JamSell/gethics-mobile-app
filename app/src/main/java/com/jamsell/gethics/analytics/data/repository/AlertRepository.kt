package com.jamsell.gethics.analytics.data.repository

import com.jamsell.gethics.analytics.data.AlertListResponse
import com.jamsell.gethics.analytics.data.AlertService
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class AlertRepository(private val service: AlertService) {

    /** US21. Solo lectura: el backend no permite marcar como leida, borrar ni filtrar. */
    suspend fun getAlerts(ownerId: String): Resource<AlertListResponse> =
        try {
            val response = service.getAlerts(ownerId)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}
