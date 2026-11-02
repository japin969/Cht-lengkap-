package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.ContactRepository
import com.example.di.AppModule
import com.example.model.Contact
import com.example.model.Message
import com.example.model.ChatRoomUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatRoomViewModel(
    private val chatRepository: ChatRepository = AppModule.chatRepository,
    private val contactRepository: ContactRepository = AppModule.contactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatRoomUiState>(ChatRoomUiState.Loading)
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    private var currentChatId: String = ""
    private var typingJob: Job? = null
    private var simulatedResponseJob: Job? = null

    fun initChat(chatId: String) {
        currentChatId = chatId
        loadMessages(chatId)
    }

    fun loadMessages(chatId: String) {
        _uiState.value = ChatRoomUiState.Loading
        val contact = contactRepository.getContactById(chatId)

        viewModelScope.launch {
            try {
                chatRepository.getMessages(chatId).collect { messages ->
                    if (messages.isEmpty()) {
                        _uiState.value = ChatRoomUiState.Empty
                    } else {
                        val currentSuccess = _uiState.value as? ChatRoomUiState.Success
                        _uiState.value = ChatRoomUiState.Success(
                            messages = messages,
                            isTyping = currentSuccess?.isTyping ?: false,
                            replyTo = currentSuccess?.replyTo,
                            contact = contact
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ChatRoomUiState.Error(e.message ?: "Failed to load messages")
            }
        }
    }

    fun onSendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val currentState = _uiState.value as? ChatRoomUiState.Success
        val replyToMsg = currentState?.replyTo

        viewModelScope.launch {
            try {
                // Clear reply state
                _uiState.update { state ->
                    if (state is ChatRoomUiState.Success) state.copy(replyTo = null) else state
                }

                chatRepository.sendMessage(
                    chatId = currentChatId,
                    text = trimmed,
                    replyTo = replyToMsg
                )

                // Simulate realistic peer typing and auto-response in active chat demo
                simulatePeerInteraction()
            } catch (e: Exception) {
                _uiState.update { state ->
                    if (state is ChatRoomUiState.Success) {
                        state.copy(
                            messages = state.messages.map {
                                if (it.text == trimmed) it.copy(status = com.example.model.MessageStatus.FAILED) else it
                            }
                        )
                    } else state
                }
            }
        }
    }

    private fun simulatePeerInteraction() {
        simulatedResponseJob?.cancel()
        simulatedResponseJob = viewModelScope.launch {
            delay(1200L)
            // Show typing indicator
            setTyping(true)
            delay(2400L)
            setTyping(false)

            // Send intelligent context-aware reply
            val replies = listOf(
                "Got it! That looks really crisp and fast.",
                "Telegram's design on Android is so well thought out.",
                "Awesome! The end-to-end encryption is working as expected.",
                "Let's test reactions and media sharing next!"
            )
            val randomReply = replies.random()
            chatRepository.sendMessage(
                chatId = currentChatId,
                text = randomReply
            )
        }
    }

    fun onTyping() {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            // Typing throttle
            delay(2000L)
        }
    }

    fun setTyping(isTyping: Boolean) {
        _uiState.update { state ->
            if (state is ChatRoomUiState.Success) state.copy(isTyping = isTyping) else state
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

    fun onDelete(messageId: String) {
        chatRepository.deleteMessage(currentChatId, messageId)
    }

    fun onRetry() {
        if (currentChatId.isNotEmpty()) {
            loadMessages(currentChatId)
        }
    }
}
