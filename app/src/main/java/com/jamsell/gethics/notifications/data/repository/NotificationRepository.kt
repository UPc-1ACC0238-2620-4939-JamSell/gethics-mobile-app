package com.jamsell.gethics.notifications.data.repository

import com.jamsell.gethics.notifications.data.NotificationPreferenceResponse
import com.jamsell.gethics.notifications.data.NotificationService
import com.jamsell.gethics.notifications.data.SetNotificationPreferenceRequest
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class NotificationRepository(private val service: NotificationService) {

    suspend fun getPreference(userId: String): Resource<NotificationPreferenceResponse> =
        try {
            val response = service.getPreference(userId)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    suspend fun setPreference(userId: String, pushEnabled: Boolean): Resource<NotificationPreferenceResponse> =
        try {
            val response = service.setPreference(userId, SetNotificationPreferenceRequest(pushEnabled))
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}
