package com.jayra.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.UserRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.MyProfileUiState
import com.jayra.app.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyProfileViewModel(
    private val userRepository: UserRepository = AppModule.userRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyProfileUiState>(MyProfileUiState.Loading)
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        _uiState.value = MyProfileUiState.Loading
        viewModelScope.launch {
            try {
                userRepository.getUserProfile().collect { user ->
                    _uiState.value = MyProfileUiState.Success(user)
                }
            } catch (e: Exception) {
                _uiState.value = MyProfileUiState.Error(e.message ?: "Gagal memuat profil saya")
            }
        }
    }

    fun onAppLockToggle(enabled: Boolean) {
        userRepository.setAppLock(enabled)
        val currentState = _uiState.value
        if (currentState is MyProfileUiState.Success) {
            _uiState.value = MyProfileUiState.Success(currentState.user.copy(isAppLockEnabled = enabled))
        }
    }

    fun onDisappearingToggle() {
        val currentState = _uiState.value
        if (currentState is MyProfileUiState.Success) {
            val newDuration = if (currentState.user.disappearingDuration == "Mati") "24 Jam" else "Mati"
            viewModelScope.launch {
                userRepository.updateDisappearingDuration(newDuration)
            }
        }
    }

    fun onLanguageChange(lang: String) {
        userRepository.updateLanguage(lang)
    }

    fun onRetry() {
        loadProfile()
    }
}
