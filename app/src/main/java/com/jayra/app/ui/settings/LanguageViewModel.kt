package com.jayra.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.model.LanguageItem
import com.jayra.app.model.LanguageUiState
import com.jayra.app.security.EncryptionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LanguageViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LanguageUiState>(LanguageUiState.Loading)
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    private val availableLanguages = listOf(
        LanguageItem("id", "Bahasa Indonesia", "Bahasa Indonesia"),
        LanguageItem("en", "English", "English (US)"),
        LanguageItem("es", "Bahasa Spanyol", "Español"),
        LanguageItem("ar", "Bahasa Arab", "العربية"),
        LanguageItem("ru", "Bahasa Rusia", "Русский"),
        LanguageItem("ja", "Bahasa Jepang", "日本語"),
        LanguageItem("de", "Bahasa Jerman", "Deutsch"),
        LanguageItem("fr", "Bahasa Prancis", "Français"),
        LanguageItem("pt", "Bahasa Portugis", "Português"),
        LanguageItem("ko", "Bahasa Korea", "한국어")
    )

    init {
        loadLanguage()
    }

    fun loadLanguage() {
        viewModelScope.launch {
            val savedLanguage = EncryptionHelper.getEncryptedPref(getApplication(), "app_language") ?: "id"
            updateState(savedLanguage, "")
        }
    }

    fun selectLanguage(code: String) {
        viewModelScope.launch {
            EncryptionHelper.saveEncryptedPref(getApplication(), "app_language", code)
            val currentQuery = (_uiState.value as? LanguageUiState.Success)?.searchQuery ?: ""
            updateState(code, currentQuery)
        }
    }

    fun searchLanguage(query: String) {
        val currentCode = (_uiState.value as? LanguageUiState.Success)?.currentLanguageCode ?: "id"
        updateState(currentCode, query)
    }

    private fun updateState(currentCode: String, query: String) {
        val filtered = if (query.isBlank()) {
            availableLanguages
        } else {
            availableLanguages.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.nativeName.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true)
            }
        }

        val mappedLanguages = filtered.map { item ->
            item.copy(isSelected = item.code == currentCode)
        }

        _uiState.value = LanguageUiState.Success(
            currentLanguageCode = currentCode,
            languages = mappedLanguages,
            searchQuery = query
        )
    }
}
