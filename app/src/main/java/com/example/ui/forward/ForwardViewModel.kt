package com.example.ui.forward

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.di.AppModule
import com.example.model.Chat
import com.example.model.ForwardUiState
import com.example.model.Message
import com.example.model.MessageStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ForwardViewModel(
    private val chatRepository: ChatRepository = AppModule.chatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ForwardUiState>(ForwardUiState.Loading)
    val uiState: StateFlow<ForwardUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _selectedChatIds = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _comment = MutableStateFlow("")
    private val _isForwarding = MutableStateFlow(false)
    private var loadedSourceMessage: Message? = null

    fun loadForwardContext(sourceChatId: String, messageId: String) {
        _uiState.value = ForwardUiState.Loading
        viewModelScope.launch {
            val fetchedMessage = chatRepository.fetchMessageById(sourceChatId, messageId)
                ?: Message(
                    id = messageId,
                    chatId = sourceChatId,
                    senderId = chatRepository.currentUserId,
                    senderName = "Pengguna Jayra",
                    text = "Pesan terenkripsi",
                    timestamp = "Baru saja",
                    status = MessageStatus.SENT
                )
            loadedSourceMessage = fetchedMessage
            val personalSavedChatId = chatRepository.currentUserId

            combine(
                chatRepository.getChats(),
                _selectedChatIds,
                _searchQuery,
                _comment,
                _isForwarding
            ) { chats, selectedIds, query, commentText, forwarding ->
                val savedMessagesItem = Chat(
                    id = personalSavedChatId,
                    name = "Pesan Tersimpan",
                    lastMessage = "Simpan ke penyimpanan cloud pribadi",
                    timestamp = "Cloud",
                    isPinned = true,
                    participants = listOf(personalSavedChatId)
                )

                val mergedChats = buildList {
                    add(savedMessagesItem)
                    chats.filter { it.id != personalSavedChatId && !it.isArchived }
                        .forEach { add(it) }
                }

                val trimmed = query.trim()
                val filteredChats = if (trimmed.isEmpty()) {
                    mergedChats
                } else {
                    mergedChats.filter { chat ->
                        chat.name.contains(trimmed, ignoreCase = true) ||
                            chat.lastMessage.contains(trimmed, ignoreCase = true)
                    }
                }

                ForwardUiState.Success(
                    sourceMessage = fetchedMessage,
                    availableChats = filteredChats,
                    selectedChatIds = selectedIds,
                    searchQuery = query,
                    comment = commentText,
                    isForwarding = forwarding,
                    savedMessagesChatId = personalSavedChatId
                )
            }
                .catch { e ->
                    _uiState.value = ForwardUiState.Error(
                        e.localizedMessage ?: "Gagal memuat daftar obrolan tujuan"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onToggleChatSelection(chatId: String) {
        val current = _selectedChatIds.value
        _selectedChatIds.value = if (chatId in current) {
            current - chatId
        } else {
            current + chatId
        }
    }

    fun onClearSelection() {
        _selectedChatIds.value = emptySet()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCommentChange(comment: String) {
        _comment.value = comment
    }

    fun onConfirmForward(onForwardCompleted: (singleTargetChatId: String?) -> Unit) {
        val messageToForward = loadedSourceMessage ?: return
        val targets = _selectedChatIds.value.toList()
        if (targets.isEmpty() || _isForwarding.value) return

        _isForwarding.value = true
        viewModelScope.launch {
            val result = chatRepository.forwardMessageToChats(
                sourceMessage = messageToForward,
                targetChatIds = targets,
                customComment = _comment.value
            )
            _isForwarding.value = false
            result.fold(
                onSuccess = { count ->
                    _toastEvents.tryEmit(
                        if (count == 1) "Pesan berhasil diteruskan"
                        else "Pesan diteruskan ke $count obrolan"
                    )
                    val singleTarget = if (targets.size == 1) targets.first() else null
                    onForwardCompleted(singleTarget)
                },
                onFailure = { err ->
                    _toastEvents.tryEmit(
                        err.localizedMessage ?: "Gagal meneruskan pesan"
                    )
                }
            )
        }
    }
}
