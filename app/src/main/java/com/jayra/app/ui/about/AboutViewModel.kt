package com.jayra.app.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.model.AboutUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AboutViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AboutUiState>(AboutUiState.Loading)
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        loadAboutInfo()
    }

    fun loadAboutInfo() {
        viewModelScope.launch {
            _uiState.value = AboutUiState.Success(
                appName = "Jayra",
                version = "1.0.0",
                buildNumber = "100",
                releaseDate = "November 2025",
                developer = "Tim Pengembang Jayra",
                license = "GPL v3.0 / Open Source",
                githubUrl = "https://github.com/japin969/Cht-lengkap-",
                websiteUrl = "https://jayra.app"
            )
        }
    }
}
