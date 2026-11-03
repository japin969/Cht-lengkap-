package com.example.ui.call

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppModule
import com.example.model.CallLog
import com.example.model.CallSignal
import com.example.model.CallType
import com.example.model.CallUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CallViewModel(
    application: Application,
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null },
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
) : AndroidViewModel(application) {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val callRepository = AppModule.callRepository

    private val _uiState = MutableStateFlow<CallUiState>(
        CallUiState.Connecting("Kontak", isVideoCall = false)
    )
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var currentCallId: String = ""
    private var participantId: String = ""
    private var participantName: String = "Kontak"
    private var participantAvatar: String = ""
    private var isVideoCall: Boolean = false

    private var signalListener: ListenerRegistration? = null
    private var timerJob: Job? = null
    private var elapsedSeconds: Long = 0L

    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    fun initCall(
        callId: String,
        targetParticipantId: String,
        videoCall: Boolean,
        targetName: String = "",
        targetAvatar: String = ""
    ) {
        currentCallId = if (callId.isNotBlank()) callId else UUID.randomUUID().toString()
        participantId = targetParticipantId
        isVideoCall = videoCall
        participantName = if (targetName.isNotBlank()) targetName else resolveContactName(targetParticipantId)
        participantAvatar = targetAvatar

        _uiState.value = CallUiState.Connecting(
            participantName = participantName,
            participantAvatar = participantAvatar,
            isVideoCall = isVideoCall
        )

        setupAudio(videoCall)
        listenToSignaling(currentCallId)

        // WebRTC Signaling Handshake (Create Offer and simulate peer connection)
        viewModelScope.launch {
            sendOfferSignal(currentCallId)
            // Simulated connection establishment for instant preview and feedback
            delay(2000L)
            if (_uiState.value is CallUiState.Connecting) {
                onCallConnected()
            }
        }
    }

    private fun setupAudio(isVideo: Boolean) {
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = isVideo
                am.isMicrophoneMute = false
            } catch (_: Exception) {}
        }
    }

    private fun listenToSignaling(callId: String) {
        if (firestore == null) return
        try {
            signalListener?.remove()
            signalListener = firestore.collection("call_signals")
                .document(callId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val type = snapshot.getString("type") ?: ""
                    val sdp = snapshot.getString("sdp") ?: ""
                    val candidate = snapshot.getString("candidate")

                    when (type) {
                        "answer" -> onRemoteSignalReceived(sdp)
                        "candidate" -> candidate?.let { onIceCandidateReceived(it) }
                        "end" -> onRemoteEndCall()
                    }
                }
        } catch (_: Exception) {}
    }

    private fun sendOfferSignal(callId: String) {
        if (firestore == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val signal = CallSignal(
                    callId = callId,
                    senderId = currentUserId,
                    sdp = "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n",
                    type = "offer",
                    timestamp = System.currentTimeMillis()
                )
                firestore.collection("call_signals").document(callId).set(signal)
            } catch (_: Exception) {}
        }
    }

    private fun onRemoteSignalReceived(sdp: String) {
        // Set remote SDP and connect
        if (_uiState.value is CallUiState.Connecting) {
            onCallConnected()
        }
    }

    private fun onIceCandidateReceived(candidate: String) {
        // PeerConnection.addIceCandidate
    }

    private fun onCallConnected() {
        elapsedSeconds = 0L
        _uiState.value = CallUiState.Connected(
            participantName = participantName,
            participantAvatar = participantAvatar,
            duration = "00:00",
            isVideoCall = isVideoCall,
            isMuted = false,
            isVideoEnabled = isVideoCall,
            isSpeakerOn = audioManager?.isSpeakerphoneOn ?: isVideoCall
        )
        startDurationTimer()
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                elapsedSeconds++
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                val current = _uiState.value
                if (current is CallUiState.Connected) {
                    _uiState.value = current.copy(duration = formatted)
                }
            }
        }
    }

    fun onMuteToggle() {
        val current = _uiState.value as? CallUiState.Connected ?: return
        val newMute = !current.isMuted
        audioManager?.isMicrophoneMute = newMute
        _uiState.value = current.copy(isMuted = newMute)
    }

    fun onVideoToggle() {
        val current = _uiState.value as? CallUiState.Connected ?: return
        val newVideo = !current.isVideoEnabled
        _uiState.value = current.copy(isVideoEnabled = newVideo)
    }

    fun onSpeakerToggle() {
        val current = _uiState.value as? CallUiState.Connected ?: return
        val newSpeaker = !current.isSpeakerOn
        audioManager?.isSpeakerphoneOn = newSpeaker
        _uiState.value = current.copy(isSpeakerOn = newSpeaker)
    }

    fun onEndCall() {
        timerJob?.cancel()
        signalListener?.remove()

        val finalDuration = when (val state = _uiState.value) {
            is CallUiState.Connected -> state.duration
            else -> "00:00"
        }

        // Notify remote peer
        if (firestore != null && currentCallId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("call_signals").document(currentCallId)
                        .set(mapOf("type" to "end", "timestamp" to System.currentTimeMillis()))
                } catch (_: Exception) {}
            }
        }

        // Log call to CallRepository
        val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val log = CallLog(
            id = UUID.randomUUID().toString(),
            callId = currentCallId,
            participantId = participantId,
            participantName = participantName,
            participantAvatar = participantAvatar,
            type = CallType.OUTGOING,
            timestamp = System.currentTimeMillis(),
            duration = elapsedSeconds,
            timeFormatted = timeFormatted,
            isVideo = isVideoCall
        )
        callRepository.logCall(log)

        resetAudio()
        _uiState.value = CallUiState.Ended(
            participantName = participantName,
            duration = finalDuration
        )
    }

    private fun onRemoteEndCall() {
        timerJob?.cancel()
        val finalDuration = when (val state = _uiState.value) {
            is CallUiState.Connected -> state.duration
            else -> "00:00"
        }
        resetAudio()
        _uiState.value = CallUiState.Ended(
            participantName = participantName,
            duration = finalDuration
        )
    }

    private fun resetAudio() {
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_NORMAL
                am.isSpeakerphoneOn = false
                am.isMicrophoneMute = false
            } catch (_: Exception) {}
        }
    }

    private fun resolveContactName(id: String): String {
        return when (id.lowercase()) {
            "pavel_durov" -> "Pavel Durov"
            "elena_p" -> "Elena Petrova"
            "alex_pratama" -> "Alex Pratama"
            "chat_1" -> "Pavel Durov"
            "chat_2" -> "Android Developers Community"
            else -> id.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        signalListener?.remove()
        resetAudio()
    }
}
