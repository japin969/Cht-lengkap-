package com.jayra.app.data

import android.content.Context
import com.jayra.app.JayraApp
import com.jayra.app.model.NotificationSettings
import com.jayra.app.model.PrivacySettings
import com.jayra.app.model.User
import com.jayra.app.security.EncryptionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserRepository(
    private val firestore: FirebaseFirestore? = com.jayra.app.di.AppModule.firestore,
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
) {
    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    private val _currentUser = MutableStateFlow(
        User(
            id = "",
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

    fun syncAuthenticatedUser(
        uid: String,
        email: String = "",
        username: String = "",
        name: String = "",
        phoneNumber: String = "",
        jayraId: String = ""
    ) {
        if (uid.isBlank()) return
        _currentUser.update { current ->
            val sameUser = current.id == uid
            val cleanUsername = username.trim().removePrefix("@").lowercase()
                .ifBlank { if (sameUser) current.username else "" }
                .ifBlank { if (email.isNotBlank()) deriveUsernameFromEmail(email) else "" }
            val cleanName = name.trim()
                .ifBlank { if (sameUser) current.name else "" }
                .ifBlank { cleanUsername }
            val cleanPhone = phoneNumber.trim()
                .ifBlank { if (sameUser) current.phoneNumber else "" }
            val cleanJayraId = jayraId.trim()
                .ifBlank { if (sameUser) current.telegramId else "" }
                .ifBlank { deriveJayraId(uid) }
            current.copy(
                id = uid,
                username = cleanUsername,
                name = cleanName,
                phoneNumber = cleanPhone,
                telegramId = cleanJayraId,
                bio = if (sameUser) current.bio else "",
                avatarUrl = if (sameUser) current.avatarUrl else ""
            )
        }
    }

    suspend fun hydrateSessionFromPrefs(context: Context) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val firebaseUser = auth?.currentUser
        val uid = firebaseUser?.uid
            ?: EncryptionHelper.getEncryptedPref(appCtx, KEY_SESSION_UID)
            ?: return@withContext
        if (uid.isBlank() || uid == "user_telegram_me") return@withContext
        val storedEmail = EncryptionHelper.getEncryptedPref(appCtx, KEY_SESSION_EMAIL).orEmpty()
        val email = firebaseUser?.email?.takeIf { it.isNotBlank() } ?: storedEmail
        val username = EncryptionHelper.getEncryptedPref(appCtx, KEY_SESSION_USERNAME).orEmpty()
        val name = EncryptionHelper.getEncryptedPref(appCtx, KEY_SESSION_NAME)
            ?: firebaseUser?.displayName.orEmpty()
        val phone = EncryptionHelper.getEncryptedPref(appCtx, KEY_SESSION_PHONE).orEmpty()
        val jayraId = EncryptionHelper.getEncryptedPref(appCtx, KEY_SESSION_JAYRA_ID).orEmpty()
        syncAuthenticatedUser(
            uid = uid,
            email = email,
            username = username,
            name = name,
            phoneNumber = phone,
            jayraId = jayraId
        )
    }

    fun clearAuthenticatedSession() {
        _currentUser.value = User(
            disappearingDuration = "Mati",
            isAppLockEnabled = true,
            language = "Indonesia"
        )
    }

    fun getUserProfile(): Flow<User> = callbackFlow {
        val firebaseUser = auth?.currentUser
        val targetUid = firebaseUser?.uid ?: currentUserId

        if (firebaseUser != null && _currentUser.value.id != firebaseUser.uid) {
            val fallbackEmail = firebaseUser.email.orEmpty()
            val fallbackUsername = if (fallbackEmail.isNotBlank()) deriveUsernameFromEmail(fallbackEmail) else ""
            val fallbackName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: fallbackUsername
            _currentUser.update {
                it.copy(
                    id = firebaseUser.uid,
                    username = fallbackUsername,
                    name = fallbackName,
                    telegramId = deriveJayraId(firebaseUser.uid)
                )
            }
        }

        val localJob = launch {
            _currentUser.collect { user ->
                trySend(user)
            }
        }

        val listener = if (firestore != null && targetUid != "user_telegram_me") {
            firestore.collection("users").document(targetUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return@addSnapshotListener
                    }
                    val remote = snapshot.toObject(User::class.java) ?: return@addSnapshotListener
                    _currentUser.update { cached ->
                        val sameUser = cached.id == snapshot.id
                        val resolvedUsername = remote.username.trim().removePrefix("@").lowercase()
                            .ifBlank { if (sameUser) cached.username else "" }
                            .ifBlank {
                                val email = firebaseUser?.email.orEmpty()
                                if (email.isNotBlank()) deriveUsernameFromEmail(email) else ""
                            }
                        val resolvedName = remote.name.trim()
                            .ifBlank { if (sameUser) cached.name else "" }
                            .ifBlank { firebaseUser?.displayName?.takeIf { it.isNotBlank() }.orEmpty() }
                            .ifBlank { resolvedUsername }
                        val resolvedPhone = remote.phoneNumber.trim()
                            .ifBlank { if (sameUser) cached.phoneNumber else "" }
                        val resolvedJayraId = remote.telegramId.trim()
                            .ifBlank { if (sameUser) cached.telegramId else "" }
                            .ifBlank { deriveJayraId(snapshot.id) }

                        remote.copy(
                            id = snapshot.id,
                            username = resolvedUsername,
                            name = resolvedName,
                            phoneNumber = resolvedPhone,
                            telegramId = resolvedJayraId,
                            isAppLockEnabled = cached.isAppLockEnabled,
                            language = remote.language.ifBlank { cached.language }
                        )
                    }
                }
        } else {
            null
        }

        awaitClose {
            localJob.cancel()
            listener?.remove()
        }
    }

    companion object {
        private const val KEY_SESSION_UID = "auth_session_uid"
        private const val KEY_SESSION_USERNAME = "auth_session_username"
        private const val KEY_SESSION_NAME = "auth_session_name"
        private const val KEY_SESSION_PHONE = "auth_session_phone"
        private const val KEY_SESSION_JAYRA_ID = "auth_session_jayra_id"
        private const val KEY_SESSION_EMAIL = "auth_session_email"

        fun deriveJayraId(uid: String): String {
            if (uid.isBlank() || uid == "user_telegram_me") return ""
            return try {
                val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray(Charsets.UTF_8))
                val raw = ((digest[0].toLong() and 0xFFL) shl 24) or
                    ((digest[1].toLong() and 0xFFL) shl 16) or
                    ((digest[2].toLong() and 0xFFL) shl 8) or
                    (digest[3].toLong() and 0xFFL)
                val nineDigits = (raw % 900_000_000L) + 100_000_000L
                nineDigits.toString()
            } catch (_: Exception) {
                val positive = (uid.hashCode().toLong() and 0x7FFFFFFFL) % 900_000_000L + 100_000_000L
                positive.toString()
            }
        }

        fun deriveUsernameFromEmail(email: String): String {
            val prefix = email.substringBefore("@").lowercase()
                .replace(Regex("[^a-z0-9_.]"), "")
                .take(32)
            return if (prefix.length >= 3) prefix else "jayra_${prefix.ifBlank { "user" }}".take(32)
        }
    }

    fun getPrivacySettings(): Flow<PrivacySettings> = _privacySettings.asStateFlow()

    fun getNotificationSettings(): Flow<NotificationSettings> = _notificationSettings.asStateFlow()

    fun getAppTheme(): Flow<String> = _appTheme.asStateFlow()

    suspend fun updateProfile(name: String, bio: String, username: String) {
        val cleanUsername = username.trim().removePrefix("@").lowercase().take(64)
        val cleanName = name.trim().ifBlank { cleanUsername }.take(100)
        val cleanBio = bio.trim().take(300)
        _currentUser.update {
            it.copy(name = cleanName, bio = cleanBio, username = cleanUsername)
        }
        JayraApp.appContextOrNull()?.let { ctx ->
            withContext(Dispatchers.IO) {
                if (cleanUsername.isNotEmpty()) {
                    EncryptionHelper.saveEncryptedPref(ctx, KEY_SESSION_USERNAME, cleanUsername)
                }
                if (cleanName.isNotEmpty()) {
                    EncryptionHelper.saveEncryptedPref(ctx, KEY_SESSION_NAME, cleanName)
                }
            }
        }
        if (currentUserId != "user_telegram_me") {
            try {
                firestore?.collection("users")?.document(currentUserId)?.update(
                    mapOf(
                        "name" to cleanName,
                        "bio" to cleanBio,
                        "username" to cleanUsername,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } catch (_: Exception) {}
        }
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
