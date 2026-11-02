package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserRepository
import com.example.di.AppModule
import com.example.model.NotificationSettings
import com.example.model.NotificationsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val userRepository: UserRepository = AppModule.userRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotificationSettings()
    }

    fun loadNotificationSettings() {
        _uiState.value = NotificationsUiState.Loading
        viewModelScope.launch {
            try {
                userRepository.getNotificationSettings().collect { settings ->
                    _uiState.value = NotificationsUiState.Success(settings)
                }
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "Gagal memuat pengaturan notifikasi")
            }
        }
    }

    fun onPrivateToggle(enabled: Boolean) {
        userRepository.updatePrivateNotifications(enabled)
    }

    fun onGroupToggle(enabled: Boolean) {
        userRepository.updateGroupNotifications(enabled)
    }

    fun onChannelToggle(enabled: Boolean) {
        userRepository.updateChannelNotifications(enabled)
    }

    fun onSoundChange(sound: String) {
        userRepository.updateSound(sound)
    }

    fun onVibrateChange(vibrate: String) {
        userRepository.updateVibrate(vibrate)
    }

    fun onPriorityToggle(enabled: Boolean) {
        userRepository.updatePriorityMessage(enabled)
    }

    fun onDeletedToggle(enabled: Boolean) {
        userRepository.updateDeletedNotice(enabled)
    }

    fun onPreviewToggle(enabled: Boolean) {
        userRepository.updateMessagePreview(enabled)
    }
}
