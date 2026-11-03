package com.jayra.app.data

import android.content.Context
import android.widget.Toast
import com.jayra.app.model.Contact
import com.jayra.app.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactRepository(
    private val firestore: FirebaseFirestore? = com.jayra.app.di.AppModule.firestore,
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
) {
    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    // Backward compatibility contacts for ChatRoom and ContactInfo
    private val _contacts = MutableStateFlow<Map<String, Contact>>(
        mapOf(
            "pavel_durov" to Contact(
                id = "pavel_durov",
                name = "Pavel Durov",
                phone = "+1 (555) 739-0192",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                status = "online",
                isOnline = true,
                mediaCount = 184,
                starredCount = 19,
                isMuted = false,
                saveToCamera = true,
                disappearingEnabled = false
            ),
            "chat_1" to Contact(
                id = "chat_1",
                name = "Pavel Durov",
                phone = "+1 (555) 739-0192",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                status = "online",
                isOnline = true,
                mediaCount = 184,
                starredCount = 19,
                isMuted = false,
                saveToCamera = true,
                disappearingEnabled = false
            ),
            "chat_2" to Contact(
                id = "chat_2",
                name = "Android Developers Community",
                phone = "3,428 members, 219 online",
                avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400",
                status = "online",
                isOnline = true,
                mediaCount = 590,
                starredCount = 42,
                isMuted = true,
                saveToCamera = false,
                disappearingEnabled = false
            ),
            "chat_3" to Contact(
                id = "chat_3",
                name = "Elena Petrova",
                phone = "+44 7911 123456",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
                status = "last seen 15m ago",
                isOnline = false,
                mediaCount = 64,
                starredCount = 5,
                isMuted = false,
                saveToCamera = true,
                disappearingEnabled = true
            )
        )
    )

    // Full User Contacts list for ContactsScreen
    private val _userContacts = MutableStateFlow<List<User>>(
        listOf(
            User(
                id = "pavel_durov",
                name = "Pavel Durov",
                phoneNumber = "+1 555 739 0192",
                bio = "Building Jayra. Freedom and privacy.",
                username = "durov",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                isOnline = true,
                lastSeenFormatted = "online",
                isMuted = false
            ),
            User(
                id = "alex_rivera",
                name = "Alex Rivera",
                phoneNumber = "+62 812 3456 7890",
                bio = "Kotlin enthusiast & Android dev",
                username = "alexrivera",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
                isOnline = true,
                lastSeenFormatted = "online",
                isMuted = false
            ),
            User(
                id = "david_kim",
                name = "David Kim",
                phoneNumber = "+1 202 555 0147",
                bio = "Designing interfaces for the future",
                username = "davidkim",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
                isOnline = true,
                lastSeenFormatted = "online",
                isMuted = false
            ),
            User(
                id = "elena_p",
                name = "Elena Petrova",
                phoneNumber = "+44 7911 123456",
                bio = "Cybersecurity researcher & open web advocate",
                username = "elenap",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
                isOnline = false,
                lastSeen = System.currentTimeMillis() - 1000 * 60 * 15,
                lastSeenFormatted = "terlihat 15 menit lalu",
                isMuted = false
            ),
            User(
                id = "jessica_t",
                name = "Jessica Taylor",
                phoneNumber = "+1 415 555 2671",
                bio = "Coffee lover and full-stack developer",
                username = "jtaylor",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400",
                isOnline = false,
                lastSeen = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                lastSeenFormatted = "terlihat 2 jam lalu",
                isMuted = true
            ),
            User(
                id = "michael_v",
                name = "Michael Vance",
                phoneNumber = "+61 491 570 156",
                bio = "Photography & Mountain Hiking",
                username = "mikevance",
                avatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400",
                isOnline = false,
                lastSeen = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                lastSeenFormatted = "terlihat kemarin pada 18:30",
                isMuted = false
            ),
            User(
                id = "sarah_connor",
                name = "Sarah Connor",
                phoneNumber = "+1 312 555 0199",
                bio = "Tech explorer",
                username = "sconnor",
                avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400",
                isOnline = false,
                lastSeen = System.currentTimeMillis() - 1000 * 60 * 60 * 48,
                lastSeenFormatted = "terlihat 2 hari lalu",
                isMuted = false
            )
        )
    )

    fun getContacts(): Flow<List<User>> = callbackFlow {
        if (firestore != null) {
            val listener = firestore.collection("users")
                .whereArrayContains("contacts", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) {
                        trySend(_userContacts.value)
                        return@addSnapshotListener
                    }
                    val remote = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                    if (remote.isNotEmpty()) {
                        trySend(remote)
                    } else {
                        trySend(_userContacts.value)
                    }
                }
            awaitClose { listener.remove() }
        } else {
            val job = launch {
                _userContacts.collect { list -> trySend(list) }
            }
            awaitClose { job.cancel() }
        }
    }

    suspend fun addContact(user: User, context: Context? = null) {
        _userContacts.update { list ->
            listOf(user) + list.filter { it.id != user.id }
        }
        _contacts.update { map ->
            map + (user.id to Contact(
                id = user.id,
                name = user.name,
                phone = user.phoneNumber,
                avatarUrl = user.avatarUrl,
                status = user.lastSeenFormatted,
                isOnline = user.isOnline
            ))
        }
        context?.let {
            Toast.makeText(it, "Kontak ${user.name} berhasil ditambahkan", Toast.LENGTH_SHORT).show()
        }
        try {
            firestore?.collection("users")?.document(user.id)?.set(user)
        } catch (_: Exception) {}
    }

    suspend fun deleteContact(userId: String, context: Context? = null) {
        val contactName = _userContacts.value.find { it.id == userId }?.name ?: "Kontak"
        _userContacts.update { list -> list.filter { it.id != userId } }
        _contacts.update { map -> map - userId }
        context?.let {
            Toast.makeText(it, "$contactName telah dihapus", Toast.LENGTH_SHORT).show()
        }
        try {
            firestore?.collection("users")?.document(userId)?.delete()
        } catch (_: Exception) {}
    }

    suspend fun toggleMuteContact(userId: String) {
        _userContacts.update { list ->
            list.map { user ->
                if (user.id == userId) user.copy(isMuted = !user.isMuted) else user
            }
        }
        val isMuted = _userContacts.value.find { it.id == userId }?.isMuted ?: false
        toggleMute(userId, isMuted)
        try {
            firestore?.collection("users")?.document(userId)?.update("isMuted", isMuted)
        } catch (_: Exception) {}
    }

    suspend fun blockContact(userId: String, context: Context? = null) {
        _userContacts.update { list ->
            list.map { user ->
                if (user.id == userId) user.copy(blocked = true) else user
            }
        }
        context?.let {
            Toast.makeText(it, "Kontak telah diblokir", Toast.LENGTH_SHORT).show()
        }
        try {
            firestore?.collection("users")?.document(userId)?.update("blocked", true)
        } catch (_: Exception) {}
    }

    fun getContact(contactId: String): Flow<Map<String, Contact>> = _contacts.asStateFlow()

    fun getContactById(contactId: String): Contact {
        return _contacts.value[contactId] ?: Contact(
            id = contactId,
            name = "Contact",
            phone = "+1 (555) 000-0000",
            avatarUrl = "",
            status = "last seen recently",
            isOnline = false
        )
    }

    fun getUserById(userId: String): User {
        return _userContacts.value.find { it.id == userId } ?: when (userId) {
            "user_telegram_me" -> User(
                id = "user_telegram_me",
                name = "Alex Pratama (Anda)",
                username = "alexpratama",
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400",
                isOnline = true,
                lastSeenFormatted = "online"
            )
            else -> User(
                id = userId,
                name = "Pengguna $userId",
                username = userId,
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                isOnline = false,
                lastSeenFormatted = "terlihat baru-baru ini"
            )
        }
    }

    fun toggleMute(contactId: String, muted: Boolean) {
        _contacts.update { map ->
            val contact = map[contactId] ?: getContactById(contactId)
            map + (contactId to contact.copy(isMuted = muted))
        }
        try {
            firestore?.collection("users")?.document(contactId)?.update("muted", muted)
        } catch (_: Exception) {}
    }

    fun toggleSaveToCamera(contactId: String, enabled: Boolean) {
        _contacts.update { map ->
            val contact = map[contactId] ?: getContactById(contactId)
            map + (contactId to contact.copy(saveToCamera = enabled))
        }
        try {
            firestore?.collection("users")?.document(contactId)?.update("saveToCamera", enabled)
        } catch (_: Exception) {}
    }

    fun toggleDisappearing(contactId: String) {
        _contacts.update { map ->
            val contact = map[contactId] ?: getContactById(contactId)
            map + (contactId to contact.copy(disappearingEnabled = !contact.disappearingEnabled))
        }
    }
}
