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
import kotlinx.coroutines.flow.map
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
                                .thenByDescending { it.timestampMillis }
                        )
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatListUiState.Loading)

    /**
     * Derived index of first unread chat in current uiState.chats list.
     * - Returns -1 if no unread or state not Success.
     * - Reactive to filter + sort changes (derived from uiState, not _rawChats).
     * - Contract for Designer: consume via collectAsStateWithLifecycle(),
     *   insert "Pesan belum dibaca" divider at this index in LazyColumn itemsIndexed.
     */
    val firstUnreadIndex: StateFlow<Int> = uiState
        .map { state ->
            when (state) {
                is ChatListUiState.Success -> state.chats.indexOfFirst { it.unreadCount > 0 }
                else -> -1
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

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

    val archivedChats: StateFlow<List<Chat>> = _rawChats
        .combine(StateFlowStub()) { chats, _ ->
            chats.filter { it.isArchived }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun StateFlowStub() = kotlinx.coroutines.flow.flowOf(Unit)

    fun onUnarchiveChat(chatId: String) {
        viewModelScope.launch {
            try {
                repository.unarchiveChat(chatId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
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
        viewModelScope.launch {
            try { repository.togglePin(chatId) } catch (_: Exception) {}
        }
    }

    fun onMuteToggle(chatId: String) {
        viewModelScope.launch {
            try { repository.toggleMute(chatId) } catch (_: Exception) {}
        }
    }

    fun onMarkAsRead(chatId: String) {
        viewModelScope.launch {
            try { repository.markAsRead(chatId) } catch (_: Exception) {}
        }
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

    fun toggleFavorite(chatId: String) {
        viewModelScope.launch {
            try { repository.toggleFavorite(chatId) } catch (_: Exception) {}
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
