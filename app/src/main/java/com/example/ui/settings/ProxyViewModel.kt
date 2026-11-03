package com.example.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.model.ProxyUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ProxyViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("jayra_proxy_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<ProxyUiState>(ProxyUiState.Loading)
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    init {
        loadProxySettings()
    }

    fun loadProxySettings() {
        val isEnabled = prefs.getBoolean("proxy_enabled", false)
        val host = prefs.getString("proxy_host", "") ?: ""
        val port = prefs.getString("proxy_port", "") ?: ""
        val username = prefs.getString("proxy_username", "") ?: ""
        val password = prefs.getString("proxy_password", "") ?: ""

        _uiState.value = ProxyUiState.Success(
            isEnabled = isEnabled,
            host = host,
            port = port,
            username = username,
            password = password
        )
    }

    fun onToggle(enabled: Boolean) {
        prefs.edit().putBoolean("proxy_enabled", enabled).apply()
        val current = (_uiState.value as? ProxyUiState.Success) ?: return
        _uiState.value = current.copy(isEnabled = enabled)
    }

    fun onSave(host: String, port: String, username: String, password: String) {
        prefs.edit()
            .putString("proxy_host", host.trim())
            .putString("proxy_port", port.trim())
            .putString("proxy_username", username.trim())
            .putString("proxy_password", password.trim())
            .apply()

        val current = (_uiState.value as? ProxyUiState.Success) ?: return
        _uiState.value = current.copy(
            host = host.trim(),
            port = port.trim(),
            username = username.trim(),
            password = password.trim()
        )
        _toastEvents.tryEmit("Pengaturan proxy disimpan")
    }
}
