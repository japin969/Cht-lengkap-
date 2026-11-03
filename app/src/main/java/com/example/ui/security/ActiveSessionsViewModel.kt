package com.example.ui.security

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ActiveSessionsUiState
import com.example.model.Session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActiveSessionsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    private val firestore: FirebaseFirestore? = com.example.di.AppModule.provideFirestore(application)

    private val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    private val _uiState = MutableStateFlow<ActiveSessionsUiState>(ActiveSessionsUiState.Loading)
    val uiState: StateFlow<ActiveSessionsUiState> = _uiState.asStateFlow()

    private val defaultSessions = listOf(
        Session(
            id = "sess_current",
            deviceName = "Jayra Android (Perangkat Ini)",
            location = "Jakarta, Indonesia",
            lastActive = "Online sekarang",
            isCurrent = true,
            ipAddress = "103.144.17.82",
            appVersion = "Jayra v1.0 Android",
            isDesktop = false
        ),
        Session(
            id = "sess_desktop",
            deviceName = "Jayra Desktop (Windows 11 Pro)",
            location = "Bandung, Indonesia",
            lastActive = "2 jam yang lalu",
            isCurrent = false,
            ipAddress = "180.252.164.12",
            appVersion = "Jayra Desktop v4.16",
            isDesktop = true
        ),
        Session(
            id = "sess_web",
            deviceName = "Jayra Web (Chrome macOS)",
            location = "Surabaya, Indonesia",
            lastActive = "Kemarin, 21:15",
            isCurrent = false,
            ipAddress = "114.122.39.5",
            appVersion = "Jayra Web K",
            isDesktop = true
        )
    )

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = ActiveSessionsUiState.Loading
            if (firestore != null) {
                firestore.collection("sessions")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            val list = snapshot.documents.mapNotNull { doc ->
                                val deviceName = doc.getString("deviceName") ?: "Perangkat Tidak Dikenal"
                                val location = doc.getString("location") ?: "Indonesia"
                                val lastActive = doc.getString("lastActive") ?: "Baru saja"
                                val isCurrent = doc.getBoolean("isCurrent") ?: false
                                val ip = doc.getString("ipAddress") ?: "127.0.0.1"
                                val appVersion = doc.getString("appVersion") ?: "Jayra v1.0"
                                val isDesktop = doc.getBoolean("isDesktop") ?: false
                                Session(
                                    id = doc.id,
                                    deviceName = deviceName,
                                    location = location,
                                    lastActive = lastActive,
                                    isCurrent = isCurrent,
                                    ipAddress = ip,
                                    appVersion = appVersion,
                                    isDesktop = isDesktop
                                )
                            }
                            _uiState.value = ActiveSessionsUiState.Success(list)
                        } else {
                            _uiState.value = ActiveSessionsUiState.Success(defaultSessions)
                        }
                    }
                    .addOnFailureListener {
                        _uiState.value = ActiveSessionsUiState.Success(defaultSessions)
                    }
            } else {
                _uiState.value = ActiveSessionsUiState.Success(defaultSessions)
            }
        }
    }

    fun onEndSession(sessionId: String) {
        val currentState = _uiState.value
        if (currentState is ActiveSessionsUiState.Success) {
            val updated = currentState.sessions.filter { it.id != sessionId }
            _uiState.value = if (updated.isEmpty()) ActiveSessionsUiState.Empty else ActiveSessionsUiState.Success(updated)

            try {
                firestore?.collection("sessions")?.document(sessionId)?.delete()
            } catch (_: Exception) {}
        }
    }

    fun onEndAllOtherSessions() {
        val currentState = _uiState.value
        if (currentState is ActiveSessionsUiState.Success) {
            val currentOnly = currentState.sessions.filter { it.isCurrent }
            val otherSessions = currentState.sessions.filter { !it.isCurrent }

            _uiState.value = ActiveSessionsUiState.Success(currentOnly)

            viewModelScope.launch {
                otherSessions.forEach { session ->
                    try {
                        firestore?.collection("sessions")?.document(session.id)?.delete()
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
