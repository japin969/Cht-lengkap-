package com.jayra.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.ChatRepository
import com.jayra.app.data.ContactRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.Contact
import com.jayra.app.model.ContactMessage
import com.jayra.app.model.ContactSharingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactSharingViewModel(
    private val contactRepository: ContactRepository = AppModule.contactRepository,
    private val chatRepository: ChatRepository = AppModule.chatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactSharingUiState>(ContactSharingUiState.Loading)
    val uiState: StateFlow<ContactSharingUiState> = _uiState.asStateFlow()

    private var allContacts: List<Contact> = emptyList()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = ContactSharingUiState.Loading
            try {
                contactRepository.getContacts().collect { userList ->
                    val contacts = userList.map { u ->
                        Contact(
                            id = u.id,
                            name = u.name,
                            phone = u.phoneNumber,
                            avatarUrl = u.avatarUrl,
                            status = u.lastSeenFormatted,
                            isOnline = u.isOnline
                        )
                    }
                    allContacts = contacts
                    filterContacts(_searchQuery.value)
                }
            } catch (e: Exception) {
                _uiState.value = ContactSharingUiState.Error(e.message ?: "Gagal memuat kontak")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterContacts(query)
    }

    private fun filterContacts(query: String) {
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            allContacts
        } else {
            allContacts.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                it.phone.contains(trimmed, ignoreCase = true)
            }
        }

        _uiState.value = if (filtered.isEmpty()) {
            ContactSharingUiState.Empty
        } else {
            ContactSharingUiState.Success(contacts = filtered, searchQuery = query)
        }
    }

    fun shareContact(chatId: String, contact: Contact, onSuccess: () -> Unit) {
        if (_isSharing.value) return
        _isSharing.value = true

        viewModelScope.launch {
            try {
                val messageText = "👤 [Kontak] ${contact.name}\n${contact.phone}"
                chatRepository.sendMessage(
                    chatId = chatId,
                    text = messageText
                )
                _isSharing.value = false
                onSuccess()
            } catch (_: Exception) {
                _isSharing.value = false
            }
        }
    }
}
