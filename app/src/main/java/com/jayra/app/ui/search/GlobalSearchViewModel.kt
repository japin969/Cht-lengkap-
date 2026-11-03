package com.jayra.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.ChannelRepository
import com.jayra.app.data.ChatRepository
import com.jayra.app.data.ContactRepository
import com.jayra.app.data.GroupRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.FileInfo
import com.jayra.app.model.GlobalSearchStrings
import com.jayra.app.model.GlobalSearchUiState
import com.jayra.app.model.SearchFilter
import com.jayra.app.model.SearchResult
import com.jayra.app.model.SearchResultType
import com.jayra.app.security.EncryptionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlobalSearchViewModel(
    private val firestoreProvider: () -> FirebaseFirestore? = { AppModule.firestore },
    private val authProvider: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    },
    private val chatRepository: ChatRepository = AppModule.chatRepository,
    private val contactRepository: ContactRepository = AppModule.contactRepository,
    private val groupRepository: GroupRepository = AppModule.groupRepository,
    private val channelRepository: ChannelRepository = AppModule.channelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GlobalSearchUiState>(GlobalSearchUiState.Idle)
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SearchFilter.ALL)
    val selectedFilter: StateFlow<SearchFilter> = _selectedFilter.asStateFlow()

    private val _uiStrings = MutableStateFlow(GlobalSearchStrings())
    val uiStrings: StateFlow<GlobalSearchStrings> = _uiStrings.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(rawQuery: String) {
        val cappedRaw = rawQuery.take(MAX_QUERY_LENGTH)
        _searchQuery.value = cappedRaw

        searchJob?.cancel()

        val sanitized = sanitizeSearchQuery(cappedRaw)
        if (sanitized.isBlank()) {
            _uiState.value = GlobalSearchUiState.Idle
            return
        }

        _uiState.value = GlobalSearchUiState.Loading
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS)
            executeGlobalSearch(sanitized)
        }
    }

    fun onFilterChange(filter: SearchFilter) {
        _selectedFilter.value = filter
    }

    fun onClearQuery() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _uiState.value = GlobalSearchUiState.Idle
    }

    fun onRetry() {
        val currentSanitized = sanitizeSearchQuery(_searchQuery.value)
        if (currentSanitized.isBlank()) {
            _uiState.value = GlobalSearchUiState.Idle
            return
        }
        searchJob?.cancel()
        _uiState.value = GlobalSearchUiState.Loading
        searchJob = viewModelScope.launch {
            executeGlobalSearch(currentSanitized)
        }
    }

    private suspend fun executeGlobalSearch(sanitizedQuery: String) {
        try {
            val combinedResults = withContext(Dispatchers.IO) {
                coroutineScope {
                    val uid = authProvider()?.currentUser?.uid
                    val db = firestoreProvider()

                    val chatsAndGroupsDeferred = async {
                        searchChatsGroupsAndChannels(db, uid, sanitizedQuery)
                    }
                    val messagesDeferred = async {
                        searchMessages(db, uid, sanitizedQuery)
                    }
                    val contactsDeferred = async {
                        searchContacts(db, uid, sanitizedQuery)
                    }
                    val filesDeferred = async {
                        searchFiles(db, uid, sanitizedQuery)
                    }

                    val merged = buildList {
                        addAll(chatsAndGroupsDeferred.await())
                        addAll(messagesDeferred.await())
                        addAll(contactsDeferred.await())
                        addAll(filesDeferred.await())
                    }

                    merged.distinctBy { "${it.type}_${it.targetId}_${it.title}_${it.subtitle}" }
                }
            }

            _uiState.value = if (combinedResults.isEmpty()) {
                GlobalSearchUiState.Empty
            } else {
                GlobalSearchUiState.Success(combinedResults)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            _uiState.value = GlobalSearchUiState.Error(
                e.localizedMessage ?: "Terjadi kesalahan saat mencari data"
            )
        }
    }

    private suspend fun searchChatsGroupsAndChannels(
        db: FirebaseFirestore?,
        uid: String?,
        query: String
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // 1. Firestore participant-scoped chats, groups, and channels
        if (db != null && !uid.isNullOrBlank()) {
            runCatching {
                val chatSnap = db.collection("chats")
                    .whereArrayContains("participants", uid)
                    .get()
                    .await()

                for (doc in chatSnap.documents) {
                    val name = doc.getString("name").orEmpty()
                    val lastMessage = doc.getString("lastMessage").orEmpty()
                    if (name.contains(query, ignoreCase = true) ||
                        lastMessage.contains(query, ignoreCase = true)
                    ) {
                        val isChannel = doc.getBoolean("isChannel") ?: false
                        val isGroup = doc.getBoolean("isGroup") ?: false
                        val chatId = doc.getString("id")?.takeIf { it.isNotBlank() } ?: doc.id
                        results.add(
                            SearchResult(
                                id = "fs_chat_$chatId",
                                title = name,
                                subtitle = lastMessage.ifBlank {
                                    when {
                                        isChannel -> "Saluran"
                                        isGroup -> "Grup"
                                        else -> "Obrolan"
                                    }
                                },
                                timestamp = doc.getString("timestamp").orEmpty(),
                                type = when {
                                    isChannel -> SearchResultType.CHANNEL
                                    isGroup -> SearchResultType.GROUP
                                    else -> SearchResultType.CHAT
                                },
                                avatarUrl = doc.getString("avatarUrl").orEmpty(),
                                targetId = chatId
                            )
                        )
                    }
                }
            }

            runCatching {
                val groupSnap = db.collection("groups")
                    .whereArrayContains("members", uid)
                    .get()
                    .await()

                for (doc in groupSnap.documents) {
                    val name = doc.getString("name").orEmpty()
                    val desc = doc.getString("description").orEmpty()
                    if (name.contains(query, ignoreCase = true) ||
                        desc.contains(query, ignoreCase = true)
                    ) {
                        val members = (doc.get("members") as? List<*>)?.size ?: 0
                        results.add(
                            SearchResult(
                                id = "fs_group_${doc.id}",
                                title = name,
                                subtitle = desc.ifBlank { "$members anggota" },
                                timestamp = "",
                                type = SearchResultType.GROUP,
                                avatarUrl = doc.getString("avatarUrl").orEmpty(),
                                memberCount = members,
                                targetId = doc.id
                            )
                        )
                    }
                }
            }

            runCatching {
                val channelSnap = db.collection("channels")
                    .whereArrayContains("subscribers", uid)
                    .get()
                    .await()

                for (doc in channelSnap.documents) {
                    val name = doc.getString("name").orEmpty()
                    val desc = doc.getString("description").orEmpty()
                    if (name.contains(query, ignoreCase = true) ||
                        desc.contains(query, ignoreCase = true)
                    ) {
                        val subscribers = (doc.get("subscribers") as? List<*>)?.size ?: 0
                        results.add(
                            SearchResult(
                                id = "fs_channel_${doc.id}",
                                title = name,
                                subtitle = desc.ifBlank { "$subscribers pelanggan" },
                                timestamp = "",
                                type = SearchResultType.CHANNEL,
                                avatarUrl = "",
                                memberCount = subscribers,
                                targetId = doc.id
                            )
                        )
                    }
                }
            }
        }

        // 2. Repository state sync (ensures instant results for active session chats/groups/channels)
        runCatching {
            val localChats = chatRepository.getChats().first()
            localChats.filter { chat ->
                chat.name.contains(query, ignoreCase = true) ||
                    chat.lastMessage.contains(query, ignoreCase = true)
            }.forEach { chat ->
                results.add(
                    SearchResult(
                        id = "repo_chat_${chat.id}",
                        title = chat.name,
                        subtitle = chat.lastMessage,
                        timestamp = chat.timestamp,
                        type = when {
                            chat.isChannel -> SearchResultType.CHANNEL
                            chat.isGroup -> SearchResultType.GROUP
                            else -> SearchResultType.CHAT
                        },
                        avatarUrl = chat.avatarUrl,
                        targetId = chat.id
                    )
                )
            }
        }

        runCatching {
            val groups = groupRepository.getAllGroups().first()
            groups.filter { grp ->
                grp.name.contains(query, ignoreCase = true) ||
                    grp.description.contains(query, ignoreCase = true)
            }.forEach { grp ->
                results.add(
                    SearchResult(
                        id = "repo_grp_${grp.id}",
                        title = grp.name,
                        subtitle = grp.description.ifBlank { "${grp.memberCount} anggota" },
                        timestamp = "",
                        type = SearchResultType.GROUP,
                        avatarUrl = grp.avatarUrl,
                        memberCount = grp.memberCount,
                        targetId = grp.id
                    )
                )
            }
        }

        runCatching {
            val channels = channelRepository.getAllChannels().first()
            channels.filter { ch ->
                ch.name.contains(query, ignoreCase = true) ||
                    ch.description.contains(query, ignoreCase = true)
            }.forEach { ch ->
                results.add(
                    SearchResult(
                        id = "repo_ch_${ch.id}",
                        title = ch.name,
                        subtitle = ch.description.ifBlank { "${ch.subscriberCount} pelanggan" },
                        timestamp = "",
                        type = SearchResultType.CHANNEL,
                        avatarUrl = ch.avatarUrl,
                        memberCount = ch.subscriberCount,
                        targetId = ch.id
                    )
                )
            }
        }

        return results
    }

    private suspend fun searchMessages(
        db: FirebaseFirestore?,
        uid: String?,
        query: String
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val chatAvatarLookup = mutableMapOf<String, String>()
        val chatNameLookup = mutableMapOf<String, String>()

        runCatching {
            chatRepository.getChats().first().forEach { chat ->
                chatAvatarLookup[chat.id] = chat.avatarUrl
                chatNameLookup[chat.id] = chat.name
            }
        }

        // 1. Firestore participant-scoped chat message subcollections (decrypted E2EE)
        if (db != null && !uid.isNullOrBlank()) {
            runCatching {
                val chatSnap = db.collection("chats")
                    .whereArrayContains("participants", uid)
                    .get()
                    .await()

                coroutineScope {
                    chatSnap.documents.map { chatDoc ->
                        async {
                            val chatId = chatDoc.getString("id")?.takeIf { it.isNotBlank() } ?: chatDoc.id
                            val chatAvatar = chatDoc.getString("avatarUrl").orEmpty()
                            val chatTitle = chatDoc.getString("name").orEmpty()
                            val msgSnap = runCatching {
                                db.collection("chats")
                                    .document(chatDoc.id)
                                    .collection("messages")
                                    .whereArrayContains("participants", uid)
                                    .limit(50)
                                    .get()
                                    .await()
                            }.getOrNull()

                            val matchedInChat = mutableListOf<SearchResult>()
                            msgSnap?.documents?.forEach { msgDoc ->
                                val cipherText = msgDoc.getString("cipherText").orEmpty()
                                val iv = msgDoc.getString("iv").orEmpty()
                                val plainText = decryptMessageSafely(cipherText, iv)
                                val senderName = msgDoc.getString("senderName")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: chatTitle.ifBlank { "Pesan" }

                                if (plainText.contains(query, ignoreCase = true) ||
                                    senderName.contains(query, ignoreCase = true)
                                ) {
                                    matchedInChat.add(
                                        SearchResult(
                                            id = "fs_msg_${chatDoc.id}_${msgDoc.id}",
                                            title = senderName,
                                            subtitle = plainText,
                                            timestamp = msgDoc.getString("timestampFormatted").orEmpty(),
                                            type = SearchResultType.MESSAGE,
                                            avatarUrl = chatAvatar.ifBlank {
                                                chatAvatarLookup[chatId].orEmpty()
                                            },
                                            targetId = chatId
                                        )
                                    )
                                }
                            }
                            matchedInChat
                        }
                    }.awaitAll().flatten().let { results.addAll(it) }
                }
            }
        }

        // 2. Repository messages
        runCatching {
            val allMessages = chatRepository.getAllMessages().first()
            allMessages.filter { msg ->
                msg.text.contains(query, ignoreCase = true) ||
                    msg.senderName.contains(query, ignoreCase = true)
            }.forEach { msg ->
                val resolvedTitle = msg.senderName.ifBlank {
                    chatNameLookup[msg.chatId] ?: "Pesan"
                }
                results.add(
                    SearchResult(
                        id = "repo_msg_${msg.id}",
                        title = resolvedTitle,
                        subtitle = msg.text,
                        timestamp = msg.timestamp,
                        type = SearchResultType.MESSAGE,
                        avatarUrl = chatAvatarLookup[msg.chatId].orEmpty(),
                        targetId = msg.chatId.ifBlank { "chat_1" }
                    )
                )
            }
        }

        return results
    }

    private suspend fun searchContacts(
        db: FirebaseFirestore?,
        uid: String?,
        query: String
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // 1. Authenticated user profile in Firestore (/users/{uid})
        if (db != null && !uid.isNullOrBlank()) {
            runCatching {
                val userDoc = db.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    val name = userDoc.getString("name").orEmpty()
                    val username = userDoc.getString("username").orEmpty()
                    val phone = userDoc.getString("phoneNumber").orEmpty()
                    if (name.contains(query, ignoreCase = true) ||
                        username.contains(query, ignoreCase = true) ||
                        phone.contains(query, ignoreCase = true)
                    ) {
                        results.add(
                            SearchResult(
                                id = "fs_user_$uid",
                                title = name.ifBlank { username.ifBlank { phone } },
                                subtitle = "online",
                                timestamp = "online",
                                type = SearchResultType.CONTACT,
                                avatarUrl = userDoc.getString("avatarUrl").orEmpty(),
                                targetId = uid,
                                isOnline = true
                            )
                        )
                    }
                }
            }
        }

        // 2. ContactRepository contacts
        runCatching {
            val contacts = contactRepository.getContacts().first()
            contacts.filter { user ->
                user.name.contains(query, ignoreCase = true) ||
                    user.username.contains(query, ignoreCase = true) ||
                    user.phoneNumber.contains(query, ignoreCase = true)
            }.forEach { user ->
                val statusLabel = if (user.isOnline) {
                    "online"
                } else {
                    user.lastSeenFormatted.ifBlank { "last seen recently" }
                }
                results.add(
                    SearchResult(
                        id = "repo_contact_${user.id}",
                        title = user.name,
                        subtitle = statusLabel,
                        timestamp = statusLabel,
                        type = SearchResultType.CONTACT,
                        avatarUrl = user.avatarUrl,
                        targetId = user.id,
                        isOnline = user.isOnline
                    )
                )
            }
        }

        return results
    }

    private suspend fun searchFiles(
        db: FirebaseFirestore?,
        uid: String?,
        query: String
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // 1. Extract shared files/attachments referenced in Firestore chat messages
        if (db != null && !uid.isNullOrBlank()) {
            runCatching {
                val chatSnap = db.collection("chats")
                    .whereArrayContains("participants", uid)
                    .get()
                    .await()

                for (chatDoc in chatSnap.documents) {
                    val chatId = chatDoc.getString("id")?.takeIf { it.isNotBlank() } ?: chatDoc.id
                    val msgSnap = runCatching {
                        db.collection("chats")
                            .document(chatDoc.id)
                            .collection("messages")
                            .whereArrayContains("participants", uid)
                            .limit(30)
                            .get()
                            .await()
                    }.getOrNull()

                    msgSnap?.documents?.forEach { msgDoc ->
                        val plain = decryptMessageSafely(
                            msgDoc.getString("cipherText").orEmpty(),
                            msgDoc.getString("iv").orEmpty()
                        )
                        val extracted = extractFileNameIfPresent(plain)
                        if (extracted != null && extracted.contains(query, ignoreCase = true)) {
                            results.add(
                                SearchResult(
                                    id = "fs_file_${chatDoc.id}_${msgDoc.id}",
                                    title = extracted,
                                    subtitle = estimateReadableSize(plain),
                                    timestamp = msgDoc.getString("timestampFormatted").orEmpty(),
                                    type = SearchResultType.FILE,
                                    targetId = chatId
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Extract shared files from repository messages
        runCatching {
            val allMessages = chatRepository.getAllMessages().first()
            allMessages.forEach { msg ->
                val extracted = extractFileNameIfPresent(msg.text)
                if (extracted != null && extracted.contains(query, ignoreCase = true)) {
                    results.add(
                        SearchResult(
                            id = "repo_msg_file_${msg.id}",
                            title = extracted,
                            subtitle = estimateReadableSize(msg.text),
                            timestamp = msg.timestamp,
                            type = SearchResultType.FILE,
                            targetId = msg.chatId.ifBlank { "document" }
                        )
                    )
                }
            }
        }

        // 3. Indexed Jayra media & document storage catalog (synced with StorageDetail categories)
        val todayFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val indexedFiles = listOf(
            IndexedStorageFile("file_doc_manual", "Jayra_Manual.pdf", "1.8 MB", todayFormatted, "document"),
            IndexedStorageFile("file_doc_notes", "Notes_Project.docx", "540 KB", todayFormatted, "document"),
            IndexedStorageFile("file_doc_arch", "Jayra_Architecture_Spec.pdf", "2.4 MB", todayFormatted, "document"),
            IndexedStorageFile("file_img_001", "IMG_20261102_001.jpg", "3.2 MB", todayFormatted, "photo"),
            IndexedStorageFile("file_img_042", "IMG_20261101_042.png", "2.8 MB", todayFormatted, "photo"),
            IndexedStorageFile("file_vid_102", "VID_20261028_102.mp4", "45.0 MB", todayFormatted, "video"),
            IndexedStorageFile("file_aud_starboy", "Track_Starboy_Offline.mp3", "8.5 MB", todayFormatted, "music"),
            IndexedStorageFile("file_aud_blinding", "Track_BlindingLights.mp3", "7.2 MB", todayFormatted, "music")
        )

        indexedFiles.filter { item ->
            val info = FileInfo(
                id = item.id,
                name = item.name,
                size = item.size,
                date = item.date,
                path = item.category
            )
            info.name.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)
        }.forEach { item ->
            results.add(
                SearchResult(
                    id = item.id,
                    title = item.name,
                    subtitle = "${item.size} • ${item.date}",
                    timestamp = item.size,
                    type = SearchResultType.FILE,
                    targetId = item.category
                )
            )
        }

        return results
    }

    private fun decryptMessageSafely(cipherText: String, iv: String): String {
        if (cipherText.isBlank()) return ""
        if (iv.isBlank()) return cipherText
        return try {
            EncryptionHelper.decrypt(cipherText, iv)
        } catch (_: Exception) {
            cipherText
        }
    }

    private fun extractFileNameIfPresent(text: String): String? {
        val fileRegex = Regex("""([A-Za-z0-9_\-]+\.(pdf|docx?|xlsx?|pptx?|zip|rar|apk|txt|csv|jpg|jpeg|png|mp4|mp3))""", RegexOption.IGNORE_CASE)
        return fileRegex.find(text)?.value
    }

    private fun estimateReadableSize(text: String): String {
        val sizeBytes = (text.length.coerceAtLeast(1) * 128L).coerceAtLeast(1024L)
        val kb = sizeBytes / 1024.0
        return if (kb >= 1024.0) {
            String.format(Locale.US, "%.1f MB", kb / 1024.0)
        } else {
            String.format(Locale.US, "%.0f KB", kb)
        }
    }

    private data class IndexedStorageFile(
        val id: String,
        val name: String,
        val size: String,
        val date: String,
        val category: String
    )

    companion object {
        const val DEBOUNCE_MILLIS = 300L
        const val MAX_QUERY_LENGTH = 100

        /**
         * Sanitasi query untuk mencegah karakter injeksi & karakter kontrol tidak valid
         * sebelum dikirimkan ke query Firebase Firestore maupun pencocokan teks.
         */
        fun sanitizeSearchQuery(rawQuery: String): String {
            if (rawQuery.isBlank()) return ""
            return rawQuery
                .replace(Regex("""[\u0000-\u001F\u007F<>"';\\`]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .take(MAX_QUERY_LENGTH)
        }

        fun matchesFilter(result: SearchResult, filter: SearchFilter): Boolean {
            return when (filter) {
                SearchFilter.ALL -> true
                SearchFilter.CHATS -> {
                    result.type == SearchResultType.CHAT ||
                        result.type == SearchResultType.GROUP ||
                        result.type == SearchResultType.CHANNEL
                }
                SearchFilter.MESSAGES -> result.type == SearchResultType.MESSAGE
                SearchFilter.CONTACTS -> result.type == SearchResultType.CONTACT
                SearchFilter.FILES -> result.type == SearchResultType.FILE
                SearchFilter.GROUPS -> result.type == SearchResultType.GROUP
                SearchFilter.CHANNELS -> result.type == SearchResultType.CHANNEL
            }
        }
    }
}
