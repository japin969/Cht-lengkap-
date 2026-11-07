package com.jayra.app.ui.viewmodel

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.ChatRepository
import com.jayra.app.data.ContactRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.Chat
import com.jayra.app.model.ChatRoomUiState
import com.jayra.app.model.ChatTopic
import com.jayra.app.model.Contact
import com.jayra.app.model.Message
import com.jayra.app.model.MessageStatus
import com.jayra.app.model.RichTextEditorConfig
import com.jayra.app.model.RichTextFormatOption
import com.jayra.app.security.EncryptionHelper
import com.jayra.app.util.LocalNotificationManager
import com.jayra.app.util.MarkdownParser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatRoomViewModel @JvmOverloads constructor(
    application: Application,
    private val chatRepository: ChatRepository = AppModule.chatRepository,
    private val contactRepository: ContactRepository = AppModule.contactRepository,
    private val firestore: FirebaseFirestore? = AppModule.provideFirestore(application),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ChatRoomUiState>(ChatRoomUiState.Loading)
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    private val _editorValue = MutableStateFlow(TextFieldValue(""))
    val editorValue: StateFlow<TextFieldValue> = _editorValue.asStateFlow()

    private val _editorConfig = MutableStateFlow(RichTextEditorConfig())
    val editorConfig: StateFlow<RichTextEditorConfig> = _editorConfig.asStateFlow()

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat.asStateFlow()

    private val _topics = MutableStateFlow(ChatTopic.DEFAULT_TOPICS)
    val topics: StateFlow<List<ChatTopic>> = _topics.asStateFlow()

    private val _selectedTopicId = MutableStateFlow(ChatTopic.GENERAL_TOPIC_ID)
    val selectedTopicId: StateFlow<String> = _selectedTopicId.asStateFlow()

    private var currentChatId: String = ""
    private var cachedContact: Contact? = null
    private var allMessagesForChat: List<Message> = emptyList()
    private var typingJob: Job? = null
    private var chatMetadataJob: Job? = null
    private var messagesCollectionJob: Job? = null
    private var simulatedResponseJob: Job? = null
    private var messageSnapshotListener: ListenerRegistration? = null
    private val notifiedMessageIds = mutableSetOf<String>()

    fun initChat(chatId: String) {
        if (currentChatId == chatId && (_uiState.value is ChatRoomUiState.Success || _uiState.value is ChatRoomUiState.Empty)) return
        currentChatId = chatId
        listenToTyping(chatId)
        _selectedTopicId.value = ChatTopic.GENERAL_TOPIC_ID
        _currentChat.value = chatRepository.getChatSnapshotById(chatId)
        observeChatMetadata(chatId)
        attachRealtimeBackgroundNotificationListener(chatId)
        loadMessages(chatId)
    }

    private fun observeChatMetadata(chatId: String) {
        chatMetadataJob?.cancel()
        chatMetadataJob = viewModelScope.launch {
            chatRepository.observeChat(chatId).collect { chat ->
                val previousTopicsEnabled = _currentChat.value?.let { it.isGroup && it.hasTopics } ?: false
                _currentChat.value = chat
                val nowTopicsEnabled = chat?.let { it.isGroup && it.hasTopics } ?: false
                if (previousTopicsEnabled != nowTopicsEnabled && allMessagesForChat.isNotEmpty()) {
                    applyTopicFilterAndEmitState()
                }
            }
        }
    }

    fun onSelectTopic(topicId: String) {
        if (_selectedTopicId.value == topicId) return
        _selectedTopicId.value = topicId
        applyTopicFilterAndEmitState()
    }

    /**
     * 17.2 Memasang addSnapshotListener pada koleksi pesan Firestore.
     * Jika ada pesan baru dari pengguna lain dan aplikasi sedang berada di background,
     * panggil LocalNotificationManager.showMessageNotification(...).
     */
    private fun attachRealtimeBackgroundNotificationListener(chatId: String) {
        messageSnapshotListener?.remove()
        if (firestore == null || chatId.isBlank()) return

        var isInitialSnapshot = true
        val appContext = getApplication<Application>().applicationContext

        try {
            messageSnapshotListener = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    if (isInitialSnapshot) {
                        isInitialSnapshot = false
                        snapshot.documents.forEach { notifiedMessageIds.add(it.id) }
                        return@addSnapshotListener
                    }

                    for (change in snapshot.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            if (!notifiedMessageIds.add(doc.id)) continue

                            val senderId = doc.getString("senderId") ?: ""
                            if (senderId.isNotBlank() && senderId != chatRepository.currentUserId) {
                                if (LocalNotificationManager.isAppInBackground(appContext)) {
                                    val cipherText = doc.getString("cipherText")
                                        ?: doc.getString("text")
                                        ?: ""
                                    val iv = doc.getString("iv") ?: ""
                                    val plainText = if (iv.isNotBlank()) {
                                        EncryptionHelper.decrypt(cipherText, iv)
                                    } else {
                                        cipherText
                                    }
                                    val fallbackSender = contactRepository.getContactById(chatId).name
                                    val senderName = doc.getString("senderName")
                                        ?.takeIf { it.isNotBlank() }
                                        ?: fallbackSender

                                    LocalNotificationManager.showMessageNotification(
                                        context = appContext,
                                        chatId = chatId,
                                        senderName = senderName,
                                        messageText = plainText,
                                        messageId = doc.id
                                    )
                                }
                            }
                        }
                    }
                }
        } catch (_: Exception) {
            // Firestore belum aktif atau mode lokal; fallback ditangani di koleksi pesan
        }
    }

    fun loadMessages(chatId: String) {
        _uiState.value = ChatRoomUiState.Loading
        val contact = contactRepository.getContactById(chatId)
        cachedContact = contact
        val appContext = getApplication<Application>().applicationContext
        var isFirstEmission = true

        messagesCollectionJob?.cancel()
        messagesCollectionJob = viewModelScope.launch {
            try {
                chatRepository.getMessages(chatId).collect { messages ->
                    if (isFirstEmission) {
                        isFirstEmission = false
                        messages.forEach { notifiedMessageIds.add(it.id) }
                    } else {
                        val newestForeignMessages = messages.filter { msg ->
                            msg.senderId != chatRepository.currentUserId &&
                                notifiedMessageIds.add(msg.id)
                        }
                        if (newestForeignMessages.isNotEmpty() &&
                            LocalNotificationManager.isAppInBackground(appContext)
                        ) {
                            newestForeignMessages.forEach { msg ->
                                LocalNotificationManager.showMessageNotification(
                                    context = appContext,
                                    chatId = chatId,
                                    senderName = msg.senderName.ifBlank { contact.name },
                                    messageText = msg.text,
                                    messageId = msg.id
                                )
                            }
                        }
                    }

                    allMessagesForChat = messages
                    applyTopicFilterAndEmitState()
                }
            } catch (e: Exception) {
                _uiState.value = ChatRoomUiState.Error(e.message ?: "Gagal memuat pesan")
            }
        }
    }

    private fun applyTopicFilterAndEmitState() {
        val chat = _currentChat.value
        val topicFilteringActive = chat?.isGroup == true && chat.hasTopics
        val activeTopic = _selectedTopicId.value
        val filtered = if (topicFilteringActive) {
            allMessagesForChat.filter { it.topicId == activeTopic }
        } else {
            allMessagesForChat
        }

        if (filtered.isEmpty()) {
            _uiState.value = ChatRoomUiState.Empty
        } else {
            val currentSuccess = _uiState.value as? ChatRoomUiState.Success
            val contact = cachedContact ?: contactRepository.getContactById(currentChatId)
            _uiState.value = ChatRoomUiState.Success(
                messages = filtered,
                isTyping = currentSuccess?.isTyping ?: false,
                replyTo = currentSuccess?.replyTo,
                contact = contact
            )
        }
    }

    fun onEditorValueChange(newValue: TextFieldValue) {
        val previousText = _editorValue.value.text
        _editorValue.value = newValue
        if (newValue.text != previousText && newValue.text.isNotBlank()) {
            onTyping()
        }
    }

    fun onApplyFormat(option: RichTextFormatOption) {
        _editorValue.update { current ->
            MarkdownParser.applyFormatting(current, option)
        }
    }

    fun onSendCurrentMessage() {
        val currentText = _editorValue.value.text
        if (currentText.isBlank()) return
        _editorValue.value = TextFieldValue("")
        onSendMessage(currentText)
    }

    fun onSendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        if (_editorValue.value.text == text) {
            _editorValue.value = TextFieldValue("")
        }

        val currentState = _uiState.value as? ChatRoomUiState.Success
        val replyToMsg = currentState?.replyTo
        val activeTopicId = if (_currentChat.value?.isGroup == true && _currentChat.value?.hasTopics == true) {
            _selectedTopicId.value
        } else {
            ChatTopic.GENERAL_TOPIC_ID
        }

        viewModelScope.launch {
            try {
                _uiState.update { state ->
                    if (state is ChatRoomUiState.Success) state.copy(replyTo = null) else state
                }

                chatRepository.sendMessage(
                    chatId = currentChatId,
                    text = trimmed,
                    replyTo = replyToMsg,
                    topicId = activeTopicId
                )

                simulatePeerInteraction(activeTopicId)
            } catch (_: Exception) {
                _uiState.update { state ->
                    if (state is ChatRoomUiState.Success) {
                        state.copy(
                            messages = state.messages.map {
                                if (it.text == trimmed) it.copy(status = MessageStatus.FAILED) else it
                            }
                        )
                    } else state
                }
            }
        }
    }

    private fun simulatePeerInteraction(topicId: String = ChatTopic.GENERAL_TOPIC_ID) {
        simulatedResponseJob?.cancel()
        simulatedResponseJob = viewModelScope.launch {
            delay(1200L)
            this@ChatRoomViewModel.setTyping(true)
            delay(2400L)
            this@ChatRoomViewModel.setTyping(false)

            val contactName = contactRepository.getContactById(currentChatId).name
            val replies = listOf(
                "Siap! Tampilan dan responsnya sangat mulus.",
                "Desain Jayra di Android terasa cepat dan presisi.",
                "Mantap! Enkripsi end-to-end berjalan sesuai harapan.",
                "Mari kita uji panggilan grup dan berbagi pesan selanjutnya!"
            )
            val randomReply = replies.random()
            val sent = chatRepository.sendMessage(
                chatId = currentChatId,
                text = randomReply,
                topicId = topicId
            )

            // Jika pengguna meminimalkan aplikasi sebelum balasan tiba, picu notifikasi lokal
            val appContext = getApplication<Application>().applicationContext
            if (LocalNotificationManager.isAppInBackground(appContext) &&
                notifiedMessageIds.add("peer_${sent.id}")
            ) {
                LocalNotificationManager.showMessageNotification(
                    context = appContext,
                    chatId = currentChatId,
                    senderName = contactName,
                    messageText = randomReply,
                    messageId = sent.id
                )
            }
        }
    }

    fun onTyping() {
        val chatId = (currentChat.value?.id ?: currentChatId ?: return)
        val uid = auth.currentUser?.uid ?: return
        
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            chatRepository.setTyping(chatId, uid, true)
            delay(3000L)
            chatRepository.setTyping(chatId, uid, false)
        }
    }

    // Listener diletakkan di init block ViewModel
    private fun listenToTyping(chatId: String) {
        viewModelScope.launch {
            chatRepository.observeTyping(chatId).collect { typingUsers ->
                val othersTyping = typingUsers.filter { it != auth.currentUser?.uid }
                _uiState.update { state ->
                    if (state is ChatRoomUiState.Success) state.copy(isTyping = othersTyping.isNotEmpty()) else state
                }
            }
        }
    }

    fun onReply(message: Message?) {
        _uiState.update { state ->
            if (state is ChatRoomUiState.Success) state.copy(replyTo = message) else state
        }
    }

    fun onReaction(messageId: String, emoji: String) {
        chatRepository.addReaction(currentChatId, messageId, emoji)
    }

    fun onToggleChecklistItem(messageId: String, itemId: String) {
        if (currentChatId.isBlank()) return
        viewModelScope.launch {
            chatRepository.toggleChecklistItem(currentChatId, messageId, itemId)
        }
    }

    fun onDelete(messageId: String) {
        chatRepository.deleteMessage(currentChatId, messageId)
    }

    fun onRetry() {
        if (currentChatId.isNotEmpty()) {
            loadMessages(currentChatId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageSnapshotListener?.remove()
        chatMetadataJob?.cancel()
        messagesCollectionJob?.cancel()
        typingJob?.cancel()
        simulatedResponseJob?.cancel()
    }
}
