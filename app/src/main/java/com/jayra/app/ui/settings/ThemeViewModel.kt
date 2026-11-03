package com.jayra.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.model.ThemeOption
import com.jayra.app.model.ThemeUiState
import com.jayra.app.security.EncryptionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ThemeUiState>(ThemeUiState.Loading)
    val uiState: StateFlow<ThemeUiState> = _uiState.asStateFlow()

    private val themeOptions = listOf(
        ThemeOption("system", "Ikuti Sistem", "Menyesuaikan dengan pengaturan mode gelap/terang sistem perangkat."),
        ThemeOption("light", "Mode Terang", "Tampilan cerah bernuansa putih bersih khas Jayra klasik."),
        ThemeOption("dark", "Mode Gelap", "Tampilan gelap hemat baterai dengan kenyamanan mata maksimal.")
    )

    init {
        loadTheme()
    }

    fun loadTheme() {
        viewModelScope.launch {
            val savedTheme = EncryptionHelper.getEncryptedPref(getApplication(), "app_theme") ?: "system"
            _uiState.value = ThemeUiState.Success(
                currentTheme = savedTheme,
                availableThemes = themeOptions,
                dynamicColor = true,
                accentColorHex = 0xFF0088CC
            )
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            EncryptionHelper.saveEncryptedPref(getApplication(), "app_theme", themeId)
            val currentState = _uiState.value
            if (currentState is ThemeUiState.Success) {
                _uiState.value = currentState.copy(currentTheme = themeId)
            }
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is ThemeUiState.Success) {
            _uiState.value = currentState.copy(dynamicColor = enabled)
        }
    }

    fun selectAccentColor(colorHex: Long) {
        val currentState = _uiState.value
        if (currentState is ThemeUiState.Success) {
            _uiState.value = currentState.copy(accentColorHex = colorHex)
        }
    }
}
