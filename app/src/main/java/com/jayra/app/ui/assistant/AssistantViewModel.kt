package com.jayra.app.ui.assistant

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AIResponse
import com.jayra.app.data.EncryptionHelper
import com.jayra.app.data.NexadevAiApi
import com.jayra.app.data.NexadevAiResponse
import com.jayra.app.data.SiputzxApi
import com.jayra.app.model.AssistantUiState
import com.jayra.app.model.Message
import com.jayra.app.model.MessageStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AssistantViewModel @JvmOverloads constructor(
    application: Application,
    private val siputzxApi: SiputzxApi = SiputzxApi.create(),
    private val nexadevAiApi: NexadevAiApi = NexadevAiApi.create(),
    private val firestore: FirebaseFirestore? = com.jayra.app.di.AppModule.provideFirestore(application),
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("jayra_security_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Loading)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    private var historyListener: ListenerRegistration? = null
    private var lastMessageTimestamp: Long
        get() = prefs.getLong("last_ai_message_time", 0L)
        set(value) = prefs.edit().putLong("last_ai_message_time", value).apply()

    private var lastFailedPrompt: String? = null
    private val inMemoryMessages = mutableListOf<Message>()

    init {
        loadHistory()
    }

    fun isAssistantEnabled(): Boolean {
        return prefs.getBoolean("assistant_ai_enabled", true)
    }

    fun loadHistory() {
        if (!isAssistantEnabled()) {
            _uiState.value = AssistantUiState.Empty
            return
        }

        _uiState.value = AssistantUiState.Loading

        if (firestore != null) {
            historyListener?.remove()
            try {
                historyListener = firestore.collection("assistant_chats")
                    .document(currentUserId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _uiState.value = AssistantUiState.Error(error.localizedMessage ?: "Gagal memuat riwayat chat")
                            return@addSnapshotListener
                        }

                        if (snapshot == null || snapshot.isEmpty) {
                            _uiState.value = if (inMemoryMessages.isEmpty()) {
                                AssistantUiState.Empty
                            } else {
                                AssistantUiState.Success(inMemoryMessages.toList(), isLoading = false)
                            }
                            return@addSnapshotListener
                        }

                        val loaded = snapshot.documents.mapNotNull { doc ->
                            try {
                                val encryptedText = doc.getString("text") ?: ""
                                val decryptedText = EncryptionHelper.decryptString(encryptedText)
                                val statusStr = doc.getString("status") ?: MessageStatus.READ.name
                                val status = try {
                                    MessageStatus.valueOf(statusStr)
                                } catch (_: Exception) {
                                    MessageStatus.READ
                                }

                                Message(
                                    id = doc.id,
                                    chatId = "assistant",
                                    senderId = doc.getString("senderId") ?: "",
                                    senderName = doc.getString("senderName") ?: "Jayra AI",
                                    text = decryptedText,
                                    timestamp = doc.getString("timeFormatted") ?: doc.getString("timestamp") ?: "12:00 PM",
                                    status = status
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }

                        inMemoryMessages.clear()
                        inMemoryMessages.addAll(loaded)

                        _uiState.value = if (loaded.isEmpty()) {
                            AssistantUiState.Empty
                        } else {
                            AssistantUiState.Success(loaded, isLoading = false)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = AssistantUiState.Error(e.localizedMessage ?: "Terjadi kesalahan saat memuat chat")
            }
        } else {
            // Offline / In-memory fallback
            _uiState.value = if (inMemoryMessages.isEmpty()) {
                AssistantUiState.Empty
            } else {
                AssistantUiState.Success(inMemoryMessages.toList(), isLoading = false)
            }
        }
    }

    fun sendMessage(text: String) {
        if (!isAssistantEnabled()) {
            _toastEvents.tryEmit("Asisten AI dinonaktifkan")
            return
        }

        if (text.isBlank()) return

        // 3.3 Rate Limiting: 1 message per 2 seconds (2000 ms)
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastMessageTimestamp < 2000L) {
            _toastEvents.tryEmit("Tunggu sebentar")
            return
        }
        lastMessageTimestamp = nowMs

        // 3.2 Anti-Injection Input Sanitization
        val sanitized = text.replace(Regex("[<>\"'%;()&+]"), "").trim()
        val dangerousPattern = Regex("(?i).*(DROP\\s+TABLE|ALTER\\s+TABLE|DELETE\\s+FROM|<script|javascript:|SELECT\\s+\\*).*")
        if (dangerousPattern.matches(text) || sanitized.isEmpty()) {
            _toastEvents.tryEmit("Input tidak valid")
            return
        }

        val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val userMsgId = UUID.randomUUID().toString()
        val userMsg = Message(
            id = userMsgId,
            chatId = "assistant",
            senderId = currentUserId,
            senderName = "User",
            text = sanitized,
            timestamp = timeFormatted,
            status = MessageStatus.SENDING
        )

        lastFailedPrompt = sanitized

        // Optimistic UI update
        val currentMessages = when (val state = _uiState.value) {
            is AssistantUiState.Success -> state.messages
            else -> emptyList()
        }
        val updatedMessages = currentMessages + userMsg
        inMemoryMessages.clear()
        inMemoryMessages.addAll(updatedMessages)
        _uiState.value = AssistantUiState.Success(messages = updatedMessages, isLoading = true)

        // Snapshot immutable untuk dipakai di coroutine (hindari mutable reference)
        val historySnapshot: List<Message> = updatedMessages.toList()

        viewModelScope.launch {
            try {
                val aiText = withContext(Dispatchers.IO) {
                    val historyBlock = historySnapshot.dropLast(1).takeLast(10).joinToString("\n") { m ->
                        val role = if (m.senderId == "jayra_ai") "Jayra AI" else "User"
                        "$role: ${m.text.take(500)}"
                    }
                    val prompt = """Kamu adalah Jayra AI — asisten pribadi serbaguna seperti Meta AI.

GAYA JAWABAN (WAJIB):
1. Jawab LENGKAP dan KONTEKSTUAL. Jangan one-liner kecuali memang pertanyaan sepele.
2. Jika pertanyaan butuh langkah/rencana, berikan poin-poin terstruktur (list/heading).
3. Sambungkan ke riwayat percakapan — jika user merujuk "di atas", "yang tadi", pahami konteksnya.
4. Jangan mengulang sapaan "Halo/Hai" atau "Saya Jayra AI" setelah pesan pertama.
5. Jangan pernah menampilkan token kontrol seperti <|start|>, <|end|>, <|im_start|>, <|assistant|> — output hanya teks bersih.
6. Bahasa sesuai bahasa user (Indonesia/English/dll). Nada ramah, cerdas, tidak bertele-tele.
7. Jika tidak yakin, katakan tidak tahu. Jangan halusinasi.
8. Gunakan markdown untuk struktur (heading, bullet, bold, code block) bila membantu.

Riwayat percakapan:
$historyBlock

User: $sanitized

Jayra AI (jawab lengkap & kontekstual):"""

                    // Fallback berlapis: 11 endpoint AI (prioritas Siputzx karena cepat + stabil)
                    var localAiText: String? = null
                    
                    // 1. Siputzx gptoss120b (utama, paling cepat + pintar)
                    try {
                        val res = siputzxApi.askGptOss(prompt = prompt, system = "You are Jayra AI, helpful assistant.")
                        if (res.isSuccessful && res.body()?.status == true) {
                            localAiText = res.body()?.getExtractedText()
                        }
                    } catch (_: Exception) {}

                    // 2. Siputzx glm47flash
                    if (localAiText.isNullOrBlank()) {
                        try {
                            val res = siputzxApi.askGlm47Flash(prompt = prompt, system = "You are Jayra AI, helpful assistant.")
                            if (res.isSuccessful && res.body()?.status == true) {
                                localAiText = res.body()?.getExtractedText()
                            }
                        } catch (_: Exception) {}
                    }

                    // 3. Siputzx qwq32b
                    if (localAiText.isNullOrBlank()) {
                        try {
                            val res = siputzxApi.askQwq32b(prompt = prompt, system = "You are Jayra AI, helpful assistant.")
                            if (res.isSuccessful && res.body()?.status == true) {
                                localAiText = res.body()?.getExtractedText()
                            }
                        } catch (_: Exception) {}
                    }

                    // 4. Siputzx phi2
                    if (localAiText.isNullOrBlank()) {
                        try {
                            val res = siputzxApi.askPhi2(prompt = prompt, system = "You are Jayra AI, helpful assistant.")
                            if (res.isSuccessful && res.body()?.status == true) {
                                localAiText = res.body()?.getExtractedText()
                            }
                        } catch (_: Exception) {}
                    }

                    // 6. Siputzx deepseekr1 (paling pintar, tapi lambat ~15-30s)
                    if (localAiText.isNullOrBlank()) {
                        try {
                            val res = siputzxApi.askDeepseekR1(prompt = prompt, system = "You are Jayra AI, helpful assistant.")
                            if (res.isSuccessful && res.body()?.status == true) {
                                localAiText = res.body()?.getExtractedText()
                            }
                        } catch (_: Exception) {}
                    }

                    // 7-11. Nexadev fallback (kadang error, taruh terakhir)
                    if (localAiText.isNullOrBlank()) {
                        val nexadevEndpoints = listOf<suspend () -> Response<NexadevAiResponse>>(
                            { nexadevAiApi.askGpt4(query = prompt) },
                            { nexadevAiApi.askChatGptPro(query = prompt) },
                            { nexadevAiApi.askFelo(query = prompt) },
                            { nexadevAiApi.askLlama(query = prompt) },
                            { nexadevAiApi.askDeepseek(text = prompt) }
                        )
                        for (call in nexadevEndpoints) {
                            try {
                                val res = call()
                                if (res.isSuccessful && res.body() != null) {
                                    localAiText = res.body()?.extractText()
                                    if (!localAiText.isNullOrBlank()) break
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    localAiText?.let { raw ->
                        val stripped = raw
                            .replace(Regex("<\\|[^|>]*\\|>"), "")            // <|start|> <|end|> dll
                            .replace(Regex("(?i)\\b(im_start|im_end|start|end|assistant|system|user)\\b(?=[\\s:])"), "")
                            .replace(Regex("^(Halo|Hai|Hey|Hello)\\!?[,.\\s]+", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("^(Saya Jayra AI[,.]?\\s*)", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("^(Asisten[^.]+\\.\\s*)", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("\\n{3,}"), "\n\n")
                            .trim()
                        if (stripped.isBlank()) raw else stripped
                    }
                }

                if (!aiText.isNullOrBlank()) {
                    val aiTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    val aiMsg = Message(
                        id = UUID.randomUUID().toString(),
                        chatId = "assistant",
                        senderId = "jayra_ai",
                        senderName = "Jayra AI",
                        text = aiText ?: "",
                        timestamp = aiTime,
                        status = MessageStatus.READ
                    )

                    val deliveredUserMsg = userMsg.copy(status = MessageStatus.READ)
                    val finalMessages = updatedMessages.dropLast(1) + deliveredUserMsg + aiMsg
                    inMemoryMessages.clear()
                    inMemoryMessages.addAll(finalMessages)

                    _uiState.value = AssistantUiState.Success(messages = finalMessages, isLoading = false)
                    lastFailedPrompt = null

                    saveMessageToFirestore(deliveredUserMsg)
                    saveMessageToFirestore(aiMsg)
                } else {
                    handleSendFailure(userMsg, "Semua endpoint AI gagal. Coba lagi nanti.")
                }
            } catch (e: Exception) {
                handleSendFailure(userMsg, e.localizedMessage ?: "Terjadi kesalahan koneksi")
            }
        }
    }

    private fun handleSendFailure(userMsg: Message, errorMessage: String) {
        val currentMessages = when (val state = _uiState.value) {
            is AssistantUiState.Success -> state.messages
            else -> emptyList()
        }
        val failedMsg = userMsg.copy(status = MessageStatus.FAILED)
        val finalMessages = currentMessages.map { if (it.id == userMsg.id) failedMsg else it }
        inMemoryMessages.clear()
        inMemoryMessages.addAll(finalMessages)

        _uiState.value = AssistantUiState.Success(messages = finalMessages, isLoading = false)
        _toastEvents.tryEmit(errorMessage)
    }

    private fun saveMessageToFirestore(message: Message) {
        if (firestore == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // AES256-GCM encryption before saving to Firestore
                val encryptedText = EncryptionHelper.encryptString(message.text)
                val docData = hashMapOf(
                    "id" to message.id,
                    "senderId" to message.senderId,
                    "senderName" to message.senderName,
                    "text" to encryptedText,
                    "timeFormatted" to message.timestamp,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to message.status.name
                )

                firestore.collection("assistant_chats")
                    .document(currentUserId)
                    .collection("messages")
                    .document(message.id)
                    .set(docData)
            } catch (_: Exception) {
                // Keep local state intact even if Firestore remote sync is delayed
            }
        }
    }

    fun onRetry() {
        val prompt = lastFailedPrompt
        if (!prompt.isNullOrBlank()) {
            sendMessage(prompt)
        }
    }

    fun onClearHistory() {
        inMemoryMessages.clear()
        _uiState.value = AssistantUiState.Empty
        lastFailedPrompt = null

        if (firestore != null) {
            try {
                firestore.collection("assistant_chats")
                    .document(currentUserId)
                    .collection("messages")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        snapshot?.documents?.forEach { doc ->
                            doc.reference.delete()
                        }
                    }
            } catch (_: Exception) {
                // Handled gracefully
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        historyListener?.remove()
    }
}
