package com.jayra.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.ContactNoteRepository
import com.jayra.app.data.ContactRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.ContactInfoUiState
import com.jayra.app.model.ContactNote
import com.jayra.app.model.ContactNoteStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ContactInfoViewModel(
    private val contactRepository: ContactRepository = AppModule.contactRepository,
    private val contactNoteRepository: ContactNoteRepository = AppModule.contactNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactInfoUiState>(ContactInfoUiState.Loading)
    val uiState: StateFlow<ContactInfoUiState> = _uiState.asStateFlow()

    private val _contactNote = MutableStateFlow(ContactNote())
    val contactNote: StateFlow<ContactNote> = _contactNote.asStateFlow()

    private val _noteStrings = MutableStateFlow(ContactNoteStrings())
    val noteStrings: StateFlow<ContactNoteStrings> = _noteStrings.asStateFlow()

    private val _isSavingNote = MutableStateFlow(false)
    val isSavingNote: StateFlow<Boolean> = _isSavingNote.asStateFlow()

    private val _showSavedFeedback = MutableStateFlow(false)
    val showSavedFeedback: StateFlow<Boolean> = _showSavedFeedback.asStateFlow()

    private var currentContactId: String = ""
    private var noteObserveJob: Job? = null

    fun loadContact(contactId: String) {
        currentContactId = contactId
        _uiState.value = ContactInfoUiState.Loading
        viewModelScope.launch {
            try {
                val contact = contactRepository.getContactById(contactId)
                _uiState.value = ContactInfoUiState.Success(contact)
            } catch (e: Exception) {
                _uiState.value = ContactInfoUiState.Error(e.message ?: "Gagal memuat info kontak")
            }
        }
        observeContactNote(contactId)
    }

    private fun observeContactNote(contactId: String) {
        noteObserveJob?.cancel()
        noteObserveJob = viewModelScope.launch {
            contactNoteRepository.observeContactNote(contactId)
                .catch { emit(ContactNote(contactId = contactId)) }
                .collect { note ->
                    _contactNote.value = note
                }
        }
    }

    fun onSavePrivateNote(noteText: String) {
        val targetId = currentContactId.ifBlank { _contactNote.value.contactId }
        if (targetId.isBlank()) return
        val existing = _contactNote.value
        viewModelScope.launch {
            _isSavingNote.value = true
            val result = contactNoteRepository.saveContactNote(
                contactId = targetId,
                noteText = noteText,
                birthdayMillis = existing.birthdayMillis,
                birthdayFormatted = existing.birthdayFormatted
            )
            _isSavingNote.value = false
            result.onSuccess { saved ->
                _contactNote.value = saved
                triggerSavedBadge()
            }
        }
    }

    fun onSaveBirthday(selectedMillis: Long?) {
        val targetId = currentContactId.ifBlank { _contactNote.value.contactId }
        if (targetId.isBlank()) return
        val existing = _contactNote.value
        val formatted = ContactNoteRepository.formatBirthdayMillis(selectedMillis)
        viewModelScope.launch {
            _isSavingNote.value = true
            val result = contactNoteRepository.saveContactNote(
                contactId = targetId,
                noteText = existing.note,
                birthdayMillis = selectedMillis,
                birthdayFormatted = formatted
            )
            _isSavingNote.value = false
            result.onSuccess { saved ->
                _contactNote.value = saved
                triggerSavedBadge()
            }
        }
    }

    private fun triggerSavedBadge() {
        viewModelScope.launch {
            _showSavedFeedback.value = true
            delay(2000L)
            _showSavedFeedback.value = false
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
