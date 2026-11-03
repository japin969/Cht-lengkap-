package com.jayra.app.data

import com.jayra.app.model.Channel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChannelRepository(
    private val firestore: FirebaseFirestore? = com.jayra.app.di.AppModule.firestore
) {
    private val _localChannels = MutableStateFlow<Map<String, Channel>>(
        mapOf(
            "chat_4" to Channel(
                id = "chat_4",
                name = "Jayra News",
                avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400",
                subscriberCount = 894200,
                description = "Berita resmi seputar Jayra. Dapatkan informasi pembaruan fitur terbaru, tips keamanan, bot platform, stiker interaktif, dan rilis arsitektur modern Jayra untuk Android.",
                mediaCount = 824,
                starredCount = 45,
                isMuted = true,
                isCustomNotification = false,
                disappearingDuration = "Mati",
                isSignaturesEnabled = true,
                isAdmin = false,
                creatorId = "telegram_official",
                subscribers = listOf("telegram_official", "user_telegram_me")
            ),
            "durov" to Channel(
                id = "durov",
                name = "Durov's Channel",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                subscriberCount = 2500000,
                description = "Thoughts from the Product Manager and Founder of Jayra.",
                mediaCount = 312,
                starredCount = 88,
                isMuted = false,
                isCustomNotification = true,
                disappearingDuration = "Mati",
                isSignaturesEnabled = false,
                isAdmin = false,
                creatorId = "pavel_durov"
            )
        )
    )

    fun getChannel(channelId: String): Flow<Channel> = callbackFlow {
        if (firestore != null) {
            val listener = firestore.collection("channels").document(channelId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        val fallback = _localChannels.value[channelId] ?: getFallbackChannel(channelId)
                        trySend(fallback)
                        return@addSnapshotListener
                    }
                    val remote = snapshot.toObject(Channel::class.java)?.copy(id = snapshot.id)
                    if (remote != null) {
                        trySend(remote)
                    } else {
                        trySend(_localChannels.value[channelId] ?: getFallbackChannel(channelId))
                    }
                }
            awaitClose { listener.remove() }
        } else {
            val job = launch {
                _localChannels.collect { map ->
                    trySend(map[channelId] ?: getFallbackChannel(channelId))
                }
            }
            awaitClose { job.cancel() }
        }
    }

    fun getAllChannels(): Flow<List<Channel>> = callbackFlow {
        val job = launch {
            _localChannels.collect { map ->
                trySend(map.values.toList())
            }
        }
        awaitClose { job.cancel() }
    }

    private fun getFallbackChannel(channelId: String): Channel {
        return Channel(
            id = channelId,
            name = "Channel Jayra",
            avatarUrl = "",
            subscriberCount = 100,
            description = "Deskripsi channel resmi.",
            isAdmin = false
        )
    }

    suspend fun updateMute(channelId: String, muted: Boolean) {
        _localChannels.update { map ->
            val c = map[channelId] ?: getFallbackChannel(channelId)
            map + (channelId to c.copy(isMuted = muted))
        }
        try {
            firestore?.collection("channels")?.document(channelId)?.update("muted", muted)
        } catch (_: Exception) {}
    }

    suspend fun updateCustomNotification(channelId: String, enabled: Boolean) {
        _localChannels.update { map ->
            val c = map[channelId] ?: getFallbackChannel(channelId)
            map + (channelId to c.copy(isCustomNotification = enabled))
        }
        try {
            firestore?.collection("channels")?.document(channelId)?.update("customNotification", enabled)
        } catch (_: Exception) {}
    }

    suspend fun updateSignatures(channelId: String, enabled: Boolean) {
        _localChannels.update { map ->
            val c = map[channelId] ?: getFallbackChannel(channelId)
            map + (channelId to c.copy(isSignaturesEnabled = enabled))
        }
        try {
            firestore?.collection("channels")?.document(channelId)?.update("signaturesEnabled", enabled)
        } catch (_: Exception) {}
    }

    suspend fun leaveChannel(channelId: String, userId: String) {
        _localChannels.update { map ->
            val c = map[channelId] ?: return@update map
            map + (channelId to c.copy(
                subscriberCount = (c.subscriberCount - 1).coerceAtLeast(0),
                subscribers = c.subscribers - userId
            ))
        }
        try {
            firestore?.collection("channels")?.document(channelId)
                ?.update("subscribers", FieldValue.arrayRemove(userId))
        } catch (_: Exception) {}
    }

    suspend fun deleteChannel(channelId: String) {
        _localChannels.update { map -> map - channelId }
        try {
            firestore?.collection("channels")?.document(channelId)?.delete()
        } catch (_: Exception) {}
    }
}
