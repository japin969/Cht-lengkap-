package com.example.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.di.AppModule
import com.example.model.Message
import com.example.model.SavedMessagesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SavedMessagesViewModel(
    private val chatRepository: ChatRepository = AppModule.chatRepository
) : ViewModel() {

    val savedChatId: String
        get() = chatRepository.currentUserId

    private val _uiState = MutableStateFlow<SavedMessagesUiState>(SavedMessagesUiState.Loading)
    val uiState: StateFlow<SavedMessagesUiState> = _uiState.asStateFlow()

    private val _replyTo = MutableStateFlow<Message?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)

    init {
        initializeSavedMessages()
    }

    fun initializeSavedMessages() {
        val targetChatId = savedChatId
        viewModelScope.launch {
            chatRepository.ensureSavedMessagesChat()
        }
        viewModelScope.launch {
            combine(
                chatRepository.getMessages(targetChatId),
                _replyTo,
                _searchQuery,
                _isSearchActive
            ) { messages, replyMsg, query, searchActive ->
                val trimmedQuery = query.trim()
                val filteredMessages = if (trimmedQuery.isEmpty()) {
                    messages
                } else {
                    messages.filter { msg ->
                        msg.text.contains(trimmedQuery, ignoreCase = true) ||
                            (msg.forwardedFrom?.contains(trimmedQuery, ignoreCase = true) == true)
                    }
                }
                SavedMessagesUiState.Success(
                    chatId = targetChatId,
                    headerTitle = "Pesan Tersimpan",
                    headerSubtitle = if (messages.isEmpty()) {
                        "Penyimpanan cloud pribadi"
                    } else {
                        "${messages.size} catatan tersimpan"
                    },
                    messages = filteredMessages,
                    replyTo = replyMsg,
                    searchQuery = query,
                    isSearchActive = searchActive
                )
            }
                .catch { e ->
                    _uiState.value = SavedMessagesUiState.Error(
                        e.localizedMessage ?: "Gagal memuat Pesan Tersimpan"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onSendNote(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val currentReply = _replyTo.value
        _replyTo.value = null
        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = savedChatId,
                text = trimmed,
                replyTo = currentReply
            )
        }
    }

    fun onReply(message: Message?) {
        _replyTo.value = message
    }

    fun onReaction(messageId: String, emoji: String) {
        chatRepository.addReaction(savedChatId, messageId, emoji)
    }

    fun onToggleChecklistItem(messageId: String, itemId: String) {
        viewModelScope.launch {
            chatRepository.toggleChecklistItem(savedChatId, messageId, itemId)
        }
    }

    fun onDelete(messageId: String) {
        chatRepository.deleteMessage(savedChatId, messageId)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onToggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }
}
