package com.jamsell.gethics.notifications.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jamsell.gethics.R

class GethicsFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Aquí enviamos el token al backend cuando se genera o renueva
        registerTokenOnBackend(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extraer el título y mensaje
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Gethics"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Tienes una nueva notificación"

        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "gethics_push_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas y Notificaciones Gethics",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para alertas sanitarias y eventos de ganado/mascotas"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Reemplazar por el ícono de notificaciones si aplica
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun registerTokenOnBackend(token: String) {
        // Mock de registro del token en tu API / IAM / Notification Service
        println("FCM Token generado: $token")
    }
}