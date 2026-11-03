package com.jayra.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.CallRepository
import com.jayra.app.data.ContactRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.CallType
import com.jayra.app.model.ContactsUiState
import com.jayra.app.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ContactsViewModel(
    private val contactRepository: ContactRepository = AppModule.contactRepository,
    private val callRepository: CallRepository = AppModule.callRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ContactsUiState> = combine(
        contactRepository.getContacts(),
        _searchQuery,
        _errorMessage
    ) { allContacts, query, error ->
        if (error != null) {
            ContactsUiState.Error(error)
        } else {
            val filteredContacts = if (query.isBlank()) {
                allContacts.sortedBy { it.name.lowercase() }
            } else {
                allContacts.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.username.contains(query, ignoreCase = true) ||
                    it.phoneNumber.contains(query, ignoreCase = true)
                }.sortedBy { it.name.lowercase() }
            }

            if (filteredContacts.isEmpty() && allContacts.isEmpty()) {
                ContactsUiState.Empty
            } else {
                val online = filteredContacts.filter { it.isOnline }
                ContactsUiState.Success(
                    contacts = filteredContacts,
                    onlineUsers = online
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactsUiState.Loading)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch(active: Boolean) {
        _isSearching.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun addContact(name: String, phone: String, username: String, context: Context) {
        if (name.isBlank()) return
        val newContact = User(
            id = "user_${UUID.randomUUID()}",
            name = name.trim(),
            phoneNumber = phone.trim(),
            username = username.trim().removePrefix("@"),
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400",
            isOnline = false,
            lastSeenFormatted = "terlihat baru saja"
        )
        viewModelScope.launch {
            contactRepository.addContact(newContact, context)
        }
    }

    fun deleteContact(userId: String, context: Context) {
        viewModelScope.launch {
            contactRepository.deleteContact(userId, context)
        }
    }

    fun toggleMuteContact(userId: String) {
        viewModelScope.launch {
            contactRepository.toggleMuteContact(userId)
        }
    }

    fun blockContact(userId: String, context: Context) {
        viewModelScope.launch {
            contactRepository.blockContact(userId, context)
        }
    }

    fun startVoiceCall(context: Context, user: User) {
        callRepository.startCall(context, user.id, user.name, CallType.VOICE)
    }

    fun startVideoCall(context: Context, user: User) {
        callRepository.startCall(context, user.id, user.name, CallType.VIDEO)
    }
}
