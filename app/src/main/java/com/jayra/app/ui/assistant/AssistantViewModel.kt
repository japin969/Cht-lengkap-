package com.jayra.app.ui.assistant

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AIResponse
import com.jayra.app.data.EncryptionHelper
import com.jayra.app.data.SiputzxApi
import com.jayra.app.model.AiMode
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.tasks.await

class AssistantViewModel @JvmOverloads constructor(
    application: Application,
    private val siputzxApi: SiputzxApi = SiputzxApi.create(),
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

    init { loadHistory() }

    fun isAssistantEnabled(): Boolean = prefs.getBoolean("assistant_ai_enabled", true)

    fun getMode(): AiMode {
        val raw = prefs.getString("assistant_ai_mode", AiMode.NORMAL.name) ?: AiMode.NORMAL.name
        return try { AiMode.valueOf(raw) } catch (_: Exception) { AiMode.NORMAL }
    }

    fun setMode(mode: AiMode) {
        prefs.edit().putString("assistant_ai_mode", mode.name).apply()
        when (val cur = _uiState.value) {
            is AssistantUiState.Success -> _uiState.value = cur.copy(mode = mode)
            is AssistantUiState.Empty -> _uiState.value =
                AssistantUiState.Success(emptyList(), isLoading = false, mode = mode)
            else -> { }
        }
        _toastEvents.tryEmit(if (mode == AiMode.THINKING) "Mode: Berpikir (DeepSeek R1)" else "Mode: Biasa (GPT-OSS)")
    }

    fun loadHistory() {
        if (!isAssistantEnabled()) { _uiState.value = AssistantUiState.Empty; return }
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
                            val previous = _uiState.value as? AssistantUiState.Success
                            _uiState.value = if (inMemoryMessages.isEmpty()) AssistantUiState.Empty
                                else AssistantUiState.Success(
                                    messages = inMemoryMessages.toList(),
                                    isLoading = previous?.isLoading ?: false,
                                    mode = getMode()
                                )
                            return@addSnapshotListener
                        }

                        val loaded = snapshot.documents.mapNotNull { doc ->
                            try {
                                val encryptedText = doc.getString("text") ?: ""
                                val decryptedText = EncryptionHelper.decryptString(encryptedText)
                                val statusStr = doc.getString("status") ?: MessageStatus.READ.name
                                val status = try { MessageStatus.valueOf(statusStr) } catch (_: Exception) { MessageStatus.READ }
                                Message(
                                    id = doc.id,
                                    chatId = "assistant",
                                    senderId = doc.getString("senderId") ?: "",
                                    senderName = doc.getString("senderName") ?: "Jayra AI",
                                    text = decryptedText,
                                    timestamp = doc.getString("timeFormatted") ?: "12:00 PM",
                                    status = status,
                                    thinkText = doc.getString("thinkText")
                                )
                            } catch (_: Exception) { null }
                        }
                        // Jangan replace total: gabungkan snapshot remote dengan pesan lokal
                        // yang belum tersimpan (SENDING/FAILED atau balasan AI yang baru tampil),
                        // supaya pesan tidak "muncul lalu hilang".
                        val merged = mergeWithLocalMessages(loaded)
                        val previous = _uiState.value as? AssistantUiState.Success
                        inMemoryMessages.clear()
                        inMemoryMessages.addAll(merged)
                        _uiState.value = if (merged.isEmpty()) {
                            if (previous?.isLoading == true) {
                                AssistantUiState.Success(emptyList(), isLoading = true, mode = getMode())
                            } else {
                                AssistantUiState.Empty
                            }
                        } else {
                            AssistantUiState.Success(
                                messages = merged,
                                isLoading = previous?.isLoading ?: false,
                                mode = getMode()
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = AssistantUiState.Error(e.localizedMessage ?: "Terjadi kesalahan saat memuat chat")
            }
        } else {
            _uiState.value = if (inMemoryMessages.isEmpty()) AssistantUiState.Empty
                else AssistantUiState.Success(inMemoryMessages.toList(), isLoading = false, mode = getMode())
        }
    }

    fun sendMessage(text: String) {
        if (!isAssistantEnabled()) { _toastEvents.tryEmit("Asisten AI dinonaktifkan"); return }
        if (text.isBlank()) return

        val nowMs = System.currentTimeMillis()
        if (nowMs - lastMessageTimestamp < 2000L) { _toastEvents.tryEmit("Tunggu sebentar"); return }
        lastMessageTimestamp = nowMs

        val sanitized = text.replace(Regex("[<>\"'%;()&+]"), "").trim()
        val dangerousPattern = Regex("(?i).*(DROP\\s+TABLE|ALTER\\s+TABLE|DELETE\\s+FROM|<script|javascript:|SELECT\\s+\\*).*")
        if (dangerousPattern.matches(text) || sanitized.isEmpty()) {
            _toastEvents.tryEmit("Input tidak valid"); return
        }

        val mode = getMode()
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

        val currentMessages = when (val state = _uiState.value) {
            is AssistantUiState.Success -> state.messages
            else -> emptyList()
        }
        val updatedMessages = currentMessages + userMsg
        inMemoryMessages.clear()
        inMemoryMessages.addAll(updatedMessages)
        _uiState.value = AssistantUiState.Success(messages = updatedMessages, isLoading = true, mode = mode)

        val historySnapshot = updatedMessages.toList()

        viewModelScope.launch {
            try {
                val (thinkText, aiText) = withContext(Dispatchers.IO) {
                    val historyBlock = historySnapshot.dropLast(1).takeLast(10).joinToString("\n") { m ->
                        val role = if (m.senderId == "jayra_ai") "Jayra AI" else "User"
                        "$role: ${m.text.take(500)}"
                    }

                    // Deteksi bahasa dari pesan terakhir user (bukan nebak-nebak di sisi API).
                    val userLang = detectLanguage(sanitized)
                    val langDirective = languageDirective(userLang)

                    val prompt = """Kamu adalah Jayra AI — asisten pribadi serbaguna seperti Meta AI di WhatsApp. Cerdas, sopan, hangat, dan bisa diandalkan untuk apapun.

## IDENTITAS
- Nama: Jayra AI
- Peran: Asisten pribadi serbaguna (curhat, riset, coding, hitung, translate, kreatif, apapun)
- Kepribadian: Ramah, sopan, langsung ke inti, tidak bertele-tele, tidak menggurui

## PRINSIP JAWABAN (WAJIB DIPATUHI)
1. BERPIKIR DULU, BARU JAWAB. Analisis pertanyaan secara kritis sebelum merespons. Jangan refleks menjawab.
2. ZERO HALUSINASI. Kalau tidak tahu, katakan "Saya tidak tahu" atau "Saya tidak yakin". JANGAN mengarang fakta, angka, tanggal, nama, URL, atau referensi.
3. JAWAB LENGKAP & KONTEKSTUAL. Jangan one-liner kecuali pertanyaan sepele. Kalau butuh langkah, beri poin terstruktur (list/heading/nomor).
4. INGAT KONTEKS PERCAKAPAN. Kalau user merujuk "yang tadi", "di atas", "itu", sambungkan ke riwayat.
5. BAHASA USER — ATURAN KERAS, TIDAK BOLEH DILANGGAR:
    * WAJIB jawab dengan bahasa yang SAMA PERSIS dengan bahasa user di pesan terakhir.
    * Kalau user pakai Bahasa Indonesia (termasuk bahasa gaul: gua, lu, bro, wkwk), jawab FULL Bahasa Indonesia gaul yang santai. JANGAN campur Bahasa Inggris, kecuali istilah teknis yang memang tidak ada padanannya (misal: API, bug, commit).
    * JANGAN PERNAH campur 2 bahasa dalam 1 jawaban. Tidak boleh 1 kalimat Inggris lalu 1 kalimat Indonesia.
    * Kalau user pakai Bahasa Inggris, jawab FULL Bahasa Inggris.
    * Kalau user pakai bahasa lain, ikuti bahasa itu sepenuhnya.
    * Teks proses berpikir (think) juga WAJIB memakai bahasa yang sama dengan user. Jangan think pakai Inggris kalau jawaban final Indonesia.
    * Samakan tone: user santai -> santai; user formal -> formal. Jangan tiba-tiba formal seperti skripsi kalau user ngobrol santai.
    * Jangan terjemahkan nama fitur, nama file, atau kode. Kode tetap Inggris, penjelasannya pakai bahasa user.
6. SOPAN & HANGAT. Seperti teman pintar, bukan robot kaku atau dosen menggurui.
7. JANGAN ULANG SAPAAN. Setelah pesan pertama, jangan mulai dengan "Halo/Hai/Hey". Langsung jawab.
8. JANGAN BOCORKAN PROSES INTERNAL. Jangan tulis "Let me think", "Okay the user asked", "First I need to". Langsung beri jawaban final.
9. MARKDOWN UNTUK STRUKTUR. Gunakan heading (##), bullet (-), bold (**), code block bila membantu keterbacaan.
10. JANGAN TAMPILKAN TOKEN KONTROL seperti <|start|>, <|end|>, <|im_start|>, <|assistant|>, <|user|>, <|system|>. Output hanya teks bersih.
11. SERBA GUNA. Bisa bantu: curhat/emosional, riset/edukasi, coding/debug, hitung/math, translate, nulis kreatif, brainstorm ide, ringkas dokumen, jelaskan konsep rumit dengan sederhana.
12. KALAU PERTANYAAN AMBIGU, tanya balik dengan sopan untuk klarifikasi, jangan asumsi.
13. PANJANG JAWABAN PROPORSIONAL. Pertanyaan sepele -> jawab singkat. Pertanyaan kompleks -> jawab tuntas.

Riwayat percakapan:
$historyBlock

$langDirective

User: $sanitized

Jayra AI:"""

                    callAi(mode, prompt, userLang)
                }

                if (aiText.isNotBlank()) {
                    val aiTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    val aiMsg = Message(
                        id = UUID.randomUUID().toString(),
                        chatId = "assistant",
                        senderId = "jayra_ai",
                        senderName = "Jayra AI",
                        text = aiText,
                        timestamp = aiTime,
                        status = MessageStatus.READ,
                        thinkText = thinkText
                    )

                    val deliveredUserMsg = userMsg.copy(status = MessageStatus.READ)
                    val finalMessages = updatedMessages.dropLast(1) + deliveredUserMsg + aiMsg
                    inMemoryMessages.clear()
                    inMemoryMessages.addAll(finalMessages)
                    _uiState.value = AssistantUiState.Success(messages = finalMessages, isLoading = false, mode = mode)
                    lastFailedPrompt = null
                    saveBothToFirestoreSequential(deliveredUserMsg, aiMsg)
                } else {
                    handleSendFailure(userMsg, "Endpoint AI sedang sibuk. Coba lagi sebentar.", mode)
                }
            } catch (e: Exception) {
                handleSendFailure(userMsg, e.localizedMessage ?: "Terjadi kesalahan koneksi", mode)
            }
        }
    }

    private suspend fun callAi(mode: AiMode, prompt: String, userLang: String = "id"): Pair<String?, String> {
        val langName = when (userLang) {
            "en" -> "English"
            "id" -> "Bahasa Indonesia"
            else -> userLang
        }
        val system = "You are Jayra AI, helpful assistant. Ikuti semua PRINSIP JAWABAN di prompt user. " +
            "STRICT LANGUAGE RULE: User language: $userLang ($langName). You MUST answer fully in $langName only — " +
            "never mix two languages in one answer. Technical terms (API, bug, commit, file names, code) stay in English. " +
            "If the model produces a thinking/reasoning block, it must also be written fully in $langName."
        return when (mode) {
            AiMode.NORMAL -> {
                try {
                    val res = siputzxApi.askGptOss(prompt = prompt, system = system)
                    if (res.isSuccessful && res.body()?.status == true) {
                        null to (extractRawText(res.body()) ?: "").trim()
                    } else null to ""
                } catch (_: Exception) { null to "" }
            }
            AiMode.THINKING -> {
                try {
                    val res = siputzxApi.askDeepseekR1(prompt = prompt, system = system)
                    if (res.isSuccessful && res.body()?.status == true) {
                        parseThinkBlock(extractRawText(res.body()) ?: "")
                    } else null to ""
                } catch (_: Exception) { null to "" }
            }
        }
    }

    /**
     * Deteksi sederhana bahasa pesan terakhir user supaya hasilnya tidak bergantung
     * pada tebakan model. Skor kata kunci ID vs EN, fallback ke "id" (mayoritas user).
     */
    private fun detectLanguage(text: String): String {
        val lower = text.lowercase(Locale.US)
        val words = Regex("[a-z']+").findAll(lower).map { it.value }.toList()
        if (words.isEmpty()) return "id"

        val idWords = setOf(
            "gua", "gue", "gw", "aku", "saya", "lu", "lo", "kamu", "anda", "kalian", "kita", "kami",
            "bro", "bang", "kak", "sis", "gan", "om", "tante",
            "gak", "ga", "nggak", "enggak", "gaada", "gabisa", "udah", "udh", "belum", "blm",
            "banget", "bgt", "banyak", "sedikit", "gimana", "gmn", "kenapa", "knp", "apa", "apaan",
            "siapa", "kapan", "dimana", "mana", "bagaimana", "tolong", "bantu", "bikinin", "bikin",
            "buat", "buatin", "jangan", "bisa", "bisakah", "gabut", "santai", "serius", "becanda",
            "wkwk", "wkwkwk", "hehe", "haha", "anjir", "anjay", "mantap", "keren", "bagus", "jelek",
            "makasih", "thanks", "terima", "kasih", "dong", "sih", "nih", "tuh", "deh", "yah", "ya",
            "iya", "engga", "ngga", "sama", "sekali", "banget", "nih", "coba", "cobain", "mau", "pengen",
            "pengin", "harus", "gimana", "kok", "emang", "memang", "tetep", "tetap", "udahan", "lanjut",
            "pusing", "capek", "lelah", "salah", "bener", "benar", "kalo", "kalau", "gitu", "gt",
            "kayak", "kaya", "biar", "dulu", "nanti", "sekarang", "hari", "besok", "kemarin"
        )
        val enWords = setOf(
            "the", "is", "are", "was", "were", "you", "your", "i", "me", "my", "we", "they", "he", "she",
            "it", "what", "how", "why", "when", "where", "who", "which", "please", "can", "could",
            "would", "should", "will", "do", "does", "did", "not", "nothing", "don't", "doesn't",
            "this", "that", "these", "those", "with", "for", "from", "about", "there", "here",
            "thanks", "thank", "hello", "hi", "hey", "good", "bad", "great", "want", "need",
            "make", "build", "fix", "help", "explain", "tell", "give", "show", "just", "very",
            "and", "but", "or", "because", "so", "if", "then", "also", "have", "has", "had"
        )

        var idScore = 0
        var enScore = 0
        words.forEach { w ->
            if (w in idWords) idScore++
            if (w in enWords) enScore++
        }

        return when {
            idScore == 0 && enScore == 0 -> {
                // Tanpa penanda: anggap Indonesia kalau ada huruf latin (default app ID).
                "id"
            }
            idScore >= enScore -> "id"
            else -> "en"
        }
    }

    private fun languageDirective(lang: String): String = when (lang) {
        "en" -> "## USER LANGUAGE: en (English)\nThe user's last message is in ENGLISH. Answer FULLY in English. Do not mix Indonesian. Thinking block must be in English too."
        else -> "## USER LANGUAGE: id (Bahasa Indonesia)\nPesan terakhir user memakai BAHASA INDONESIA. Jawab FULL Bahasa Indonesia (gaul/santai kalau user santai). JANGAN campur Bahasa Inggris kecuali istilah teknis. Blok thinking juga wajib Bahasa Indonesia."
    }

    private fun extractRawText(body: AIResponse?): String? {
        if (body == null) return null
        return when (val d = body.data) {
            is String -> d
            is Map<*, *> -> (d["response"] ?: d["text"] ?: d["result"] ?: d["message"])?.toString()
            null -> body.result ?: body.message
            else -> d.toString()
        }
    }

    private fun parseThinkBlock(raw: String): Pair<String?, String> {
        val match = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL).find(raw)
        val think = match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        val answer = raw.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
        return think to answer
    }

    private fun handleSendFailure(userMsg: Message, errorMessage: String, mode: AiMode) {
        val currentMessages = when (val state = _uiState.value) {
            is AssistantUiState.Success -> state.messages
            else -> emptyList()
        }
        val failedMsg = userMsg.copy(status = MessageStatus.FAILED)
        val finalMessages = currentMessages.map { if (it.id == userMsg.id) failedMsg else it }
        inMemoryMessages.clear()
        inMemoryMessages.addAll(finalMessages)
        _uiState.value = AssistantUiState.Success(messages = finalMessages, isLoading = false, mode = mode)
        _toastEvents.tryEmit(errorMessage)
    }

    /**
     * Gabungkan daftar dari Firestore dengan pesan yang masih lokal (belum tersimpan).
     * Pesan remote jadi acuan urutan; pesan lokal yang belum ada di remote tetap
     * dipertahankan di akhir supaya bubble optimistik / balasan AI tidak hilang.
     */
    private fun mergeWithLocalMessages(remote: List<Message>): List<Message> {
        if (remote.isEmpty()) return inMemoryMessages.toList()
        val remoteIds = remote.map { it.id }.toHashSet()
        val localOnly = inMemoryMessages.filter { it.id !in remoteIds }
        if (localOnly.isEmpty()) return remote
        return remote + localOnly
    }

    private fun saveBothToFirestoreSequential(userMsg: Message, aiMsg: Message) {
        if (firestore == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseTime = System.currentTimeMillis()
                val userData = hashMapOf(
                    "id" to userMsg.id,
                    "senderId" to userMsg.senderId,
                    "senderName" to userMsg.senderName,
                    "text" to EncryptionHelper.encryptString(userMsg.text),
                    "timeFormatted" to userMsg.timestamp,
                    "timestamp" to baseTime,
                    "status" to userMsg.status.name
                )
                val aiData = hashMapOf(
                    "id" to aiMsg.id,
                    "senderId" to aiMsg.senderId,
                    "senderName" to aiMsg.senderName,
                    "text" to EncryptionHelper.encryptString(aiMsg.text),
                    "timeFormatted" to aiMsg.timestamp,
                    "timestamp" to (baseTime + 1L),
                    "status" to aiMsg.status.name,
                    "thinkText" to (aiMsg.thinkText ?: "")
                )
                val col = firestore.collection("assistant_chats")
                    .document(currentUserId)
                    .collection("messages")
                col.document(userMsg.id).set(userData).await()
                col.document(aiMsg.id).set(aiData).await()
            } catch (_: Exception) { }
        }
    }

    fun onRetry() {
        val prompt = lastFailedPrompt
        if (!prompt.isNullOrBlank()) sendMessage(prompt)
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
                        snapshot?.documents?.forEach { doc -> doc.reference.delete() }
                    }
            } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        historyListener?.remove()
    }
}
