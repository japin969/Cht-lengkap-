package com.example.util

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object LocalNotificationManager {

    const val CHAT_CHANNEL_ID = "jayra_chat_channel"
    const val EXTRA_CHAT_ID = "extra_chat_id"

    /**
     * Memeriksa apakah aplikasi Jayra saat ini berada di background (bukan foreground aktif).
     */
    fun isAppInBackground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        val runningProcesses = activityManager.runningAppProcesses ?: return true
        val currentPackage = context.packageName
        for (processInfo in runningProcesses) {
            if (processInfo.processName == currentPackage) {
                return processInfo.importance !=
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }
        }
        return true
    }

    /**
     * Memastikan NotificationChannel "jayra_chat_channel" terdaftar pada sistem Android O+.
     */
    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(CHAT_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHAT_CHANNEL_ID,
                    context.getString(R.string.fcm_chat_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.fcm_chat_channel_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Menampilkan notifikasi pesan lokal saat ada pesan baru masuk dari lawan bicara
     * ketika aplikasi sedang berada di background (tanpa memerlukan Cloud Functions berbayar).
     */
    fun showMessageNotification(
        context: Context,
        chatId: String,
        senderName: String,
        messageText: String,
        messageId: String = ""
    ) {
        if (chatId.isBlank() || messageText.isBlank()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        val openChatIntent = Intent(appContext, MainActivity::class.java).apply {
            putExtra(EXTRA_CHAT_ID, chatId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            chatId.hashCode(),
            openChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = senderName.ifBlank { appContext.getString(R.string.app_name) }
        val notification = NotificationCompat.Builder(appContext, CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (messageId.isNotBlank()) {
            (chatId + "_" + messageId).hashCode()
        } else {
            chatId.hashCode()
        }

        NotificationManagerCompat.from(appContext).notify(notificationId, notification)
    }
}
