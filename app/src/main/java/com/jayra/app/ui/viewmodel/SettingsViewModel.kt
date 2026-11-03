package com.jayra.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.UserRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val userRepository: UserRepository = AppModule.userRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        userRepository.getUserProfile(),
        userRepository.getAppTheme(),
        _errorMessage
    ) { user, theme, error ->
        if (error != null) {
            SettingsUiState.Error(error)
        } else {
            SettingsUiState.Success(
                user = user,
                appTheme = theme,
                language = user.language
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState.Loading)

    fun onThemeChange(newTheme: String) {
        userRepository.updateAppTheme(newTheme)
    }

    fun onLanguageChange(newLang: String) {
        userRepository.updateLanguage(newLang)
    }
}
