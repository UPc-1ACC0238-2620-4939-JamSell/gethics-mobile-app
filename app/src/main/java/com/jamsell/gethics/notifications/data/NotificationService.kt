package com.jamsell.gethics.notifications.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificationService {

    /** US22. Preferencia de notificaciones push del usuario (userId es un UUID). */
    @GET("api/v1/users/{userId}/notification-preference")
    suspend fun getPreference(@Path("userId") userId: String): Response<NotificationPreferenceResponse>

    /** US22. Activa o desactiva las notificaciones push. Responde la preferencia ya guardada. */
    @PUT("api/v1/users/{userId}/notification-preference")
    suspend fun setPreference(
        @Path("userId") userId: String,
        @Body request: SetNotificationPreferenceRequest
    ): Response<NotificationPreferenceResponse>
}
