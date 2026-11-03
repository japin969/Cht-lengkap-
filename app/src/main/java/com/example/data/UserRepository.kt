package com.example.data

import com.example.model.NotificationSettings
import com.example.model.PrivacySettings
import com.example.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserRepository(
    private val firestore: FirebaseFirestore? = com.example.di.AppModule.firestore,
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
) {
    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    private val _currentUser = MutableStateFlow(
        User(
            id = "user_telegram_me",
            name = "Alex Pratama",
            phoneNumber = "+62 812-3456-7890",
            bio = "Mobile Engineer & Jetpack Compose Enthusiast 🚀",
            username = "alexpratama",
            telegramId = "782910342",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400",
            disappearingDuration = "Mati",
            isAppLockEnabled = true,
            language = "Indonesia"
        )
    )

    private val _privacySettings = MutableStateFlow(
        PrivacySettings(
            isAppLockEnabled = true,
            phoneNumberPrivacy = "Semua Orang",
            lastSeenPrivacy = "Kontak Saya",
            profilePhotoPrivacy = "Semua Orang",
            disappearingDuration = "Mati",
            isTwoStepVerificationEnabled = true,
            activeDevicesCount = 3
        )
    )

    private val _notificationSettings = MutableStateFlow(
        NotificationSettings(
            isPrivateChatNotificationsEnabled = true,
            isGroupNotificationsEnabled = true,
            isChannelNotificationsEnabled = true,
            sound = "Default",
            vibrate = "Default",
            isPriorityMessageEnabled = true,
            isDeletedMessageNoticeEnabled = false,
            isMessagePreviewEnabled = true
        )
    )

    private val _appTheme = MutableStateFlow("System")

    fun getUserProfile(): Flow<User> = callbackFlow {
        if (firestore != null) {
            val listener = firestore.collection("users").document(currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(_currentUser.value)
                        return@addSnapshotListener
                    }
                    val remote = snapshot.toObject(User::class.java)?.copy(
                        id = snapshot.id,
                        isAppLockEnabled = _currentUser.value.isAppLockEnabled
                    )
                    if (remote != null) {
                        trySend(remote)
                    } else {
                        trySend(_currentUser.value)
                    }
                }
            awaitClose { listener.remove() }
        } else {
            val job = launch {
                _currentUser.collect { user ->
                    trySend(user)
                }
            }
            awaitClose { job.cancel() }
        }
    }

    fun getPrivacySettings(): Flow<PrivacySettings> = _privacySettings.asStateFlow()

    fun getNotificationSettings(): Flow<NotificationSettings> = _notificationSettings.asStateFlow()

    fun getAppTheme(): Flow<String> = _appTheme.asStateFlow()

    suspend fun updateProfile(name: String, bio: String, username: String) {
        _currentUser.update {
            it.copy(name = name, bio = bio, username = username)
        }
        try {
            firestore?.collection("users")?.document(currentUserId)?.update(
                mapOf(
                    "name" to name,
                    "bio" to bio,
                    "username" to username
                )
            )
        } catch (_: Exception) {}
    }

    suspend fun updateAvatar(photoUrl: String) {
        _currentUser.update { it.copy(avatarUrl = photoUrl) }
        try {
            firestore?.collection("users")?.document(currentUserId)?.update("avatarUrl", photoUrl)
        } catch (_: Exception) {}
    }

    fun setAppLock(enabled: Boolean) {
        _currentUser.update { it.copy(isAppLockEnabled = enabled) }
        _privacySettings.update { it.copy(isAppLockEnabled = enabled) }
    }

    fun updatePhoneNumberPrivacy(privacy: String) {
        _privacySettings.update { it.copy(phoneNumberPrivacy = privacy) }
    }

    fun updateLastSeenPrivacy(privacy: String) {
        _privacySettings.update { it.copy(lastSeenPrivacy = privacy) }
    }

    fun updateProfilePhotoPrivacy(privacy: String) {
        _privacySettings.update { it.copy(profilePhotoPrivacy = privacy) }
    }

    suspend fun updateDisappearingDuration(duration: String) {
        _currentUser.update { it.copy(disappearingDuration = duration) }
        _privacySettings.update { it.copy(disappearingDuration = duration) }
        try {
            firestore?.collection("users")?.document(currentUserId)?.update("disappearingDuration", duration)
        } catch (_: Exception) {}
    }

    fun updateLanguage(language: String) {
        _currentUser.update { it.copy(language = language) }
    }

    fun updateAppTheme(theme: String) {
        _appTheme.update { theme }
    }

    fun updatePrivateNotifications(enabled: Boolean) {
        _notificationSettings.update { it.copy(isPrivateChatNotificationsEnabled = enabled) }
    }

    fun updateGroupNotifications(enabled: Boolean) {
        _notificationSettings.update { it.copy(isGroupNotificationsEnabled = enabled) }
    }

    fun updateChannelNotifications(enabled: Boolean) {
        _notificationSettings.update { it.copy(isChannelNotificationsEnabled = enabled) }
    }

    fun updateSound(sound: String) {
        _notificationSettings.update { it.copy(sound = sound) }
    }

    fun updateVibrate(vibrate: String) {
        _notificationSettings.update { it.copy(vibrate = vibrate) }
    }

    fun updatePriorityMessage(enabled: Boolean) {
        _notificationSettings.update { it.copy(isPriorityMessageEnabled = enabled) }
    }

    fun updateDeletedNotice(enabled: Boolean) {
        _notificationSettings.update { it.copy(isDeletedMessageNoticeEnabled = enabled) }
    }

    fun updateMessagePreview(enabled: Boolean) {
        _notificationSettings.update { it.copy(isMessagePreviewEnabled = enabled) }
    }
}
