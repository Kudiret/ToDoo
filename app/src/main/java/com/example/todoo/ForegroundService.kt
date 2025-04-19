package com.example.ToDoo

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {

    private val CHANNEL_ID = "todo_foreground_channel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val textIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_TEXT"
        }
        val textPendingIntent = PendingIntent.getBroadcast(
            this, 0, textIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val voiceIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_VOICE"
        }
        val voicePendingIntent = PendingIntent.getBroadcast(
            this, 1, voiceIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ToDoo")
            .setContentText("Добавьте новую задачу")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true) // важно! уведомление нельзя свайпнуть
            .addAction(R.drawable.ic_edit, "Текстом", textPendingIntent)
            .addAction(R.drawable.ic_mic, "Голосом", voicePendingIntent)
            .build()

        startForeground(1, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ToDoo Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
