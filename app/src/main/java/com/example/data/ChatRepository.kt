package com.example.data

import com.example.model.Chat
import com.example.model.ChecklistItem
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
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatRepository(
    private val firestore: FirebaseFirestore? = com.example.di.AppModule.firestore,
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
                hasTopics = true,
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
                hasTopics = true,
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
            ),
            "chat_2" to listOf(
                Message(
                    id = "msg_c2_gen_1",
                    chatId = "chat_2",
                    senderId = "dev_1",
                    senderName = "Raka Pratama",
                    text = "Selamat datang di **General**! Diskusi umum seputar rilis Android terbaru ada di sini.",
                    timestamp = "09:15 AM",
                    dateGroup = "Today",
                    status = MessageStatus.READ,
                    topicId = "general"
                ),
                Message(
                    id = "msg_c2_t1_1",
                    chatId = "chat_2",
                    senderId = "dev_2",
                    senderName = "Nadia Putri",
                    text = "Di **Topik 1**, kita bahas optimasi `LazyColumn` dan `derivedStateOf` di Jetpack Compose.",
                    timestamp = "09:22 AM",
                    dateGroup = "Today",
                    status = MessageStatus.READ,
                    topicId = "topik_1"
                ),
                Message(
                    id = "msg_c2_t2_1",
                    chatId = "chat_2",
                    senderId = "dev_1",
                    senderName = "Raka Pratama",
                    text = "Untuk **Topik 2**, silakan bagikan konfigurasi aturan keamanan `Firebase Firestore`.",
                    timestamp = "09:30 AM",
                    dateGroup = "Today",
                    status = MessageStatus.READ,
                    topicId = "topik_2"
                )
            )
        )
    )

    fun getChatSnapshotById(chatId: String): Chat? {
        return _localChats.value.find { it.id == chatId }
    }

    fun observeChat(chatId: String): Flow<Chat?> = callbackFlow {
        val localFallback = { _localChats.value.find { it.id == chatId } }
        if (firestore != null && auth?.currentUser != null && chatId.isNotBlank()) {
            val listener = firestore.collection("chats")
                .document(chatId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(localFallback())
                        return@addSnapshotListener
                    }
                    val remote = snapshot.toObject(Chat::class.java)?.copy(id = snapshot.id)
                    trySend(remote ?: localFallback())
                }
            awaitClose { listener.remove() }
        } else {
            val job = launch {
                _localChats.collect { list ->
                    trySend(list.find { it.id == chatId })
                }
            }
            awaitClose { job.cancel() }
        }
    }

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

                        val parsedChecklistItems = parseChecklistItemsFromRaw(data["checklistItems"])
                        val parsedChecklistTitle = (data["checklistTitle"] as? String)?.takeIf { it.isNotBlank() }
                        val allowOthersToToggle = (data["allowOthersToToggle"] as? Boolean) ?: true
                        val parsedVoiceDuration = (data["voiceDurationMillis"] as? Number)?.toLong()
                        val parsedVoiceWaveform = parseVoiceWaveformFromRaw(data["voiceWaveform"])
                        val cachedVoicePath = _messagesByChat.value[chatId]
                            ?.firstOrNull { it.id == doc.id }
                            ?.voiceFilePath

                        Message(
                            id = doc.id,
                            chatId = chatId,
                            senderId = data["senderId"] as? String ?: "",
                            senderName = data["senderName"] as? String ?: "",
                            text = decrypted,
                            timestamp = data["timestampFormatted"] as? String ?: "Baru saja",
                            dateGroup = "Hari Ini",
                            status = MessageStatus.READ,
                            replyToId = data["replyToId"] as? String,
                            replyToText = data["replyToText"] as? String,
                            replyToSender = data["replyToSender"] as? String,
                            forwardedFrom = data["forwardedFrom"] as? String,
                            topicId = (data["topicId"] as? String)?.takeIf { it.isNotBlank() } ?: "general",
                            checklistTitle = parsedChecklistTitle,
                            checklistItems = parsedChecklistItems,
                            allowOthersToToggle = allowOthersToToggle,
                            voiceFilePath = cachedVoicePath,
                            voiceDurationMillis = parsedVoiceDuration,
                            voiceWaveform = parsedVoiceWaveform
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
        replyTo: Message? = null,
        topicId: String = "general"
    ): Message {
        val normalizedTopicId = topicId.trim().ifBlank { "general" }
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
            replyToSender = replyTo?.senderName,
            topicId = normalizedTopicId
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
            if (firestore != null && auth?.currentUser != null) {
                val targetLocalChat = _localChats.value.find { it.id == chatId }
                val rawParticipants = targetLocalChat?.participants ?: listOf(currentUserId)
                val normalizedParticipants = (rawParticipants.filter { it != "user_telegram_me" } + currentUserId).distinct()

                val chatDocRef = firestore.collection("chats").document(chatId)
                val chatSnap = chatDocRef.get().await()
                if (!chatSnap.exists()) {
                    chatDocRef.set(
                        mapOf(
                            "id" to chatId,
                            "name" to if (chatId == currentUserId) "Pesan Tersimpan" else (targetLocalChat?.name ?: "Obrolan"),
                            "lastMessage" to text.take(2000),
                            "timestamp" to timeString.take(64),
                            "unreadCount" to 0,
                            "isPinned" to (chatId == currentUserId),
                            "isMuted" to false,
                            "isRead" to true,
                            "isGroup" to (targetLocalChat?.isGroup ?: false),
                            "hasTopics" to (targetLocalChat?.hasTopics ?: false),
                            "isChannel" to (targetLocalChat?.isChannel ?: false),
                            "isArchived" to false,
                            "participants" to normalizedParticipants.take(50)
                        )
                    ).await()
                } else {
                    chatDocRef.update(
                        mapOf(
                            "lastMessage" to text.take(2000),
                            "timestamp" to timeString.take(64)
                        )
                    ).await()
                }

                val data = mapOf(
                    "senderId" to currentUserId,
                    "senderName" to (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Me"),
                    "cipherText" to cipherText,
                    "iv" to iv,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "timestampFormatted" to timeString.take(64),
                    "topicId" to normalizedTopicId.take(64),
                    "participants" to normalizedParticipants.take(50),
                    "replyToId" to replyTo?.id,
                    "replyToText" to replyTo?.text?.take(2000),
                    "replyToSender" to replyTo?.senderName?.take(100)
                ).filterValues { it != null }

                chatDocRef.collection("messages")
                    .document(newMsg.id)
                    .set(data)
                    .await()
            }
        } catch (_: Exception) {
            // Firestore not configured or offline; optimistic message persists
        }

        return newMsg
    }

    suspend fun sendChecklistMessage(
        chatId: String,
        title: String,
        items: List<ChecklistItem>,
        allowOthersToToggle: Boolean = true,
        topicId: String = "general"
    ): Message {
        val trimmedTitle = title.trim().take(200)
        val sanitizedItems = items
            .mapNotNull { item ->
                val text = item.text.trim().take(300)
                if (text.isEmpty()) null else item.copy(
                    id = item.id.trim().ifBlank { UUID.randomUUID().toString() }.take(128),
                    text = text
                )
            }
            .take(50)

        require(trimmedTitle.isNotEmpty()) { "Judul daftar tugas tidak boleh kosong." }
        require(sanitizedItems.isNotEmpty()) { "Tambahkan minimal satu tugas." }

        val normalizedTopicId = topicId.trim().ifBlank { "general" }
        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val summaryText = buildChecklistSummaryText(trimmedTitle, sanitizedItems)
        val (cipherText, iv) = EncryptionHelper.encrypt(summaryText)

        val newMsg = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentUserId,
            senderName = "Me",
            text = summaryText,
            timestamp = timeString,
            dateGroup = "Today",
            status = MessageStatus.SENT,
            topicId = normalizedTopicId,
            checklistTitle = trimmedTitle,
            checklistItems = sanitizedItems,
            allowOthersToToggle = allowOthersToToggle
        )

        _messagesByChat.update { currentMap ->
            val list = currentMap[chatId]?.toMutableList() ?: mutableListOf()
            list.add(newMsg)
            currentMap + (chatId to list)
        }

        _localChats.update { list ->
            list.map { c ->
                if (c.id == chatId) c.copy(lastMessage = "☑️ $trimmedTitle", timestamp = timeString) else c
            }
        }

        try {
            if (firestore != null && auth?.currentUser != null) {
                persistChecklistMessageRemote(
                    chatId = chatId,
                    message = newMsg,
                    cipherText = cipherText,
                    iv = iv,
                    timeString = timeString,
                    normalizedTopicId = normalizedTopicId
                )
            }
        } catch (_: Exception) {
            // Offline fallback keeps optimistic checklist intact
        }

        return newMsg
    }

    suspend fun persistChecklistMessageRemote(
        chatId: String,
        message: Message,
        cipherText: String,
        iv: String,
        timeString: String,
        normalizedTopicId: String = "general",
        explicitParticipants: List<String>? = null
    ) {
        val db = firestore ?: error("Firestore tidak tersedia.")
        val uid = auth?.currentUser?.uid ?: error("Wajib masuk akun terlebih dahulu.")
        val targetLocalChat = _localChats.value.find { it.id == chatId }
        val rawParticipants = explicitParticipants ?: targetLocalChat?.participants ?: listOf(uid)
        val normalizedParticipants = (rawParticipants.filter { it != "user_telegram_me" } + uid).distinct()

        val chatDocRef = db.collection("chats").document(chatId)
        val chatSnap = chatDocRef.get().await()
        val lastPreview = "☑️ ${message.checklistTitle ?: "Daftar Tugas"}".take(2000)

        if (!chatSnap.exists()) {
            chatDocRef.set(
                mapOf(
                    "id" to chatId,
                    "name" to if (chatId == uid) "Pesan Tersimpan" else (targetLocalChat?.name ?: "Obrolan"),
                    "lastMessage" to lastPreview,
                    "timestamp" to timeString.take(64),
                    "unreadCount" to 0,
                    "isPinned" to (chatId == uid),
                    "isMuted" to false,
                    "isRead" to true,
                    "isGroup" to (targetLocalChat?.isGroup ?: false),
                    "hasTopics" to (targetLocalChat?.hasTopics ?: false),
                    "isChannel" to (targetLocalChat?.isChannel ?: false),
                    "isArchived" to false,
                    "participants" to normalizedParticipants.take(50)
                )
            ).await()
        } else {
            chatDocRef.update(
                mapOf(
                    "lastMessage" to lastPreview,
                    "timestamp" to timeString.take(64)
                )
            ).await()
        }

        val itemMaps = message.checklistItems.map { item ->
            mapOf(
                "id" to item.id.take(128),
                "text" to item.text.take(300),
                "isDone" to item.isDone
            )
        }

        val payload = mapOf(
            "senderId" to uid,
            "senderName" to (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Me"),
            "cipherText" to cipherText,
            "iv" to iv,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "timestampFormatted" to timeString.take(64),
            "topicId" to normalizedTopicId.take(64),
            "checklistTitle" to (message.checklistTitle ?: "Daftar Tugas").take(200),
            "checklistItems" to itemMaps,
            "allowOthersToToggle" to message.allowOthersToToggle,
            "participants" to normalizedParticipants.take(50)
        )

        chatDocRef.collection("messages")
            .document(message.id)
            .set(payload)
            .await()
    }

    suspend fun toggleChecklistItem(chatId: String, messageId: String, itemId: String) {
        var toggledLocalItems: List<ChecklistItem>? = null
        _messagesByChat.update { map ->
            val currentList = map[chatId]?.toMutableList() ?: return@update map
            val index = currentList.indexOfFirst { it.id == messageId }
            if (index >= 0) {
                val msg = currentList[index]
                val updatedItems = msg.checklistItems.map { item ->
                    if (item.id == itemId) item.copy(isDone = !item.isDone) else item
                }
                toggledLocalItems = updatedItems
                val updatedTitle = msg.checklistTitle ?: "Daftar Tugas"
                currentList[index] = msg.copy(
                    checklistItems = updatedItems,
                    text = buildChecklistSummaryText(updatedTitle, updatedItems)
                )
            }
            map + (chatId to currentList)
        }

        if (firestore != null && auth?.currentUser != null) {
            try {
                toggleChecklistItemRemote(
                    chatId = chatId,
                    messageId = messageId,
                    itemId = itemId,
                    precomputedItems = toggledLocalItems
                )
            } catch (_: Exception) {
                // Offline fallback preserves optimistic checkbox toggle
            }
        }
    }

    suspend fun toggleChecklistItemRemote(
        chatId: String,
        messageId: String,
        itemId: String,
        precomputedItems: List<ChecklistItem>? = null
    ): List<ChecklistItem> {
        val db = firestore ?: error("Firestore tidak tersedia.")
        val msgRef = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(messageId)

        val finalItems = if (precomputedItems != null && precomputedItems.isNotEmpty()) {
            precomputedItems
        } else {
            val snap = msgRef.get().await()
            val existingItems = parseChecklistItemsFromRaw(snap.data?.get("checklistItems"))
            existingItems.map { item ->
                if (item.id == itemId) item.copy(isDone = !item.isDone) else item
            }
        }

        val itemMaps = finalItems.map { item ->
            mapOf(
                "id" to item.id.take(128),
                "text" to item.text.take(300),
                "isDone" to item.isDone
            )
        }

        msgRef.update("checklistItems", itemMaps).await()
        return finalItems
    }

    suspend fun sendVoiceNoteMessage(
        chatId: String,
        filePath: String,
        durationMillis: Long,
        waveform: List<Float>,
        topicId: String = "general"
    ): Message {
        val safeDuration = durationMillis.coerceIn(300L, 3_600_000L)
        val safeWaveform = if (waveform.isEmpty()) {
            List(28) { 0.45f }
        } else {
            waveform.take(80).map { it.coerceIn(0.08f, 1.0f) }
        }
        val normalizedTopicId = topicId.trim().ifBlank { "general" }
        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val summaryText = buildVoiceNoteSummaryText(safeDuration)
        val (cipherText, iv) = EncryptionHelper.encrypt(summaryText)

        val newMsg = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentUserId,
            senderName = "Me",
            text = summaryText,
            timestamp = timeString,
            dateGroup = "Today",
            status = MessageStatus.SENT,
            topicId = normalizedTopicId,
            voiceFilePath = filePath,
            voiceDurationMillis = safeDuration,
            voiceWaveform = safeWaveform
        )

        _messagesByChat.update { currentMap ->
            val list = currentMap[chatId]?.toMutableList() ?: mutableListOf()
            list.add(newMsg)
            currentMap + (chatId to list)
        }

        _localChats.update { list ->
            list.map { c ->
                if (c.id == chatId) c.copy(lastMessage = summaryText, timestamp = timeString) else c
            }
        }

        try {
            if (firestore != null && auth?.currentUser != null) {
                persistVoiceNoteMessageRemote(
                    chatId = chatId,
                    message = newMsg,
                    cipherText = cipherText,
                    iv = iv,
                    timeString = timeString,
                    normalizedTopicId = normalizedTopicId
                )
            }
        } catch (_: Exception) {
            // Offline fallback preserves local trimmed voice note message
        }

        return newMsg
    }

    suspend fun persistVoiceNoteMessageRemote(
        chatId: String,
        message: Message,
        cipherText: String,
        iv: String,
        timeString: String,
        normalizedTopicId: String = "general",
        explicitParticipants: List<String>? = null
    ) {
        val db = firestore ?: error("Firestore tidak tersedia.")
        val uid = auth?.currentUser?.uid ?: error("Wajib masuk akun terlebih dahulu.")
        val targetLocalChat = _localChats.value.find { it.id == chatId }
        val rawParticipants = explicitParticipants ?: targetLocalChat?.participants ?: listOf(uid)
        val normalizedParticipants = (rawParticipants.filter { it != "user_telegram_me" } + uid).distinct()

        val chatDocRef = db.collection("chats").document(chatId)
        val chatSnap = chatDocRef.get().await()
        val lastPreview = message.text.take(2000)

        if (!chatSnap.exists()) {
            chatDocRef.set(
                mapOf(
                    "id" to chatId,
                    "name" to if (chatId == uid) "Pesan Tersimpan" else (targetLocalChat?.name ?: "Obrolan"),
                    "lastMessage" to lastPreview,
                    "timestamp" to timeString.take(64),
                    "unreadCount" to 0,
                    "isPinned" to (chatId == uid),
                    "isMuted" to false,
                    "isRead" to true,
                    "isGroup" to (targetLocalChat?.isGroup ?: false),
                    "hasTopics" to (targetLocalChat?.hasTopics ?: false),
                    "isChannel" to (targetLocalChat?.isChannel ?: false),
                    "isArchived" to false,
                    "participants" to normalizedParticipants.take(50)
                )
            ).await()
        } else {
            chatDocRef.update(
                mapOf(
                    "lastMessage" to lastPreview,
                    "timestamp" to timeString.take(64)
                )
            ).await()
        }

        val safeDuration = (message.voiceDurationMillis ?: 1_000L).coerceIn(100L, 3_600_000L)
        val safeWaveform = (if (message.voiceWaveform.isEmpty()) listOf(0.5f) else message.voiceWaveform)
            .take(80)
            .map { it.coerceIn(0f, 1f).toDouble() }

        val payload = mapOf(
            "senderId" to uid,
            "senderName" to (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Me"),
            "cipherText" to cipherText,
            "iv" to iv,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "timestampFormatted" to timeString.take(64),
            "topicId" to normalizedTopicId.take(64),
            "voiceDurationMillis" to safeDuration,
            "voiceWaveform" to safeWaveform,
            "participants" to normalizedParticipants.take(50)
        )

        chatDocRef.collection("messages")
            .document(message.id)
            .set(payload)
            .await()
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
        try {
            if (firestore != null && auth?.currentUser != null) {
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(messageId)
                    .delete()
            }
        } catch (_: Exception) {}
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

    fun createGroupChat(
        chatId: String,
        name: String,
        avatarUrl: String,
        participants: List<String>,
        hasTopics: Boolean = true
    ) {
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
            hasTopics = hasTopics,
            participants = participants
        )
        _localChats.update { current ->
            listOf(newChat) + current.filter { it.id != chatId }
        }
        try {
            firestore?.collection("chats")?.document(chatId)?.set(newChat)
        } catch (_: Exception) {}
    }

    suspend fun ensureSavedMessagesChat(): String {
        val uid = currentUserId
        if (firestore != null && auth?.currentUser != null) {
            try {
                val docRef = firestore.collection("chats").document(uid)
                val snapshot = docRef.get().await()
                if (!snapshot.exists()) {
                    val savedChatData = mapOf(
                        "id" to uid,
                        "name" to "Pesan Tersimpan",
                        "avatarUrl" to "",
                        "lastMessage" to "Penyimpanan cloud pribadi",
                        "timestamp" to "Aktif",
                        "unreadCount" to 0,
                        "isPinned" to true,
                        "isMuted" to false,
                        "isRead" to true,
                        "isGroup" to false,
                        "isChannel" to false,
                        "isArchived" to false,
                        "participants" to listOf(uid)
                    )
                    docRef.set(savedChatData, SetOptions.merge()).await()
                }
            } catch (_: Exception) {
                // Offline fallback remains active
            }
        }
        return uid
    }

    suspend fun fetchMessageById(chatId: String, messageId: String): Message? {
        val cached = _messagesByChat.value[chatId]?.find { it.id == messageId }
        if (cached != null) return cached

        if (firestore != null) {
            try {
                val doc = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(messageId)
                    .get()
                    .await()
                val data = doc.data
                if (doc.exists() && data != null) {
                    val cipherText = data["cipherText"] as? String ?: (data["text"] as? String ?: "")
                    val iv = data["iv"] as? String ?: ""
                    val decrypted = if (iv.isNotEmpty()) EncryptionHelper.decrypt(cipherText, iv) else cipherText
                    return Message(
                        id = doc.id,
                        chatId = chatId,
                        senderId = data["senderId"] as? String ?: "",
                        senderName = data["senderName"] as? String ?: "",
                        text = decrypted,
                        timestamp = data["timestampFormatted"] as? String ?: "Baru saja",
                        dateGroup = "Hari Ini",
                        status = MessageStatus.READ,
                        replyToId = data["replyToId"] as? String,
                        replyToText = data["replyToText"] as? String,
                        replyToSender = data["replyToSender"] as? String,
                        forwardedFrom = data["forwardedFrom"] as? String,
                        topicId = (data["topicId"] as? String)?.takeIf { it.isNotBlank() } ?: "general",
                        checklistTitle = (data["checklistTitle"] as? String)?.takeIf { it.isNotBlank() },
                        checklistItems = parseChecklistItemsFromRaw(data["checklistItems"]),
                        allowOthersToToggle = (data["allowOthersToToggle"] as? Boolean) ?: true,
                        voiceDurationMillis = (data["voiceDurationMillis"] as? Number)?.toLong(),
                        voiceWaveform = parseVoiceWaveformFromRaw(data["voiceWaveform"])
                    )
                }
            } catch (_: Exception) {
                // Fallback null if unreachable
            }
        }
        return null
    }

    suspend fun forwardMessageToChats(
        sourceMessage: Message,
        targetChatIds: List<String>,
        customComment: String = ""
    ): Result<Int> {
        if (targetChatIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("Pilih minimal satu obrolan tujuan."))
        }
        return try {
            val uid = currentUserId
            val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val originalAuthor = when {
                !sourceMessage.forwardedFrom.isNullOrBlank() -> sourceMessage.forwardedFrom
                sourceMessage.senderName.isNotBlank() && sourceMessage.senderName != "Me" -> sourceMessage.senderName
                else -> auth?.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Pengguna Jayra"
            }
            val senderDisplayName = auth?.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Me"
            val trimmedComment = customComment.trim()

            for (targetChatId in targetChatIds.distinct()) {
                val targetLocalChat = _localChats.value.find { it.id == targetChatId }
                val rawParticipants = targetLocalChat?.participants ?: listOf(uid)
                val normalizedParticipants = (rawParticipants.filter { it != "user_telegram_me" } + uid).distinct()

                // 1. Optional user comment sent right before forwarded message (authentic Telegram behavior)
                if (trimmedComment.isNotEmpty()) {
                    val commentId = UUID.randomUUID().toString()
                    val (commentCipher, commentIv) = EncryptionHelper.encrypt(trimmedComment)
                    val commentMsg = Message(
                        id = commentId,
                        chatId = targetChatId,
                        senderId = uid,
                        senderName = senderDisplayName,
                        text = trimmedComment,
                        timestamp = timeString,
                        dateGroup = "Hari Ini",
                        status = MessageStatus.SENT
                    )
                    _messagesByChat.update { currentMap ->
                        val list = currentMap[targetChatId]?.toMutableList() ?: mutableListOf()
                        list.add(commentMsg)
                        currentMap + (targetChatId to list)
                    }
                    syncMessageDocumentToFirestore(
                        targetChatId = targetChatId,
                        targetChatName = if (targetChatId == uid) "Pesan Tersimpan" else (targetLocalChat?.name ?: "Obrolan"),
                        isGroup = targetLocalChat?.isGroup ?: false,
                        isChannel = targetLocalChat?.isChannel ?: false,
                        participants = normalizedParticipants,
                        messageId = commentId,
                        senderId = uid,
                        senderName = senderDisplayName,
                        cipherText = commentCipher,
                        iv = commentIv,
                        timeString = timeString,
                        forwardedFrom = null,
                        lastMessagePreview = trimmedComment
                    )
                }

                // 2. Forwarded message itself
                val forwardedId = UUID.randomUUID().toString()
                val (cipherText, iv) = EncryptionHelper.encrypt(sourceMessage.text)
                val forwardedMsg = Message(
                    id = forwardedId,
                    chatId = targetChatId,
                    senderId = uid,
                    senderName = senderDisplayName,
                    text = sourceMessage.text,
                    timestamp = timeString,
                    dateGroup = "Hari Ini",
                    status = MessageStatus.SENT,
                    forwardedFrom = originalAuthor,
                    checklistTitle = sourceMessage.checklistTitle,
                    checklistItems = sourceMessage.checklistItems,
                    allowOthersToToggle = sourceMessage.allowOthersToToggle,
                    voiceFilePath = sourceMessage.voiceFilePath,
                    voiceDurationMillis = sourceMessage.voiceDurationMillis,
                    voiceWaveform = sourceMessage.voiceWaveform
                )

                _messagesByChat.update { currentMap ->
                    val list = currentMap[targetChatId]?.toMutableList() ?: mutableListOf()
                    list.add(forwardedMsg)
                    currentMap + (targetChatId to list)
                }

                val previewLabel = "↪ ${sourceMessage.text}"
                _localChats.update { list ->
                    val exists = list.any { it.id == targetChatId }
                    if (exists) {
                        list.map { c ->
                            if (c.id == targetChatId) {
                                c.copy(lastMessage = previewLabel.take(120), timestamp = timeString)
                            } else c
                        }
                    } else if (targetChatId == uid) {
                        listOf(
                            Chat(
                                id = uid,
                                name = "Pesan Tersimpan",
                                lastMessage = previewLabel.take(120),
                                timestamp = timeString,
                                isPinned = true,
                                participants = listOf(uid)
                            )
                        ) + list
                    } else {
                        list
                    }
                }

                syncMessageDocumentToFirestore(
                    targetChatId = targetChatId,
                    targetChatName = if (targetChatId == uid) "Pesan Tersimpan" else (targetLocalChat?.name ?: "Obrolan"),
                    isGroup = targetLocalChat?.isGroup ?: false,
                    isChannel = targetLocalChat?.isChannel ?: false,
                    participants = normalizedParticipants,
                    messageId = forwardedId,
                    senderId = uid,
                    senderName = senderDisplayName,
                    cipherText = cipherText,
                    iv = iv,
                    timeString = timeString,
                    forwardedFrom = originalAuthor,
                    lastMessagePreview = previewLabel.take(500),
                    checklistTitle = sourceMessage.checklistTitle,
                    checklistItems = sourceMessage.checklistItems,
                    allowOthersToToggle = sourceMessage.allowOthersToToggle,
                    voiceDurationMillis = sourceMessage.voiceDurationMillis,
                    voiceWaveform = sourceMessage.voiceWaveform
                )
            }

            Result.success(targetChatIds.distinct().size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncMessageDocumentToFirestore(
        targetChatId: String,
        targetChatName: String,
        isGroup: Boolean,
        isChannel: Boolean,
        participants: List<String>,
        messageId: String,
        senderId: String,
        senderName: String,
        cipherText: String,
        iv: String,
        timeString: String,
        forwardedFrom: String?,
        lastMessagePreview: String,
        checklistTitle: String? = null,
        checklistItems: List<ChecklistItem> = emptyList(),
        allowOthersToToggle: Boolean = true,
        voiceDurationMillis: Long? = null,
        voiceWaveform: List<Float> = emptyList()
    ) {
        if (firestore == null || auth?.currentUser == null) return
        try {
            val chatDocRef = firestore.collection("chats").document(targetChatId)
            val chatSnap = chatDocRef.get().await()
            if (!chatSnap.exists()) {
                val roomData = mapOf(
                    "id" to targetChatId,
                    "name" to targetChatName.take(120),
                    "lastMessage" to lastMessagePreview.take(2000),
                    "timestamp" to timeString.take(64),
                    "unreadCount" to 0,
                    "isPinned" to (targetChatId == senderId),
                    "isMuted" to false,
                    "isRead" to true,
                    "isGroup" to isGroup,
                    "isChannel" to isChannel,
                    "isArchived" to false,
                    "participants" to participants.take(50)
                )
                chatDocRef.set(roomData).await()
            } else {
                chatDocRef.update(
                    mapOf(
                        "lastMessage" to lastMessagePreview.take(2000),
                        "timestamp" to timeString.take(64)
                    )
                ).await()
            }

            val msgPayload = mutableMapOf<String, Any>(
                "senderId" to senderId,
                "senderName" to senderName.take(100),
                "cipherText" to cipherText,
                "iv" to iv,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "timestampFormatted" to timeString.take(64),
                "participants" to participants.take(50)
            )
            if (!forwardedFrom.isNullOrBlank()) {
                msgPayload["forwardedFrom"] = forwardedFrom.take(100)
            }
            if (!checklistTitle.isNullOrBlank() && checklistItems.isNotEmpty()) {
                msgPayload["checklistTitle"] = checklistTitle.take(200)
                msgPayload["checklistItems"] = checklistItems.take(50).map { item ->
                    mapOf(
                        "id" to item.id.take(128),
                        "text" to item.text.take(300),
                        "isDone" to item.isDone
                    )
                }
                msgPayload["allowOthersToToggle"] = allowOthersToToggle
            }
            if (voiceDurationMillis != null && voiceDurationMillis >= 100L) {
                msgPayload["voiceDurationMillis"] = voiceDurationMillis.coerceIn(100L, 3_600_000L)
                if (voiceWaveform.isNotEmpty()) {
                    msgPayload["voiceWaveform"] = voiceWaveform.take(80).map { it.coerceIn(0f, 1f).toDouble() }
                }
            }

            chatDocRef.collection("messages")
                .document(messageId)
                .set(msgPayload)
                .await()
        } catch (_: Exception) {
            // Local optimistic message remains intact if offline
        }
    }

    companion object {
        fun buildChecklistSummaryText(title: String, items: List<ChecklistItem>): String {
            val lines = items.joinToString("\n") { item ->
                val mark = if (item.isDone) "[x]" else "[ ]"
                "$mark ${item.text}"
            }
            return "☑️ Daftar Tugas: $title\n$lines"
        }

        fun buildVoiceNoteSummaryText(durationMillis: Long): String {
            val compact = com.example.util.AudioWaveTrimmer.formatCompactDurationMillis(durationMillis)
            return "🎤 [Voice Note $compact]"
        }

        fun parseChecklistItemsFromRaw(raw: Any?): List<ChecklistItem> {
            val rawList = raw as? List<*> ?: return emptyList()
            return rawList.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val id = (map["id"] as? String)?.trim().orEmpty()
                val text = (map["text"] as? String)?.trim().orEmpty()
                val isDone = (map["isDone"] as? Boolean) ?: false
                if (id.isNotEmpty() && text.isNotEmpty()) {
                    ChecklistItem(id = id, text = text, isDone = isDone)
                } else {
                    null
                }
            }
        }

        fun parseVoiceWaveformFromRaw(raw: Any?): List<Float> {
            val rawList = raw as? List<*> ?: return emptyList()
            return rawList.mapNotNull { entry ->
                (entry as? Number)?.toFloat()?.coerceIn(0.08f, 1.0f)
            }.take(80)
        }
    }
}


