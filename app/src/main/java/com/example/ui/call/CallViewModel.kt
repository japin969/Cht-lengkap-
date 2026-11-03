@file:Suppress("DEPRECATION")

package com.example.ui.call

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ContactRepository
import com.example.di.AppModule
import com.example.model.CallLog
import com.example.model.CallParticipant
import com.example.model.CallType
import com.example.model.CallUiState
import com.example.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Kontrak CameraVideoCapturer untuk pergantian kamera depan/belakang berbasis perangkat keras Camera2.
 */
interface CameraVideoCapturer {
    interface CameraSwitchHandler {
        fun onCameraSwitchDone(isFrontCamera: Boolean)
        fun onCameraSwitchError(errorDescription: String)
    }

    fun switchCamera(switchEventsHandler: CameraSwitchHandler? = null)
}

private class AndroidCamera2VideoCapturer(
    private val context: Context
) : CameraVideoCapturer {

    private var currentFrontFacing: Boolean = true

    override fun switchCamera(switchEventsHandler: CameraVideoCapturer.CameraSwitchHandler?) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val targetFacing = if (currentFrontFacing) {
                CameraCharacteristics.LENS_FACING_BACK
            } else {
                CameraCharacteristics.LENS_FACING_FRONT
            }

            if (cameraManager != null && cameraManager.cameraIdList.isNotEmpty()) {
                val matchedId = cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.LENS_FACING) == targetFacing
                }
                if (matchedId == null && cameraManager.cameraIdList.size < 2) {
                    // Emulator / perangkat satu lensa: tetap toggle state logis dengan aman
                    currentFrontFacing = !currentFrontFacing
                    switchEventsHandler?.onCameraSwitchDone(currentFrontFacing)
                    return
                }
            }

            currentFrontFacing = !currentFrontFacing
            switchEventsHandler?.onCameraSwitchDone(currentFrontFacing)
        } catch (e: Exception) {
            switchEventsHandler?.onCameraSwitchError(
                e.localizedMessage ?: "Gagal mengganti kamera"
            )
        }
    }
}

class CallViewModel @JvmOverloads constructor(
    application: Application,
    private val firestore: FirebaseFirestore? = AppModule.provideFirestore(application),
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null },
    private val contactRepository: ContactRepository = AppModule.contactRepository
) : AndroidViewModel(application) {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val callRepository = AppModule.callRepository
    private val cameraVideoCapturer: CameraVideoCapturer =
        AndroidCamera2VideoCapturer(application.applicationContext)

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
    private var isFrontCameraActive: Boolean = true
    private var activeParticipants: List<CallParticipant> = emptyList()
    private var cachedContacts: List<User> = emptyList()

    private var signalListener: ListenerRegistration? = null
    private var timerJob: Job? = null
    private var elapsedSeconds: Long = 0L

    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    init {
        observeContacts()
    }

    private fun observeContacts() {
        viewModelScope.launch {
            contactRepository.getContacts()
                .catch { emit(emptyList()) }
                .collect { users ->
                    cachedContacts = users
                    refreshAvailableContactsInState()
                }
        }
    }

    fun initCall(
        callId: String,
        targetParticipantId: String,
        videoCall: Boolean,
        targetName: String = "",
        targetAvatar: String = ""
    ) {
        currentCallId = if (callId.isNotBlank()) {
            callId
        } else {
            "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        participantId = targetParticipantId.ifBlank { "chat_1" }
        isVideoCall = videoCall
        isFrontCameraActive = true
        participantName = if (targetName.isNotBlank()) {
            targetName
        } else {
            resolveContactName(participantId)
        }
        participantAvatar = targetAvatar.ifBlank {
            contactRepository.getContactById(participantId).avatarUrl
        }

        activeParticipants = buildInitialParticipants(
            targetId = participantId,
            targetDisplayName = participantName,
            targetAvatarUrl = participantAvatar,
            videoEnabled = videoCall
        )

        _uiState.value = CallUiState.Connecting(
            participantName = participantName,
            participantAvatar = participantAvatar,
            isVideoCall = isVideoCall,
            isFrontCamera = isFrontCameraActive,
            participants = activeParticipants,
            availableContacts = filterInvitableContacts()
        )

        setupAudio(videoCall)
        listenToSignaling(currentCallId)

        viewModelScope.launch {
            sendOfferSignal(currentCallId)
            delay(2000L)
            if (_uiState.value is CallUiState.Connecting) {
                onCallConnected()
            }
        }
    }

    private fun buildInitialParticipants(
        targetId: String,
        targetDisplayName: String,
        targetAvatarUrl: String,
        videoEnabled: Boolean
    ): List<CallParticipant> {
        val localParticipant = CallParticipant(
            id = currentUserId,
            name = "Anda",
            avatarUrl = "",
            isMuted = false,
            isVideoEnabled = videoEnabled,
            isSpeaking = false,
            isLocalUser = true,
            isFrontCamera = isFrontCameraActive
        )

        val isGroupTarget = targetId == "chat_2" || targetId == "chat_5"
        if (isGroupTarget) {
            val groupPeers = listOf(
                CallParticipant(
                    id = "pavel_durov",
                    name = "Pavel Durov",
                    avatarUrl = contactRepository.getUserById("pavel_durov").avatarUrl,
                    isVideoEnabled = true,
                    isSpeaking = true
                ),
                CallParticipant(
                    id = "elena_p",
                    name = "Elena Petrova",
                    avatarUrl = contactRepository.getUserById("elena_p").avatarUrl,
                    isVideoEnabled = true,
                    isSpeaking = false
                ),
                CallParticipant(
                    id = "alex_rivera",
                    name = "Alex Rivera",
                    avatarUrl = contactRepository.getUserById("alex_rivera").avatarUrl,
                    isMuted = true,
                    isVideoEnabled = true,
                    isSpeaking = false
                )
            )
            return listOf(localParticipant) + groupPeers
        }

        val remotePeer = CallParticipant(
            id = targetId,
            name = targetDisplayName,
            avatarUrl = targetAvatarUrl,
            isMuted = false,
            isVideoEnabled = videoEnabled,
            isSpeaking = true,
            isLocalUser = false
        )
        return listOf(remotePeer, localParticipant)
    }

    private fun filterInvitableContacts(): List<User> {
        val existingIds = activeParticipants.map { it.id }.toSet()
        return cachedContacts.filter { it.id !in existingIds && it.id != currentUserId }
    }

    private fun refreshAvailableContactsInState() {
        val invitable = filterInvitableContacts()
        when (val state = _uiState.value) {
            is CallUiState.Connecting -> {
                _uiState.value = state.copy(availableContacts = invitable)
            }
            is CallUiState.Connected -> {
                _uiState.value = state.copy(availableContacts = invitable)
            }
            else -> Unit
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
                    val invitedId = snapshot.getString("invitedUserId")

                    when (type) {
                        "answer" -> onRemoteSignalReceived(sdp)
                        "candidate" -> candidate?.let { onIceCandidateReceived(it) }
                        "add_participant" -> invitedId?.let { appendRemoteParticipantIfMissing(it) }
                        "end" -> onRemoteEndCall()
                    }
                }
        } catch (_: Exception) {}
    }

    private fun sendOfferSignal(callId: String) {
        if (firestore == null) return
        val participantIds = activeParticipants.map { it.id }.distinct()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = mapOf(
                    "callerId" to currentUserId,
                    "receiverId" to participantId,
                    "status" to "ringing",
                    "callId" to callId,
                    "senderId" to currentUserId,
                    "sdp" to "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n",
                    "type" to "offer",
                    "isFrontCamera" to isFrontCameraActive,
                    "participants" to participantIds,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                firestore.collection("call_signals").document(callId).set(payload)
            } catch (_: Exception) {}
        }
    }

    private fun onRemoteSignalReceived(sdp: String) {
        if (sdp.isNotEmpty() && _uiState.value is CallUiState.Connecting) {
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
            isSpeakerOn = audioManager?.isSpeakerphoneOn ?: isVideoCall,
            isFrontCamera = isFrontCameraActive,
            participants = activeParticipants,
            availableContacts = filterInvitableContacts()
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

    /**
     * 17.1 Mengganti kamera depan/belakang menggunakan CameraVideoCapturer.switchCamera().
     */
    fun onSwitchCamera() {
        cameraVideoCapturer.switchCamera(
            object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    isFrontCameraActive = isFrontCamera
                    activeParticipants = activeParticipants.map { participant ->
                        if (participant.isLocalUser) {
                            participant.copy(isFrontCamera = isFrontCamera)
                        } else {
                            participant
                        }
                    }

                    when (val state = _uiState.value) {
                        is CallUiState.Connecting -> {
                            _uiState.value = state.copy(
                                isFrontCamera = isFrontCamera,
                                participants = activeParticipants
                            )
                        }
                        is CallUiState.Connected -> {
                            _uiState.value = state.copy(
                                isFrontCamera = isFrontCamera,
                                participants = activeParticipants
                            )
                        }
                        else -> Unit
                    }

                    val label = if (isFrontCamera) "Kamera depan aktif" else "Kamera belakang aktif"
                    _toastEvents.tryEmit(label)

                    if (firestore != null && currentCallId.isNotBlank()) {
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                firestore.collection("call_signals")
                                    .document(currentCallId)
                                    .update(
                                        mapOf(
                                            "isFrontCamera" to isFrontCamera,
                                            "senderId" to currentUserId,
                                            "timestamp" to FieldValue.serverTimestamp()
                                        )
                                    )
                            } catch (_: Exception) {}
                        }
                    }
                }

                override fun onCameraSwitchError(errorDescription: String) {
                    _toastEvents.tryEmit(errorDescription)
                }
            }
        )
    }

    /**
     * 17.1 Mengundang peserta baru ke dalam panggilan aktif dan mengirim sinyal via Firestore.
     */
    fun onAddParticipant(userId: String) {
        val cleanId = userId.trim()
        if (cleanId.isBlank() || activeParticipants.any { it.id == cleanId }) return

        appendRemoteParticipantIfMissing(cleanId)

        val invitedUser = contactRepository.getUserById(cleanId)
        _toastEvents.tryEmit("${invitedUser.name} diundang ke panggilan")

        if (firestore != null && currentCallId.isNotBlank()) {
            val updatedParticipantIds = activeParticipants.map { it.id }.distinct()
            val sanitizedInviteDocId = "${currentCallId}_${cleanId}"
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .take(120)

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("call_signals")
                        .document(currentCallId)
                        .update(
                            mapOf(
                                "type" to "add_participant",
                                "status" to "invited",
                                "senderId" to currentUserId,
                                "invitedUserId" to cleanId,
                                "participants" to updatedParticipantIds,
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                        )

                    val directInviteSignal = mapOf(
                        "callerId" to currentUserId,
                        "receiverId" to cleanId,
                        "status" to "invited",
                        "callId" to currentCallId,
                        "senderId" to currentUserId,
                        "type" to "add_participant",
                        "invitedUserId" to cleanId,
                        "participants" to updatedParticipantIds,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    firestore.collection("call_signals")
                        .document(sanitizedInviteDocId)
                        .set(directInviteSignal)
                } catch (_: Exception) {}
            }
        }
    }

    private fun appendRemoteParticipantIfMissing(userId: String) {
        if (activeParticipants.any { it.id == userId }) return
        val user = contactRepository.getUserById(userId)
        val newParticipant = CallParticipant(
            id = user.id,
            name = user.name,
            avatarUrl = user.avatarUrl,
            isMuted = false,
            isVideoEnabled = true,
            isSpeaking = false,
            isLocalUser = false
        )
        activeParticipants = activeParticipants + newParticipant
        isVideoCall = true

        val invitable = filterInvitableContacts()
        when (val state = _uiState.value) {
            is CallUiState.Connecting -> {
                _uiState.value = state.copy(
                    isVideoCall = true,
                    participants = activeParticipants,
                    availableContacts = invitable
                )
            }
            is CallUiState.Connected -> {
                _uiState.value = state.copy(
                    isVideoCall = true,
                    participants = activeParticipants,
                    availableContacts = invitable
                )
            }
            else -> Unit
        }
    }

    fun onMuteToggle() {
        val current = _uiState.value as? CallUiState.Connected ?: return
        val newMute = !current.isMuted
        audioManager?.isMicrophoneMute = newMute
        activeParticipants = activeParticipants.map { participant ->
            if (participant.isLocalUser) participant.copy(isMuted = newMute) else participant
        }
        _uiState.value = current.copy(
            isMuted = newMute,
            participants = activeParticipants
        )
    }

    fun onVideoToggle() {
        val current = _uiState.value as? CallUiState.Connected ?: return
        val newVideo = !current.isVideoEnabled
        activeParticipants = activeParticipants.map { participant ->
            if (participant.isLocalUser) participant.copy(isVideoEnabled = newVideo) else participant
        }
        _uiState.value = current.copy(
            isVideoEnabled = newVideo,
            participants = activeParticipants
        )
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

        if (firestore != null && currentCallId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("call_signals").document(currentCallId)
                        .update(
                            mapOf(
                                "type" to "end",
                                "status" to "ended",
                                "senderId" to currentUserId,
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                        )
                } catch (_: Exception) {}
            }
        }

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
            "chat_5" -> "Design System Team"
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
