package com.jayra.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.jayra.app.data.OperationType
import com.jayra.app.data.handleFirestoreError
import com.jayra.app.di.AppModule
import com.jayra.app.model.NavItem
import com.jayra.app.service.MyFirebaseMessagingService
import com.jayra.app.ui.main.JayraBottomNavigationBar
import com.jayra.app.ui.main.MainScreen
import com.jayra.app.ui.theme.JayraTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private var pendingChatId by mutableStateOf<String?>(null)
    private var pendingCallParticipantId by mutableStateOf<String?>(null)
    private var pendingCallType by mutableStateOf("voice")
    private var pendingOpenGlobalSearch by mutableStateOf(false)
    private var cachedFcmToken: String? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupFCM()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppModule.init(applicationContext)
        extractNotificationExtras(intent)
        observeAuthForTokenSync()
        requestNotificationPermissionAndSetupFCM()

        // Mandatory Edge-to-Edge display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Screen Security: Prevent screenshots and recents screen sniffing in release builds
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setContent {
            JayraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        initialChatId = pendingChatId,
                        initialCallParticipantId = pendingCallParticipantId,
                        initialCallType = pendingCallType,
                        initialOpenGlobalSearch = pendingOpenGlobalSearch,
                        onNotificationNavigationHandled = {
                            pendingChatId = null
                            pendingCallParticipantId = null
                            pendingOpenGlobalSearch = false
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationExtras(intent)
    }

    override fun onDestroy() {
        authStateListener?.let { listener ->
            try {
                Firebase.auth.removeAuthStateListener(listener)
            } catch (_: Exception) {
                // Ignore if FirebaseAuth is unavailable in preview/test environments
            }
        }
        super.onDestroy()
    }

    private fun extractNotificationExtras(incomingIntent: Intent?) {
        val chatId = incomingIntent?.getStringExtra(MyFirebaseMessagingService.EXTRA_CHAT_ID)
        val callParticipantId = incomingIntent?.getStringExtra(
            MyFirebaseMessagingService.EXTRA_CALL_PARTICIPANT_ID
        )
        val callType = incomingIntent?.getStringExtra(MyFirebaseMessagingService.EXTRA_CALL_TYPE)
            ?: "voice"

        if (!chatId.isNullOrBlank()) {
            pendingChatId = chatId
            incomingIntent.removeExtra(MyFirebaseMessagingService.EXTRA_CHAT_ID)
        }
        if (!callParticipantId.isNullOrBlank()) {
            pendingCallParticipantId = callParticipantId
            pendingCallType = callType
            incomingIntent.removeExtra(MyFirebaseMessagingService.EXTRA_CALL_PARTICIPANT_ID)
        }
        val shouldOpenGlobalSearch =
            incomingIntent?.action == Intent.ACTION_SEARCH ||
                incomingIntent?.getBooleanExtra(EXTRA_OPEN_GLOBAL_SEARCH, false) == true
        if (shouldOpenGlobalSearch) {
            pendingOpenGlobalSearch = true
            incomingIntent?.removeExtra(EXTRA_OPEN_GLOBAL_SEARCH)
        }
    }

    private fun requestNotificationPermissionAndSetupFCM() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (isGranted) {
                setupFCM()
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            setupFCM()
        }
    }

    private fun setupFCM() {
        createJayraNotificationChannels()

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        cachedFcmToken = token
                        saveTokenToFirestore(token)
                    }
                } else if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Gagal mengambil token FCM: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "FirebaseMessaging belum siap: ${e.message}")
            }
        }
    }

    private fun createJayraNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val chatChannel = NotificationChannel(
                MyFirebaseMessagingService.CHAT_CHANNEL_ID,
                "Chat Jayra",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.fcm_chat_channel_desc)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(chatChannel)

            MyFirebaseMessagingService.ensureCallNotificationChannel(this)
        }
    }

    private fun observeAuthForTokenSync() {
        try {
            val auth = Firebase.auth
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val token = cachedFcmToken
                if (firebaseAuth.currentUser != null && !token.isNullOrBlank()) {
                    saveTokenToFirestore(token)
                }
            }
            authStateListener = listener
            auth.addAuthStateListener(listener)
        } catch (_: Exception) {
            // FirebaseAuth not initialized in headless preview/unit test
        }
    }

    private fun saveTokenToFirestore(token: String) {
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
                    Log.e(TAG, "Gagal menyimpan fcmToken ke Firestore.")
                }
            }
    }

    companion object {
        private const val TAG = "MainActivityFCM"
        const val ROUTE_GLOBAL_SEARCH = "global_search"
        const val EXTRA_OPEN_GLOBAL_SEARCH = "extra_open_global_search"
    }
}

@Suppress("DEPRECATION")
@Preview(name = "Root Bottom Navigation Light", showBackground = true)
@Composable
private fun MainActivityRootLightPreview() {
    val sampleTabs = listOf(
        NavItem("chat_list", "Chat", Icons.Rounded.Chat, badgeCount = 2),
        NavItem("calls", "Panggilan", Icons.Rounded.Call),
        NavItem("music_home", "Musik", Icons.Rounded.MusicNote),
        NavItem("settings", "Pengaturan", Icons.Rounded.Settings)
    )
    JayraTheme(darkTheme = false) {
        Surface {
            JayraBottomNavigationBar(
                items = sampleTabs,
                currentRoute = "chat_list",
                onItemClick = {}
            )
        }
    }
}

@Suppress("DEPRECATION")
@Preview(name = "Root Bottom Navigation Dark", showBackground = true)
@Composable
private fun MainActivityRootDarkPreview() {
    val sampleTabs = listOf(
        NavItem("chat_list", "Chat", Icons.Rounded.Chat, badgeCount = 5),
        NavItem("calls", "Panggilan", Icons.Rounded.Call),
        NavItem("music_home", "Musik", Icons.Rounded.MusicNote),
        NavItem("settings", "Pengaturan", Icons.Rounded.Settings)
    )
    JayraTheme(darkTheme = true) {
        Surface {
            JayraBottomNavigationBar(
                items = sampleTabs,
                currentRoute = "settings",
                onItemClick = {}
            )
        }
    }
}
