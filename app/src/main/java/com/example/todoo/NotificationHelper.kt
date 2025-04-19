package com.example.ToDoo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "todo_channel"
    private const val NOTIFICATION_ID = 1

    fun showNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Создаем канал для уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ToDoo Задачи",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Интент для кнопки "Текстом"
        val textIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_TEXT"
        }
        val textPendingIntent = PendingIntent.getBroadcast(
            context, 0, textIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Интент для кнопки "Голосом"
        val voiceIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_VOICE"
        }
        val voicePendingIntent = PendingIntent.getBroadcast(
            context, 1, voiceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Уведомление
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Добавь иконку в res/drawable
            .setContentTitle("ToDoo")
            .setContentText("Добавьте новую задачу")
            .addAction(R.drawable.ic_edit, "Текстом", textPendingIntent)
            .addAction(R.drawable.ic_mic, "Голосом", voicePendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
