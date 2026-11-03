package com.jayra.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.ChannelRepository
import com.jayra.app.data.ChatRepository
import com.jayra.app.data.ContactRepository
import com.jayra.app.data.GroupRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.Channel
import com.jayra.app.model.Chat
import com.jayra.app.model.Group
import com.jayra.app.model.Message
import com.jayra.app.model.SearchFilter
import com.jayra.app.model.SearchResult
import com.jayra.app.model.SearchResultType
import com.jayra.app.model.SearchUiState
import com.jayra.app.model.User
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

private data class SearchDataBundle(
    val chats: List<Chat>,
    val messages: List<Message>,
    val contacts: List<User>,
    val groups: List<Group>,
    val channels: List<Channel>
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val chatRepository: ChatRepository = AppModule.chatRepository,
    private val contactRepository: ContactRepository = AppModule.contactRepository,
    private val groupRepository: GroupRepository = AppModule.groupRepository,
    private val channelRepository: ChannelRepository = AppModule.channelRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SearchFilter.ALL)
    val selectedFilter: StateFlow<SearchFilter> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        observeSearch()
    }

    private fun observeSearch() {
        val queryAndFilterFlow = combine(
            _searchQuery.debounce(300L),
            _selectedFilter
        ) { query, filter ->
            Pair(query, filter)
        }

        val dataBundleFlow = combine(
            chatRepository.getChats(),
            chatRepository.getAllMessages(),
            contactRepository.getContacts(),
            groupRepository.getAllGroups(),
            channelRepository.getAllChannels()
        ) { chats, messages, contacts, groups, channels ->
            SearchDataBundle(chats, messages, contacts, groups, channels)
        }

        viewModelScope.launch {
            combine(queryAndFilterFlow, dataBundleFlow) { (query, filter), data ->
                val cleanQuery = query.trim()
                if (cleanQuery.isEmpty()) {
                    // Jayra displays recent chats / suggestions when query is empty
                    val suggestions = data.chats.take(5).map { chat ->
                        SearchResult(
                            id = chat.id,
                            title = chat.name,
                            subtitle = if (chat.isChannel) "Saluran" else if (chat.isGroup) "Grup" else chat.lastMessage,
                            timestamp = chat.timestamp,
                            type = if (chat.isChannel) SearchResultType.CHANNEL else if (chat.isGroup) SearchResultType.GROUP else SearchResultType.CHAT,
                            avatarUrl = chat.avatarUrl,
                            targetId = chat.id
                        )
                    }
                    return@combine SearchUiState.Success(suggestions)
                }

                val results = mutableListOf<SearchResult>()

                // 1. Obrolan (Chats)
                if (filter == SearchFilter.ALL || filter == SearchFilter.CHATS) {
                    data.chats.filter { chat ->
                        chat.name.contains(cleanQuery, ignoreCase = true) ||
                                chat.lastMessage.contains(cleanQuery, ignoreCase = true)
                    }.forEach { chat ->
                        results.add(
                            SearchResult(
                                id = "chat_${chat.id}",
                                title = chat.name,
                                subtitle = chat.lastMessage,
                                timestamp = chat.timestamp,
                                type = if (chat.isChannel) SearchResultType.CHANNEL else if (chat.isGroup) SearchResultType.GROUP else SearchResultType.CHAT,
                                avatarUrl = chat.avatarUrl,
                                targetId = chat.id
                            )
                        )
                    }
                }

                // 2. Pesan (Messages)
                if (filter == SearchFilter.ALL || filter == SearchFilter.MESSAGES) {
                    data.messages.filter { msg ->
                        msg.text.contains(cleanQuery, ignoreCase = true)
                    }.forEach { msg ->
                        results.add(
                            SearchResult(
                                id = "msg_${msg.id}",
                                title = msg.senderName,
                                subtitle = msg.text,
                                timestamp = msg.timestamp,
                                type = SearchResultType.MESSAGE,
                                avatarUrl = "",
                                targetId = msg.chatId
                            )
                        )
                    }
                }

                // 3. Kontak (Contacts)
                if (filter == SearchFilter.ALL || filter == SearchFilter.CONTACTS) {
                    data.contacts.filter { contact ->
                        contact.name.contains(cleanQuery, ignoreCase = true) ||
                                contact.username.contains(cleanQuery, ignoreCase = true) ||
                                contact.phoneNumber.contains(cleanQuery, ignoreCase = true)
                    }.forEach { contact ->
                        results.add(
                            SearchResult(
                                id = "user_${contact.id}",
                                title = contact.name,
                                subtitle = if (contact.username.isNotEmpty()) "@${contact.username}" else contact.phoneNumber,
                                timestamp = contact.lastSeenFormatted,
                                type = SearchResultType.CONTACT,
                                avatarUrl = contact.avatarUrl,
                                targetId = contact.id
                            )
                        )
                    }
                }

                // 4. Grup (Groups)
                if (filter == SearchFilter.ALL || filter == SearchFilter.GROUPS) {
                    data.groups.filter { grp ->
                        grp.name.contains(cleanQuery, ignoreCase = true) ||
                                grp.description.contains(cleanQuery, ignoreCase = true)
                    }.forEach { grp ->
                        results.add(
                            SearchResult(
                                id = "grp_${grp.id}",
                                title = grp.name,
                                subtitle = "${grp.memberCount} anggota",
                                timestamp = "",
                                type = SearchResultType.GROUP,
                                avatarUrl = grp.avatarUrl,
                                memberCount = grp.memberCount,
                                targetId = grp.id
                            )
                        )
                    }
                }

                // 5. Saluran (Channels)
                if (filter == SearchFilter.ALL || filter == SearchFilter.CHANNELS) {
                    data.channels.filter { ch ->
                        ch.name.contains(cleanQuery, ignoreCase = true) ||
                                ch.description.contains(cleanQuery, ignoreCase = true)
                    }.forEach { ch ->
                        results.add(
                            SearchResult(
                                id = "ch_${ch.id}",
                                title = ch.name,
                                subtitle = "${ch.subscriberCount} pelanggan",
                                timestamp = "",
                                type = SearchResultType.CHANNEL,
                                avatarUrl = ch.avatarUrl,
                                targetId = ch.id
                            )
                        )
                    }
                }

                val distinctResults = results.distinctBy { "${it.type}_${it.targetId}_${it.subtitle}" }
                if (distinctResults.isEmpty()) {
                    SearchUiState.Empty
                } else {
                    SearchUiState.Success(distinctResults)
                }
            }.collect { state ->
                _uiState.value = state
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.isNotBlank()) {
            _isLoading.value = true
        }
    }

    fun onFilterChange(filter: SearchFilter) {
        _selectedFilter.value = filter
    }

    fun onClearQuery() {
        _searchQuery.value = ""
        _isLoading.value = false
    }

    fun onRetry() {
        val q = _searchQuery.value
        onSearchQueryChange(q)
    }
}
