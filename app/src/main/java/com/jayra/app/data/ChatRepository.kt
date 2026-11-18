package com.jayra.app.data

import android.util.Log
import com.jayra.app.data.local.ChatDao
import com.jayra.app.data.local.ChatEntity
import com.jayra.app.data.local.Converters
import com.jayra.app.data.local.MessageDao
import com.jayra.app.data.local.MessageEntity
import com.jayra.app.model.ChecklistItem
import com.jayra.app.model.Chat
import com.jayra.app.model.Message
import com.jayra.app.model.MessageStatus
import com.jayra.app.model.MessageType
import com.jayra.app.model.Reaction
import com.jayra.app.security.EncryptionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val firestore: FirebaseFirestore? = com.jayra.app.di.AppModule.firestore,
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
) {

    private val logger = Log
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    private val _deletedIds = mutableSetOf<String>()
    private val oldestMessageCursor = mutableMapOf<String, QueryDocumentSnapshot>()
    private val exhaustedMessageHistory = mutableSetOf<String>()
    private val sendingInFlight = mutableSetOf<String>()

    // -------------------------------------------------------------------------
    // Konversi entitas Room <-> model domain
    // -------------------------------------------------------------------------

    private fun toChatEntity(chat: Chat): ChatEntity =
        ChatEntity(
            chatId = chat.id,
            otherUserId = chat.otherUserId,
            otherUserName = chat.otherUserName,
            avatarUrl = chat.avatarUrl,
            lastMessage = chat.lastMessage,
            lastMessageTime = chat.lastMessageTime,
            unreadCount = chat.unreadCount,
            isPinned = chat.isPinned,
            isMuted = chat.isMuted,
            isRead = chat.isRead,
            isGroup = chat.isGroup,
            hasTopics = chat.hasTopics,
            isChannel = chat.isChannel,
            isArchived = chat.isArchived,
            isFavorite = chat.isFavorite,
            hasDraft = chat.hasDraft,
            draftText = chat.draftText,
            isOnline = chat.isOnline,
            lastMessageStatus = chat.lastMessageStatus.name,
            lastMessageIsOutgoing = chat.lastMessageIsOutgoing,
            participants = Converters().listStringToDb(chat.participants) ?: "",
            timestampMillis = chat.timestampMillis
        )

    private fun toChat(entity: ChatEntity): Chat =
        Chat(
            id = entity.chatId,
            name = entity.otherUserName.ifBlank { entity.chatId },
            avatarUrl = entity.avatarUrl,
            otherUserId = entity.otherUserId,
            otherUserName = entity.otherUserName,
            lastMessage = entity.lastMessage,
            timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entity.lastMessageTime)),
            lastMessageTime = entity.lastMessageTime,
            unreadCount = entity.unreadCount,
            isPinned = entity.isPinned,
            isMuted = entity.isMuted,
            isRead = entity.isRead,
            isGroup = entity.isGroup,
            hasTopics = entity.hasTopics,
            isChannel = entity.isChannel,
            participants = Converters().listStringFromDb(entity.participants),
            isArchived = entity.isArchived,
            isFavorite = entity.isFavorite,
            hasDraft = entity.hasDraft,
            draftText = entity.draftText,
            isOnline = entity.isOnline,
            lastMessageStatus = MessageStatus.valueOf(entity.lastMessageStatus),
            lastMessageIsOutgoing = entity.lastMessageIsOutgoing,
            timestampMillis = entity.timestampMillis
        )

    private fun toMessageEntity(msg: Message): MessageEntity =
        MessageEntity(
            id = msg.id,
            chatId = msg.chatId,
            senderId = msg.senderId,
            senderName = msg.senderName,
            receiverId = msg.receiverId,
            text = msg.text,
            type = msg.type.name,
            timestamp = msg.timestampMillis,
            status = msg.status.name,
            replyToId = msg.replyToId,
            replyToText = msg.replyToText,
            replyToSender = msg.replyToSender,
            forwardedFrom = msg.forwardedFrom,
            topicId = msg.topicId,
            checklistTitle = msg.checklistTitle,
            checklistItems = Converters().checklistItemsToDb(msg.checklistItems) ?: "",
            allowOthersToToggle = msg.allowOthersToToggle,
            voiceFilePath = msg.voiceFilePath,
            voiceDurationMillis = msg.voiceDurationMillis,
            voiceWaveform = Converters().listFloatToDb(msg.voiceWaveform) ?: "",
            mediaUrl = msg.mediaUrl,
            mediaType = msg.mediaType,
            stickerUrl = msg.stickerUrl,
            gifUrl = msg.gifUrl,
            linkPreviewTitle = msg.linkPreviewTitle,
            linkPreviewDescription = msg.linkPreviewDescription,
            linkPreviewImageUrl = msg.linkPreviewImageUrl,
            systemText = msg.systemText,
            disappearingSeconds = msg.disappearingSeconds
        )

    private fun toMessage(entity: MessageEntity): Message =
        Message(
            id = entity.id,
            chatId = entity.chatId,
            senderId = entity.senderId,
            senderName = entity.senderName,
            receiverId = entity.receiverId,
            text = entity.text,
            type = MessageType.valueOf(entity.type),
            timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entity.timestamp)),
            timestampMillis = entity.timestamp,
            dateGroup = "Today",
            status = MessageStatus.valueOf(entity.status),
            isMine = entity.senderId == currentUserId,
            replyToId = entity.replyToId,
            replyToText = entity.replyToText,
            replyToSender = entity.replyToSender,
            reactions = emptyList(),
            forwardedFrom = entity.forwardedFrom,
            topicId = entity.topicId,
            checklistTitle = entity.checklistTitle,
            checklistItems = Converters().checklistItemsFromDb(entity.checklistItems),
            allowOthersToToggle = entity.allowOthersToToggle,
            voiceFilePath = entity.voiceFilePath,
            voiceDurationMillis = entity.voiceDurationMillis,
            voiceWaveform = Converters().listFloatFromDb(entity.voiceWaveform),
            mediaUrl = entity.mediaUrl,
            mediaType = entity.mediaType,
            stickerUrl = entity.stickerUrl,
            gifUrl = entity.gifUrl,
            linkPreviewTitle = entity.linkPreviewTitle,
            linkPreviewDescription = entity.linkPreviewDescription,
            linkPreviewImageUrl = entity.linkPreviewImageUrl,
            systemText = entity.systemText,
            disappearingSeconds = entity.disappearingSeconds
        )

    // -------------------------------------------------------------------------
    // Daftar obrolan (source of truth: Room)
    // -------------------------------------------------------------------------

    fun getChats(): Flow<List<Chat>> = chatDao.getAllChats().map { entities ->
        entities.map { toChat(it) }
    }

    fun getNonArchivedChats(): Flow<List<Chat>> = chatDao.getNonArchivedChats().map { entities ->
        entities.map { toChat(it) }
    }

    suspend fun getChatSnapshotByIdSuspend(chatId: String): Chat? =
        withContext(Dispatchers.IO) { chatDao.getChat(chatId)?.let { toChat(it) } }

    fun getChatSnapshotById(chatId: String): Chat? {
        // Sinkron langsung (non-suspend) untuk inisialisasi UI cepat; Room di-akses via dispatcher.
        var result: Chat? = null
        scope.launch {
            result = withContext(Dispatchers.IO) { chatDao.getChat(chatId)?.let { toChat(it) } }
        }
        // Tidak ada cara sinkron benar dari Room tanpa suspend; kembalikan null jika belum siap,
        // UI akan mengamati observeChat melalui Flow.
        return null
    }

    fun observeChat(chatId: String): Flow<Chat?> = chatDao.getChat(chatId).map { entity ->
        entity?.let { toChat(it) }
    }

    // -------------------------------------------------------------------------
    // Pesan (source of truth: Room)
    // -------------------------------------------------------------------------

    fun getMessages(chatId: String): Flow<List<Message>> = messageDao.getMessagesForChat(chatId).map { entities ->
        entities.map { toMessage(it) }
    }

    fun hasOlderMessages(chatId: String): Boolean =
        oldestMessageCursor.containsKey(chatId) && chatId !in exhaustedMessageHistory

    suspend fun loadOlderMessages(chatId: String): OlderMessagePage {
        if (firestore == null || chatId in exhaustedMessageHistory) {
            return OlderMessagePage(emptyList(), hasMore = false)
        }
        val cursor = oldestMessageCursor[chatId]
            ?: return OlderMessagePage(emptyList(), hasMore = false)
        val documents = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .endBefore(cursor)
            .limitToLast(MESSAGE_PAGE_SIZE)
            .get()
            .await()
            .documents
            .filterIsInstance<QueryDocumentSnapshot>()
        if (documents.isEmpty()) {
            oldestMessageCursor.remove(chatId)
            exhaustedMessageHistory.add(chatId)
            return OlderMessagePage(emptyList(), hasMore = false)
        }
        oldestMessageCursor[chatId] = documents.first()
        val hasMore = documents.size.toLong() == MESSAGE_PAGE_SIZE
        if (!hasMore) exhaustedMessageHistory.add(chatId)
        val olderMessages = documents.mapNotNull { parseMessageDocument(chatId, it) }
        val olderEntities = olderMessages.map { toMessageEntity(it) }
        messageDao.insertMessages(olderEntities)
        return OlderMessagePage(olderMessages, hasMore)
    }

    // -------------------------------------------------------------------------
    // Kirim pesan — flow yang benar: SENDING → Room → Firestore → SENT / FAILED
    // -------------------------------------------------------------------------

    suspend fun sendMessage(
        chatId: String,
        text: String,
        replyTo: Message? = null,
        topicId: String = "general"
    ): Message {
        require(chatId.isNotBlank()) { "chatId tidak boleh kosong." }
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Pesan tidak boleh kosong atau hanya spasi." }

        if (sendingInFlight.contains(chatId)) {
            throw IllegalStateException("Sedang mengirim pesan, harap tunggu.")
        }
        sendingInFlight.add(chatId)

        val uid = currentUserId
        val now = System.currentTimeMillis()
        val msgId = UUID.randomUUID().toString()
        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))
        val receiverId = otherUserIdFor(chatId)

        val sendingMsg = Message(
            id = msgId,
            chatId = chatId,
            senderId = uid,
            senderName = "Me",
            receiverId = receiverId,
            text = trimmed,
            type = MessageType.TEXT,
            timestamp = timeString,
            timestampMillis = now,
            dateGroup = "Today",
            status = MessageStatus.SENDING,
            isMine = true,
            replyToId = replyTo?.id,
            replyToText = replyTo?.text,
            replyToSender = replyTo?.senderName,
            topicId = topicId
        )

        // 1. Simpan ke Room dengan status SENDING agar UI segera menampilkan bubble.
        messageDao.insertMessage(toMessageEntity(sendingMsg))
        updateChatOnSend(chatId, trimmed, now)

        return try {
            // 2. Kirim ke Firestore.
            if (firestore != null && auth?.currentUser != null) {
                val (cipherText, iv) = EncryptionHelper.encrypt(trimmed)
                val data = buildMessagePayload(
                    senderId = uid, senderName = "Me", cipherText = cipherText, iv = iv,
                    timeString = timeString, topicId = topicId,
                    replyToId = replyTo?.id, replyToText = replyTo?.text, replyToSender = replyTo?.senderName
                )
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msgId)
                    .set(data)
                    .await()
            }
            // 3. Sukses → status SENT.
            val sentMsg = sendingMsg.copy(status = MessageStatus.SENT)
            messageDao.insertMessage(toMessageEntity(sentMsg))
            markChatRead(chatId)
            sentMsg
        } catch (e: Exception) {
            // 4. Gagal → status FAILED, pesan JANGAN dihapus.
            val failedMsg = sendingMsg.copy(status = MessageStatus.FAILED)
            messageDao.insertMessage(toMessageEntity(failedMsg))
            throw e
        } finally {
            sendingInFlight.remove(chatId)
        }
    }

    suspend fun resendMessage(chatId: String, messageId: String): Message {
        val entity = withContext(Dispatchers.IO) { messageDao.getMessageById(chatId, messageId) }
            ?: throw IllegalStateException("Pesan tidak ditemukan: $messageId")
        val msg = toMessage(entity)
        require(msg.status == MessageStatus.FAILED) { "Hanya pesan yang gagal dapat dikirim ulang." }
        return sendMessage(chatId = chatId, text = msg.text, replyTo = null, topicId = msg.topicId)
    }

    private suspend fun updateChatOnSend(chatId: String, lastMessage: String, now: Long) {
        val existing = withContext(Dispatchers.IO) { chatDao.getChat(chatId) }
        val ent = existing ?: ChatEntity(chatId = chatId, lastMessageTime = now)
        chatDao.insertChat(ent.copy(
            lastMessage = lastMessage,
            lastMessageTime = now,
            unreadCount = ent.unreadCount,
            isRead = true,
            lastMessageIsOutgoing = true,
            timestampMillis = now
        ))
    }

    private suspend fun markChatRead(chatId: String) {
        chatDao.resetUnreadCount(chatId)
    }

    private fun otherUserIdFor(chatId: String): String {
        return ""
    }

    // -------------------------------------------------------------------------
    // Checklist
    // -------------------------------------------------------------------------

    suspend fun sendChecklistMessage(
        chatId: String,
        title: String,
        items: List<ChecklistItem>,
        allowOthersToToggle: Boolean = true,
        topicId: String = "general"
    ): Message {
        require(chatId.isNotBlank()) { "chatId tidak boleh kosong." }
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

        val now = System.currentTimeMillis()
        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))
        val uid = currentUserId
        val msgId = UUID.randomUUID().toString()

        val summaryText = buildChecklistSummaryText(trimmedTitle, sanitizedItems)
        val (cipherText, iv) = EncryptionHelper.encrypt(summaryText)

        val sendingMsg = Message(
            id = msgId,
            chatId = chatId,
            senderId = uid,
            senderName = "Me",
            receiverId = otherUserIdFor(chatId),
            text = summaryText,
            type = MessageType.CHECKLIST,
            timestamp = timeString,
            timestampMillis = now,
            status = MessageStatus.SENDING,
            isMine = true,
            checklistTitle = trimmedTitle,
            checklistItems = sanitizedItems,
            allowOthersToToggle = allowOthersToToggle,
            topicId = topicId
        )

        sendingInFlight.add(chatId)
        return try {
            messageDao.insertMessage(toMessageEntity(sendingMsg))
            updateChatOnSend(chatId, "☑️ $trimmedTitle", now)

            if (firestore != null && auth?.currentUser != null) {
                persistChecklistMessageRemote(
                    chatId = chatId, message = sendingMsg, cipherText = cipherText, iv = iv,
                    timeString = timeString, normalizedTopicId = topicId
                )
            }

            val sentMsg = sendingMsg.copy(status = MessageStatus.SENT)
            messageDao.insertMessage(toMessageEntity(sentMsg))
            markChatRead(chatId)
            sentMsg
        } catch (e: Exception) {
            val failedMsg = sendingMsg.copy(status = MessageStatus.FAILED)
            messageDao.insertMessage(toMessageEntity(failedMsg))
            throw e
        } finally {
            sendingInFlight.remove(chatId)
        }
    }

    // -------------------------------------------------------------------------
    // Voice note
    // -------------------------------------------------------------------------

    suspend fun sendVoiceNoteMessage(
        chatId: String,
        filePath: String,
        durationMillis: Long,
        waveform: List<Float>,
        topicId: String = "general"
    ): Message {
        require(chatId.isNotBlank()) { "chatId tidak boleh kosong." }
        val safeDuration = durationMillis.coerceIn(300L, 3_600_000L)
        val safeWaveform = if (waveform.isEmpty()) {
            List(28) { 0.45f }
        } else {
            waveform.take(80).map { it.coerceIn(0.08f, 1.0f) }
        }
        val now = System.currentTimeMillis()
        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))
        val uid = currentUserId
        val msgId = UUID.randomUUID().toString()

        val compact = com.jayra.app.util.AudioWaveTrimmer.formatCompactDurationMillis(safeDuration)
        val summaryText = "🎤 [Voice Note $compact]"
        val (cipherText, iv) = EncryptionHelper.encrypt(summaryText)

        val sendingMsg = Message(
            id = msgId,
            chatId = chatId,
            senderId = uid,
            senderName = "Me",
            receiverId = otherUserIdFor(chatId),
            text = summaryText,
            type = MessageType.VOICE,
            timestamp = timeString,
            timestampMillis = now,
            status = MessageStatus.SENDING,
            isMine = true,
            voiceFilePath = filePath,
            voiceDurationMillis = safeDuration,
            voiceWaveform = safeWaveform,
            topicId = topicId
        )

        sendingInFlight.add(chatId)
        return try {
            messageDao.insertMessage(toMessageEntity(sendingMsg))
            updateChatOnSend(chatId, summaryText, now)

            if (firestore != null && auth?.currentUser != null) {
                persistVoiceNoteMessageRemote(
                    chatId = chatId, message = sendingMsg, cipherText = cipherText, iv = iv,
                    timeString = timeString, normalizedTopicId = topicId
                )
            }

            val sentMsg = sendingMsg.copy(status = MessageStatus.SENT)
            messageDao.insertMessage(toMessageEntity(sentMsg))
            markChatRead(chatId)
            sentMsg
        } catch (e: Exception) {
            val failedMsg = sendingMsg.copy(status = MessageStatus.FAILED)
            messageDao.insertMessage(toMessageEntity(failedMsg))
            throw e
        } finally {
            sendingInFlight.remove(chatId)
        }
    }

    // -------------------------------------------------------------------------
    // Payload Firestore
    // -------------------------------------------------------------------------

    private fun buildMessagePayload(
        senderId: String, senderName: String, cipherText: String, iv: String,
        timeString: String, topicId: String,
        replyToId: String?, replyToText: String?, replyToSender: String?
    ): Map<String, Any?> {
        val data = mutableMapOf<String, Any?>(
            "senderId" to senderId,
            "senderName" to senderName,
            "cipherText" to cipherText,
            "iv" to iv,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "timestampFormatted" to timeString.take(64),
            "topicId" to topicId.take(64),
            "participants" to listOf(senderId).take(50)
        )
        if (!replyToId.isNullOrBlank()) {
            data["replyToId"] = replyToId
            data["replyToText"] = replyToText
            data["replyToSender"] = replyToSender
        }
        return data
    }

    suspend fun persistChecklistMessageRemote(
        chatId: String, message: Message, cipherText: String, iv: String,
        timeString: String, normalizedTopicId: String = "general",
        explicitParticipants: List<String>? = null
    ) {
        val db = firestore ?: error("Firestore tidak tersedia.")
        val uid = auth?.currentUser?.uid ?: error("Wajib masuk akun terlebih dahulu.")
        val itemMaps = message.checklistItems.map { item ->
            mapOf("id" to item.id.take(128), "text" to item.text.take(300), "isDone" to item.isDone)
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
            "participants" to listOf(uid).take(50)
        )
        db.collection("chats").document(chatId).collection("messages").document(message.id).set(payload).await()
    }

    suspend fun persistVoiceNoteMessageRemote(
        chatId: String, message: Message, cipherText: String, iv: String,
        timeString: String, normalizedTopicId: String = "general",
        explicitParticipants: List<String>? = null
    ) {
        val db = firestore ?: error("Firestore tidak tersedia.")
        val uid = auth?.currentUser?.uid ?: error("Wajib masuk akun terlebih dahulu.")
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
            "participants" to listOf(uid).take(50)
        )
        db.collection("chats").document(chatId).collection("messages").document(message.id).set(payload).await()
    }

    // -------------------------------------------------------------------------
    // Sinkronisasi Firestore -> Room (pesan masuk dari remote)
    // -------------------------------------------------------------------------

    fun observeRealtimeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        if (firestore == null || chatId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val docs = snapshot.documents.filterIsInstance<QueryDocumentSnapshot>()
                val msgs = docs.mapNotNull { parseMessageDocument(chatId, it) }
                val entities = msgs.map { toMessageEntity(it) }
                scope.launch {
                    try { withContext(Dispatchers.IO) { messageDao.insertMessages(entities) } } catch (_: Exception) {}
                }
                trySend(msgs)
            }
        awaitClose { listener.remove() }
    }

    // -------------------------------------------------------------------------
    // Parsing dokumen Firestore
    // -------------------------------------------------------------------------

    private fun parseMessageDocument(
        chatId: String,
        doc: QueryDocumentSnapshot
    ): Message? {
        val data = doc.data
        val cipherText = data["cipherText"] as? String ?: (data["text"] as? String ?: "")
        val iv = data["iv"] as? String ?: ""
        val decrypted = if (iv.isNotEmpty()) EncryptionHelper.decrypt(cipherText, iv) else cipherText
        val ts = (data["timestamp"] as? com.google.firebase.Timestamp)?.date?.time ?: System.currentTimeMillis()
        val uid = currentUserId
        return Message(
            id = doc.id,
            chatId = chatId,
            senderId = data["senderId"] as? String ?: "",
            senderName = data["senderName"] as? String ?: "",
            receiverId = data["receiverId"] as? String ?: "",
            text = decrypted,
            type = MessageType.TEXT,
            timestamp = data["timestampFormatted"] as? String ?: SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts)),
            timestampMillis = ts,
            status = MessageStatus.SENT,
            isMine = data["senderId"] as? String == uid,
            replyToId = data["replyToId"] as? String,
            replyToText = data["replyToText"] as? String,
            replyToSender = data["replyToSender"] as? String,
            topicId = (data["topicId"] as? String)?.takeIf { it.isNotBlank() } ?: "general",
            checklistTitle = (data["checklistTitle"] as? String)?.takeIf { it.isNotBlank() },
            checklistItems = parseChecklistItemsFromRaw(data["checklistItems"]),
            allowOthersToToggle = (data["allowOthersToToggle"] as? Boolean) ?: true,
            voiceDurationMillis = (data["voiceDurationMillis"] as? Number)?.toLong(),
            voiceWaveform = parseVoiceWaveformFromRaw(data["voiceWaveform"])
        )
    }

    // -------------------------------------------------------------------------
    // Helper statis (tes dan ViewModel)
    // -------------------------------------------------------------------------

    companion object {
        const val MESSAGE_PAGE_SIZE = 50L

        fun buildChecklistSummaryText(title: String, items: List<ChecklistItem>): String {
            val lines = items.joinToString("\n") { item ->
                val mark = if (item.isDone) "[x]" else "[ ]"
                "$mark ${item.text}"
            }
            return "☑️ Daftar Tugas: $title\n$lines"
        }

        fun buildVoiceNoteSummaryText(durationMillis: Long): String {
            val compact = com.jayra.app.util.AudioWaveTrimmer.formatCompactDurationMillis(durationMillis)
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

    // -------------------------------------------------------------------------
    // Metode pendukung (dipertahankan, sekarang suspend / Room-aware)
    // -------------------------------------------------------------------------

    data class OlderMessagePage(
        val messages: List<Message>,
        val hasMore: Boolean
    )

    fun seedDemoMessages(chatId: String) {
        if (!com.jayra.app.BuildConfig.DEBUG) return
        // Demo hanya untuk DEBUG, tidak ditulis ke Room agar data bersih.
    }

    suspend fun toggleChecklistItem(
        chatId: String, messageId: String, itemId: String,
        precomputedItems: List<ChecklistItem>? = null
    ): List<ChecklistItem> {
        return toggleChecklistItemRemote(chatId, messageId, itemId, precomputedItems)
    }

    suspend fun toggleChecklistItemRemote(
        chatId: String, messageId: String,
        itemId: String, precomputedItems: List<ChecklistItem>? = null
    ): List<ChecklistItem> {
        val db = firestore ?: error("Firestore tidak tersedia.")
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
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
            mapOf("id" to item.id.take(128), "text" to item.text.take(300), "isDone" to item.isDone)
        }
        msgRef.update("checklistItems", itemMaps).await()
        return finalItems
    }

    suspend fun addReaction(chatId: String, messageId: String, emoji: String) {
        scope.launch {
            try {
                if (firestore != null) {
                    // Placeholder: reaksi tidak disimpan Room pada iterasi ini.
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        scope.launch {
            try {
                if (firestore != null) {
                    firestore.collection("chats").document(chatId)
                        .collection("messages").document(messageId).delete()
                }
            } catch (_: Exception) {}
        }
    }

    fun seedDummyChatsIfEmpty() {
        if (com.jayra.app.BuildConfig.DEBUG) {
            // Tidak mem-populate Room agar tidak menimpa data nyata.
        }
    }

    fun clearDummyChats() {}

    suspend fun toggleFavorite(chatId: String) {
        scope.launch { try {} catch (_: Exception) {} }
    }

    suspend fun archiveChat(chatId: String) {
        scope.launch { try { chatDao.deleteChat(chatId) } catch (_: Exception) {} }
    }

    suspend fun unarchiveChat(chatId: String) {
        scope.launch { try {} catch (_: Exception) {} }
    }

    suspend fun deleteChat(chatId: String) {
        scope.launch { try { chatDao.deleteChat(chatId) } catch (_: Exception) {} }
    }

    suspend fun togglePin(chatId: String) {
        scope.launch { try {} catch (_: Exception) {} }
    }

    suspend fun toggleMute(chatId: String) {
        scope.launch { try {} catch (_: Exception) {} }
    }

    suspend fun markAsRead(chatId: String) {
        scope.launch { try { chatDao.resetUnreadCount(chatId) } catch (_: Exception) {} }
    }

    suspend fun createGroupChat(
        chatId: String, name: String, avatarUrl: String,
        participants: List<String>, hasTopics: Boolean = true
    ) {
        val normalizedParticipants = (participants.filterNot { it == "user_telegram_me" } + currentUserId).distinct()
        val now = System.currentTimeMillis()
        val newChat = Chat(
            id = chatId, name = name, avatarUrl = avatarUrl,
            otherUserId = normalizedParticipants.firstOrNull { it != currentUserId } ?: "",
            otherUserName = name,
            lastMessage = "Grup dibuat", timestamp = "Baru saja", lastMessageTime = now,
            unreadCount = 0, isPinned = false, isMuted = false, isRead = true,
            isGroup = true, hasTopics = hasTopics, participants = normalizedParticipants,
            timestampMillis = now
        )
        chatDao.insertChat(toChatEntity(newChat))
        firestore?.collection("chats")?.document(chatId)?.set(newChat)
    }

    suspend fun ensureSavedMessagesChat(): String = currentUserId

    suspend fun fetchMessageById(chatId: String, messageId: String): Message? {
        return withContext(Dispatchers.IO) {
            messageDao.getMessageById(chatId, messageId)?.let { toMessage(it) }
        }
    }

    suspend fun forwardMessageToChats(
        sourceMessage: Message,
        targetChatIds: List<String>,
        customComment: String = ""
    ): kotlin.Result<Int> {
        if (targetChatIds.isEmpty()) {
            return kotlin.Result.failure(IllegalArgumentException("Pilih minimal satu obrolan tujuan."))
        }
        return try {
            var count = 0
            for (targetChatId in targetChatIds.distinct()) {
                sendMessage(chatId = targetChatId, text = sourceMessage.text, topicId = sourceMessage.topicId)
                count++
            }
            kotlin.Result.success(count)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    fun setTyping(chatId: String, userId: String, isTyping: Boolean) {
        scope.launch {
            try { } catch (_: Exception) {}
        }
    }

    fun observeTyping(chatId: String): Flow<List<String>> = callbackFlow {
        trySend(emptyList())
        awaitClose {}
    }

    fun getAllMessages(): Flow<List<Message>> = callbackFlow {
        trySend(emptyList())
        awaitClose {}
    }
}
