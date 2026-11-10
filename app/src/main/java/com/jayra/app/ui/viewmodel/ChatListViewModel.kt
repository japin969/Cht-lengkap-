package com.jayra.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.ChatRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.Chat
import com.jayra.app.model.ChatFilter
import com.jayra.app.model.ChatListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository = AppModule.chatRepository
) : ViewModel() {

    private val _rawChats = MutableStateFlow<List<Chat>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(ChatFilter.ALL)

    val uiState: StateFlow<ChatListUiState> = combine(
        _rawChats,
        searchQuery,
        selectedFilter,
        _isLoading,
        _errorMessage
    ) { chats, query, filter, loading, error ->
        when {
            loading -> ChatListUiState.Loading
            error != null -> ChatListUiState.Error(error)
            chats.isEmpty() -> ChatListUiState.Empty
            else -> {
                val nonArchived = chats.filter { !it.isArchived }
                val filteredByTab = when (filter) {
                    ChatFilter.ALL -> nonArchived
                    ChatFilter.UNREAD -> nonArchived.filter { it.unreadCount > 0 || !it.isRead }
                    ChatFilter.FAVORITES -> nonArchived.filter { it.isFavorite || it.isPinned }
                    ChatFilter.GROUPS -> nonArchived.filter { it.isGroup }
                    ChatFilter.CHANNELS -> nonArchived.filter { it.isChannel }
                    ChatFilter.DRAFTS -> nonArchived.filter { it.hasDraft }
                }
                val filtered = if (query.isBlank()) {
                    filteredByTab
                } else {
                    filteredByTab.filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.lastMessage.contains(query, ignoreCase = true)
                    }
                }
                if (filtered.isEmpty()) {
                    ChatListUiState.Empty
                } else {
                    ChatListUiState.Success(
                        filtered.sortedWith(
                            compareByDescending<Chat> { it.isPinned }
                                .thenByDescending { it.timestamp }
                        )
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatListUiState.Loading)

    init {
        loadChats()
    }

    fun loadChats() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                repository.getChats().collect { list ->
                    _rawChats.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load chats"
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onFilterChange(filter: ChatFilter) {
        selectedFilter.value = filter
    }

    fun onSwipeArchive(chatId: String) {
        viewModelScope.launch {
            try {
                repository.archiveChat(chatId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun onSwipeDelete(chatId: String) {
        viewModelScope.launch {
            try {
                repository.deleteChat(chatId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun onPinToggle(chatId: String) {
        repository.togglePin(chatId)
    }

    fun onMuteToggle(chatId: String) {
        repository.toggleMute(chatId)
    }

    fun onMarkAsRead(chatId: String) {
        repository.markAsRead(chatId)
    }

    fun onRetry() {
        loadChats()
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            try { repository.deleteChat(chatId) } catch (_: Exception) {}
        }
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            try { repository.archiveChat(chatId) } catch (_: Exception) {}
        }
    }

    fun togglePin(chatId: String) {
        viewModelScope.launch {
            try { repository.togglePin(chatId) } catch (_: Exception) {}
        }
    }

    fun toggleMute(chatId: String) {
        viewModelScope.launch {
            try { repository.toggleMute(chatId) } catch (_: Exception) {}
        }
    }
}
