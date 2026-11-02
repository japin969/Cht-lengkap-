package com.example.data

import com.example.model.Chat
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.model.Reaction
import com.example.security.EncryptionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatRepository(
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null },
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
) {
    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    // High fidelity seed data for offline / instant rendering
    private val _localChats = MutableStateFlow<List<Chat>>(
        listOf(
            Chat(
                id = "chat_1",
                name = "Pavel Durov",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                lastMessage = "Telegram now supports voice and video calls with end-to-end encryption.",
                timestamp = "10:45 AM",
                unreadCount = 2,
                isPinned = true,
                isMuted = false,
                isRead = false,
                isGroup = false,
                participants = listOf("user_telegram_me", "pavel_durov")
            ),
            Chat(
                id = "chat_2",
                name = "Android Developers Community",
                avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200",
                lastMessage = "Jetpack Compose 1.8 has released exciting new performance improvements!",
                timestamp = "09:30 AM",
                unreadCount = 14,
                isPinned = true,
                isMuted = true,
                isRead = false,
                isGroup = true,
                participants = listOf("user_telegram_me", "dev_1", "dev_2")
            ),
            Chat(
                id = "chat_3",
                name = "Elena Petrova",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                lastMessage = "Are we still meeting at the cafe this afternoon?",
                timestamp = "Yesterday",
                unreadCount = 0,
                isPinned = false,
                isMuted = false,
                isRead = true,
                isGroup = false,
                participants = listOf("user_telegram_me", "elena_p")
            ),
            Chat(
                id = "chat_4",
                name = "Telegram News",
                avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200",
                lastMessage = "Major Update: Introducing Chat Filters, Fast Channel Sharing, and Folders.",
                timestamp = "Yesterday",
                unreadCount = 0,
                isPinned = false,
                isMuted = true,
                isRead = true,
                isChannel = true,
                participants = listOf("user_telegram_me", "tg_news")
            ),
            Chat(
                id = "chat_5",
                name = "Design System Team",
                avatarUrl = "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=200",
                lastMessage = "All Material 3 color palettes and icons have been reviewed.",
                timestamp = "Oct 30",
                unreadCount = 0,
                isPinned = false,
                isMuted = false,
                isRead = true,
                isGroup = true,
                participants = listOf("user_telegram_me", "designer_1")
            ),
            Chat(
                id = "chat_6",
                name = "Michael Vance",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                lastMessage = "Check out the repo when you get a chance 👍",
                timestamp = "Oct 28",
                unreadCount = 0,
                isPinned = false,
                isMuted = false,
                isRead = true,
                isGroup = false,
                participants = listOf("user_telegram_me", "michael_v")
            )
        )
    )

    private val _messagesByChat = MutableStateFlow<Map<String, List<Message>>>(
        mapOf(
            "chat_1" to listOf(
                Message(
                    id = "msg_1",
                    chatId = "chat_1",
                    senderId = "pavel_durov",
                    senderName = "Pavel Durov",
                    text = "Welcome to the reconstructed Telegram Android client!",
                    timestamp = "10:40 AM",
                    dateGroup = "Today",
                    status = MessageStatus.READ,
                    reactions = listOf(Reaction(emoji = "🔥", count = 5, userReacted = true))
                ),
                Message(
                    id = "msg_2",
                    chatId = "chat_1",
                    senderId = "user_telegram_me",
                    senderName = "Me",
                    text = "Thanks! The animations and UI fidelity are super smooth.",
                    timestamp = "10:42 AM",
                    dateGroup = "Today",
                    status = MessageStatus.READ
                ),
                Message(
                    id = "msg_3",
                    chatId = "chat_1",
                    senderId = "pavel_durov",
                    senderName = "Pavel Durov",
                    text = "Telegram now supports voice and video calls with end-to-end encryption.",
                    timestamp = "10:45 AM",
                    dateGroup = "Today",
                    status = MessageStatus.READ,
                    replyToId = "msg_2",
                    replyToText = "Thanks! The animations and UI fidelity are super smooth.",
                    replyToSender = "Me",
                    reactions = listOf(
                        Reaction(emoji = "👍", count = 3, userReacted = false),
                        Reaction(emoji = "❤️", count = 12, userReacted = true)
                    )
                )
            )
        )
    )

    fun getChats(): Flow<List<Chat>> = callbackFlow {
        if (firestore != null) {
            val listener = firestore.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) {
                        trySend(_localChats.value)
                        return@addSnapshotListener
                    }
                    val remoteChats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)?.copy(id = doc.id)
                    }
                    if (remoteChats.isNotEmpty()) {
                        trySend(remoteChats)
                    } else {
                        trySend(_localChats.value)
                    }
                }
            awaitClose { listener.remove() }
        } else {
            val job = launch {
                _localChats.collect { list -> trySend(list) }
            }
            awaitClose { job.cancel() }
        }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        if (firestore != null) {
            val listener = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) {
                        trySend(_messagesByChat.value[chatId] ?: emptyList())
                        return@addSnapshotListener
                    }
                    val remoteMessages = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val cipherText = data["cipherText"] as? String ?: (data["text"] as? String ?: "")
                        val iv = data["iv"] as? String ?: ""
                        val decrypted = if (iv.isNotEmpty()) EncryptionHelper.decrypt(cipherText, iv) else cipherText

                        Message(
                            id = doc.id,
                            chatId = chatId,
                            senderId = data["senderId"] as? String ?: "",
                            senderName = data["senderName"] as? String ?: "",
                            text = decrypted,
                            timestamp = data["timestampFormatted"] as? String ?: "Just now",
                            dateGroup = "Today",
                            status = MessageStatus.READ
                        )
                    }
                    if (remoteMessages.isNotEmpty()) {
                        trySend(remoteMessages)
                    } else {
                        trySend(_messagesByChat.value[chatId] ?: emptyList())
                    }
                }
            awaitClose { listener.remove() }
        } else {
            val job = launch {
                _messagesByChat.collect { map ->
                    trySend(map[chatId] ?: emptyList())
                }
            }
            awaitClose { job.cancel() }
        }
    }

    fun getAllMessages(): Flow<List<Message>> = callbackFlow {
        val job = launch {
            _messagesByChat.collect { map ->
                trySend(map.values.flatten())
            }
        }
        awaitClose { job.cancel() }
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        replyTo: Message? = null
    ): Message {
        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val (cipherText, iv) = EncryptionHelper.encrypt(text)

        val newMsg = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentUserId,
            senderName = "Me",
            text = text,
            timestamp = timeString,
            dateGroup = "Today",
            status = MessageStatus.SENT,
            replyToId = replyTo?.id,
            replyToText = replyTo?.text,
            replyToSender = replyTo?.senderName
        )

        // Optimistically update local cache
        _messagesByChat.update { currentMap ->
            val list = currentMap[chatId]?.toMutableList() ?: mutableListOf()
            list.add(newMsg)
            currentMap + (chatId to list)
        }

        // Update last message in chat list
        _localChats.update { list ->
            list.map { c ->
                if (c.id == chatId) c.copy(lastMessage = text, timestamp = timeString) else c
            }
        }

        // Sync with Firestore if active
        try {
            if (firestore != null) {
                val data = hashMapOf(
                    "senderId" to currentUserId,
                    "senderName" to "Me",
                    "cipherText" to cipherText,
                    "iv" to iv,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "timestampFormatted" to timeString,
                    "replyToId" to replyTo?.id,
                    "replyToText" to replyTo?.text,
                    "replyToSender" to replyTo?.senderName
                )
                firestore.collection("chats").document(chatId)
                    .collection("messages").document(newMsg.id)
                    .set(data)
                
                firestore.collection("chats").document(chatId)
                    .update("lastMessage", text, "timestamp", timeString)
            }
        } catch (_: Exception) {
            // Firestore not configured or offline; optimistic message persists
        }

        return newMsg
    }

    fun addReaction(chatId: String, messageId: String, emoji: String) {
        _messagesByChat.update { map ->
            val currentList = map[chatId]?.toMutableList() ?: return@update map
            val index = currentList.indexOfFirst { it.id == messageId }
            if (index >= 0) {
                val msg = currentList[index]
                val existing = msg.reactions.find { it.emoji == emoji }
                val updatedReactions = if (existing != null) {
                    msg.reactions.map {
                        if (it.emoji == emoji) {
                            it.copy(
                                count = if (it.userReacted) it.count - 1 else it.count + 1,
                                userReacted = !it.userReacted
                            )
                        } else it
                    }.filter { it.count > 0 }
                } else {
                    msg.reactions + Reaction(emoji = emoji, count = 1, userReacted = true)
                }
                currentList[index] = msg.copy(reactions = updatedReactions)
            }
            map + (chatId to currentList)
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        _messagesByChat.update { map ->
            val currentList = map[chatId]?.toMutableList() ?: return@update map
            currentList.removeAll { it.id == messageId }
            map + (chatId to currentList)
        }
    }

    fun archiveChat(chatId: String) {
        _localChats.update { list ->
            list.map { if (it.id == chatId) it.copy(isArchived = true) else it }
        }
        try {
            firestore?.collection("chats")?.document(chatId)?.update("archived", true)
        } catch (_: Exception) {}
    }

    fun deleteChat(chatId: String) {
        _localChats.update { list -> list.filter { it.id != chatId } }
        try {
            firestore?.collection("chats")?.document(chatId)?.delete()
        } catch (_: Exception) {}
    }

    fun togglePin(chatId: String) {
        _localChats.update { list ->
            list.map { if (it.id == chatId) it.copy(isPinned = !it.isPinned) else it }
        }
    }

    fun toggleMute(chatId: String) {
        _localChats.update { list ->
            list.map { if (it.id == chatId) it.copy(isMuted = !it.isMuted) else it }
        }
    }

    fun markAsRead(chatId: String) {
        _localChats.update { list ->
            list.map { if (it.id == chatId) it.copy(unreadCount = 0, isRead = true) else it }
        }
    }

    fun createGroupChat(chatId: String, name: String, avatarUrl: String, participants: List<String>) {
        val newChat = Chat(
            id = chatId,
            name = name,
            avatarUrl = avatarUrl,
            lastMessage = "Grup dibuat",
            timestamp = "Baru saja",
            unreadCount = 0,
            isPinned = false,
            isMuted = false,
            isRead = true,
            isGroup = true,
            participants = participants
        )
        _localChats.update { current ->
            listOf(newChat) + current.filter { it.id != chatId }
        }
        try {
            firestore?.collection("chats")?.document(chatId)?.set(newChat)
        } catch (_: Exception) {}
    }
}

