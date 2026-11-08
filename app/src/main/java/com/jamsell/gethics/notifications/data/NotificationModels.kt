package com.jamsell.gethics.notifications.data

/** NotificationPreferenceResource (US22): si el usuario tiene activas las notificaciones push. */
data class NotificationPreferenceResponse(
    val userId: String,
    val pushEnabled: Boolean
)

/** Body de SetNotificationPreferenceResource. */
data class SetNotificationPreferenceRequest(
    val pushEnabled: Boolean
)
