package com.jayra.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jayra.app.BuildConfig
import com.jayra.app.MainActivity
import com.jayra.app.R
import com.jayra.app.data.OperationType
import com.jayra.app.data.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val messageType = remoteMessage.data["type"] ?: return
        when (messageType) {
            TYPE_CHAT -> handleChatNotification(remoteMessage.data)
            TYPE_CALL -> handleIncomingCallNotification(remoteMessage.data)
        }
    }

    private fun handleChatNotification(data: Map<String, String>) {
        val chatId = data["chatId"]?.takeIf { it.isNotBlank() } ?: return
        val senderName = data["senderName"]?.takeIf { it.isNotBlank() }
            ?: getString(R.string.app_name)
        val messageText = data["messageText"] ?: ""

        ensureChatNotificationChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_CHAT_ID, chatId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .build()

        notifyIfPermitted(chatId.hashCode(), notification)
    }

    private fun handleIncomingCallNotification(data: Map<String, String>) {
        val callerId = data["callerId"]
            ?: data["senderId"]
            ?: data["chatId"]
            ?: "chat_1"
        val senderName = data["senderName"]?.takeIf { it.isNotBlank() }
            ?: getString(R.string.app_name)
        val callType = data["callType"] ?: "voice"

        ensureCallNotificationChannel(this)

        val callIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_CALL_PARTICIPANT_ID, callerId)
            putExtra(EXTRA_CALL_TYPE, callType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            callerId.hashCode(),
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.fcm_incoming_call_title))
            .setContentText("$senderName ingin memanggil Anda.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        notifyIfPermitted(callerId.hashCode(), notification)
    }

    private fun notifyIfPermitted(notificationId: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return

        val currentUserId = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return

        val databaseId = applicationContext.getString(R.string.firestore_database_id)
        val docRef = FirebaseFirestore.getInstance(databaseId)
            .collection("users")
            .document(currentUserId)

        docRef.update("fcmToken", token)
            .addOnFailureListener { error ->
                if (BuildConfig.DEBUG) {
                    handleFirestoreError(error, OperationType.UPDATE, docRef.path)
                } else {
                    Log.e(TAG, "Gagal memperbarui token notifikasi di Firestore.")
                }
            }
    }

    companion object {
        private const val TAG = "JayraFCMService"
        private const val TYPE_CHAT = "chat"
        private const val TYPE_CALL = "call"

        const val CHAT_CHANNEL_ID = "jayra_chat_channel"
        const val CALL_CHANNEL_ID = "jayra_call_channel"
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_CALL_PARTICIPANT_ID = "extra_call_participant_id"
        const val EXTRA_CALL_TYPE = "extra_call_type"

        fun ensureChatNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

        fun ensureCallNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CALL_CHANNEL_ID,
                    context.getString(R.string.fcm_call_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.fcm_call_channel_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
