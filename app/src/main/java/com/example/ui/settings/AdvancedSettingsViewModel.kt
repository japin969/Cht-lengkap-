package com.example.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AdvancedSettings
import com.example.model.AdvancedSettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class AdvancedSettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("jayra_advanced_settings_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<AdvancedSettingsUiState>(AdvancedSettingsUiState.Loading)
    val uiState: StateFlow<AdvancedSettingsUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = AdvancedSettingsUiState.Loading
            try {
                val cacheBytes = withContext(Dispatchers.IO) {
                    calculateCacheBytes()
                }

                val settings = AdvancedSettings(
                    dataRoaming = prefs.getBoolean("roaming", false),
                    proxyEnabled = prefs.getBoolean("proxy_enabled", false),
                    callQuality = prefs.getString("call_quality", "Otomatis") ?: "Otomatis",
                    uploadQuality = prefs.getString("upload_quality", "HD") ?: "HD",
                    externalStorage = prefs.getBoolean("externalStorage", false),
                    cacheSize = formatFileSize(cacheBytes),
                    cacheLimit = prefs.getString("cache_limit", "5 GB") ?: "5 GB",
                    saveToGallery = prefs.getBoolean("saveToGallery", false),
                    animationsEnabled = prefs.getBoolean("animations", true),
                    typingVibration = prefs.getBoolean("typingVibration", true),
                    enterToSend = prefs.getBoolean("enterToSend", false)
                )

                _uiState.value = AdvancedSettingsUiState.Success(settings)
            } catch (e: Exception) {
                _uiState.value = AdvancedSettingsUiState.Error(e.localizedMessage ?: "Gagal memuat pengaturan")
            }
        }
    }

    fun onToggle(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
        val current = (_uiState.value as? AdvancedSettingsUiState.Success)?.settings ?: return
        val updated = when (key) {
            "roaming" -> current.copy(dataRoaming = enabled)
            "externalStorage" -> current.copy(externalStorage = enabled)
            "saveToGallery" -> current.copy(saveToGallery = enabled)
            "animations" -> current.copy(animationsEnabled = enabled)
            "typingVibration" -> current.copy(typingVibration = enabled)
            "enterToSend" -> current.copy(enterToSend = enabled)
            else -> current
        }
        _uiState.value = AdvancedSettingsUiState.Success(updated)
    }

    fun onCallQualityChange(quality: String) {
        prefs.edit().putString("call_quality", quality).apply()
        val current = (_uiState.value as? AdvancedSettingsUiState.Success)?.settings ?: return
        _uiState.value = AdvancedSettingsUiState.Success(current.copy(callQuality = quality))
    }

    fun onUploadQualityChange(quality: String) {
        prefs.edit().putString("upload_quality", quality).apply()
        val current = (_uiState.value as? AdvancedSettingsUiState.Success)?.settings ?: return
        _uiState.value = AdvancedSettingsUiState.Success(current.copy(uploadQuality = quality))
    }

    fun onCacheLimitChange(limit: String) {
        prefs.edit().putString("cache_limit", limit).apply()
        val current = (_uiState.value as? AdvancedSettingsUiState.Success)?.settings ?: return
        _uiState.value = AdvancedSettingsUiState.Success(current.copy(cacheLimit = limit))
    }

    fun onClearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val cacheDir = getApplication<Application>().cacheDir
                    cacheDir.listFiles()?.forEach { file ->
                        file.deleteRecursively()
                    }
                } catch (_: Exception) {}
            }

            val newCacheSize = formatFileSize(calculateCacheBytes())
            val current = (_uiState.value as? AdvancedSettingsUiState.Success)?.settings
            if (current != null) {
                _uiState.value = AdvancedSettingsUiState.Success(current.copy(cacheSize = newCacheSize))
            }
            _toastEvents.tryEmit("Cache dibersihkan")
        }
    }

    private fun calculateCacheBytes(): Long {
        return try {
            val cacheDir = getApplication<Application>().cacheDir
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            else -> String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }
}
