package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ContactRepository
import com.example.di.AppModule
import com.example.model.Contact
import com.example.model.ContactInfoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactInfoViewModel(
    private val contactRepository: ContactRepository = AppModule.contactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactInfoUiState>(ContactInfoUiState.Loading)
    val uiState: StateFlow<ContactInfoUiState> = _uiState.asStateFlow()

    private var currentContactId: String = ""

    fun loadContact(contactId: String) {
        currentContactId = contactId
        _uiState.value = ContactInfoUiState.Loading
        viewModelScope.launch {
            try {
                val contact = contactRepository.getContactById(contactId)
                _uiState.value = ContactInfoUiState.Success(contact)
            } catch (e: Exception) {
                _uiState.value = ContactInfoUiState.Error(e.message ?: "Failed to load contact info")
            }
        }
    }

    fun onMuteToggle(enabled: Boolean) {
        contactRepository.toggleMute(currentContactId, enabled)
        val currentState = _uiState.value
        if (currentState is ContactInfoUiState.Success) {
            _uiState.value = ContactInfoUiState.Success(currentState.contact.copy(isMuted = enabled))
        }
    }

    fun onSaveToCameraToggle(enabled: Boolean) {
        contactRepository.toggleSaveToCamera(currentContactId, enabled)
        val currentState = _uiState.value
        if (currentState is ContactInfoUiState.Success) {
            _uiState.value = ContactInfoUiState.Success(currentState.contact.copy(saveToCamera = enabled))
        }
    }

    fun onDisappearingMessagesToggle() {
        contactRepository.toggleDisappearing(currentContactId)
        val currentState = _uiState.value
        if (currentState is ContactInfoUiState.Success) {
            val nowVal = !currentState.contact.disappearingEnabled
            _uiState.value = ContactInfoUiState.Success(currentState.contact.copy(disappearingEnabled = nowVal))
        }
    }

    fun onRetry() {
        if (currentContactId.isNotEmpty()) {
            loadContact(currentContactId)
        }
    }
}
