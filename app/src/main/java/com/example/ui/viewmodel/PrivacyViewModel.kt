package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserRepository
import com.example.di.AppModule
import com.example.model.PrivacySettings
import com.example.model.PrivacyUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrivacyViewModel(
    private val userRepository: UserRepository = AppModule.userRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrivacyUiState>(PrivacyUiState.Loading)
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        loadPrivacySettings()
    }

    fun loadPrivacySettings() {
        _uiState.value = PrivacyUiState.Loading
        viewModelScope.launch {
            try {
                userRepository.getPrivacySettings().collect { settings ->
                    _uiState.value = PrivacyUiState.Success(settings)
                }
            } catch (e: Exception) {
                _uiState.value = PrivacyUiState.Error(e.message ?: "Gagal memuat pengaturan privasi")
            }
        }
    }

    fun onAppLockToggle(enabled: Boolean) {
        userRepository.setAppLock(enabled)
    }

    fun onPhoneNumberPrivacyChange(privacy: String) {
        userRepository.updatePhoneNumberPrivacy(privacy)
    }

    fun onLastSeenPrivacyChange(privacy: String) {
        userRepository.updateLastSeenPrivacy(privacy)
    }

    fun onProfilePhotoPrivacyChange(privacy: String) {
        userRepository.updateProfilePhotoPrivacy(privacy)
    }

    fun onDisappearingDurationChange(duration: String) {
        viewModelScope.launch {
            userRepository.updateDisappearingDuration(duration)
        }
    }
}
