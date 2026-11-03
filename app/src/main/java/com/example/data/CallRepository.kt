package com.example.data

import android.content.Context
import android.widget.Toast
import com.example.model.CallLog
import com.example.model.CallType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ActiveCallState(
    val callId: String = "",
    val targetName: String = "",
    val isVideo: Boolean = false,
    val isConnected: Boolean = false
)

class CallRepository(
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null },
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
) {
    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
    val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()

    private val _localCalls = MutableStateFlow<List<CallLog>>(
        listOf(
            CallLog(
                id = "call_1",
                participantId = "pavel_durov",
                participantName = "Pavel Durov",
                participantAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                type = CallType.INCOMING,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 35, // 35m ago
                duration = 184L,
                timeFormatted = "10:45 AM",
                isVideo = false
            ),
            CallLog(
                id = "call_2",
                participantId = "elena_p",
                participantName = "Elena Petrova",
                participantAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                type = CallType.MISSED,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 180, // 3h ago
                duration = 0L,
                timeFormatted = "08:15 AM",
                isVideo = true
            ),
            CallLog(
                id = "call_3",
                participantId = "michael_v",
                participantName = "Michael Vance",
                participantAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                type = CallType.OUTGOING,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24, // Yesterday
                duration = 420L,
                timeFormatted = "Yesterday",
                isVideo = false
            ),
            CallLog(
                id = "call_4",
                participantId = "sarah_connor",
                participantName = "Sarah Connor",
                participantAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200",
                type = CallType.MISSED,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48, // 2 days ago
                duration = 0L,
                timeFormatted = "Oct 31",
                isVideo = false
            ),
            CallLog(
                id = "call_5",
                participantId = "pavel_durov",
                participantName = "Pavel Durov",
                participantAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                type = CallType.INCOMING,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 72,
                duration = 312L,
                timeFormatted = "Oct 30",
                isVideo = true
            )
        )
    )

    fun getCalls(): Flow<List<CallLog>> = callbackFlow {
        if (firestore != null) {
            val listener = firestore.collection("calls")
                .whereArrayContains("participants", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) {
                        trySend(_localCalls.value)
                        return@addSnapshotListener
                    }
                    val remote = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CallLog::class.java)?.copy(id = doc.id)
                    }
                    if (remote.isNotEmpty()) {
                        trySend(remote)
                    } else {
                        trySend(_localCalls.value)
                    }
                }
            awaitClose { listener.remove() }
        } else {
            val job = launch {
                _localCalls.collect { list -> trySend(list) }
            }
            awaitClose { job.cancel() }
        }
    }

    val callLogs: Flow<List<CallLog>>
        get() = getCalls()

    fun logCall(callLog: CallLog) {
        _localCalls.update { listOf(callLog) + it }
        try {
            firestore?.collection("calls")?.document(callLog.id)?.set(callLog)
        } catch (_: Exception) {}
    }

    fun startCall(context: Context, participantId: String, participantName: String, type: CallType) {
        val isVideo = (type == CallType.VIDEO)
        _activeCall.value = ActiveCallState(
            callId = UUID.randomUUID().toString(),
            targetName = participantName,
            isVideo = isVideo,
            isConnected = true
        )
        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val newCall = CallLog(
            id = UUID.randomUUID().toString(),
            participantId = participantId,
            participantName = participantName,
            type = CallType.OUTGOING,
            timestamp = System.currentTimeMillis(),
            duration = 0L,
            timeFormatted = timeString,
            isVideo = isVideo
        )

        _localCalls.update { listOf(newCall) + it }

        val typeLabel = if (isVideo) "Panggilan Video" else "Panggilan Suara"
        Toast.makeText(context, "Memulai $typeLabel: $participantName", Toast.LENGTH_SHORT).show()

        try {
            firestore?.collection("calls")?.document(newCall.id)?.set(newCall)
        } catch (_: Exception) {}
    }

    fun startGroupVoiceCall(context: Context, groupId: String, groupName: String) {
        _activeCall.value = ActiveCallState(
            callId = groupId,
            targetName = groupName,
            isVideo = false,
            isConnected = true
        )
        Toast.makeText(context, "Memulai panggilan audio grup: $groupName", Toast.LENGTH_SHORT).show()
    }

    fun startGroupVideoCall(context: Context, groupId: String, groupName: String) {
        _activeCall.value = ActiveCallState(
            callId = groupId,
            targetName = groupName,
            isVideo = true,
            isConnected = true
        )
        Toast.makeText(context, "Memulai panggilan video grup: $groupName", Toast.LENGTH_SHORT).show()
    }

    fun startChannelVoiceStream(context: Context, channelId: String, channelName: String) {
        Toast.makeText(context, "Menghubungkan ke Voice Stream: $channelName", Toast.LENGTH_SHORT).show()
    }

    fun startChannelVideoStream(context: Context, channelId: String, channelName: String) {
        Toast.makeText(context, "Menghubungkan ke Video Live Stream: $channelName", Toast.LENGTH_SHORT).show()
    }

    suspend fun deleteCall(callId: String) {
        _localCalls.update { list -> list.filter { it.id != callId } }
        try {
            firestore?.collection("calls")?.document(callId)?.delete()
        } catch (_: Exception) {}
    }

    suspend fun blockContact(userId: String) {
        try {
            firestore?.collection("users")?.document(userId)?.update("blocked", true)
        } catch (_: Exception) {}
    }

    fun endCall() {
        _activeCall.value = null
    }
}
