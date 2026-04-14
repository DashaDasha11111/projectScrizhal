package com.example.scrizhal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SharedPrefManager(this).saveFcmToken(token)
        // TODO: отправить токен на сервер (Firestore) при следующем подключении
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: return

        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        val type = message.data["type"] ?: ""

        val targetActivity: Class<*> = when (type) {
            NOTIF_TYPE_CLERIC -> ClericProfileActivity::class.java
            NOTIF_TYPE_WORKSHOP -> WorkshopMainActivity::class.java
            else -> MainActivity::class.java
        }

        showNotification(title, body, targetActivity)
    }

    private fun showNotification(title: String, body: String, targetActivity: Class<*>) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Скрижаль — уведомления",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о наградах, назначениях и заказах"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "scrizhal_notifications"

        // Типы уведомлений (должны совпадать с тем, что шлёт сервер в data-поле "type")
        const val NOTIF_TYPE_METROPOLITAN = "metropolitan"
        const val NOTIF_TYPE_CLERIC = "cleric"
        const val NOTIF_TYPE_WORKSHOP = "workshop"
    }
}
