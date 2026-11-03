package com.jayra.app.ui.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.model.FAQ
import com.jayra.app.model.HelpUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HelpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HelpUiState>(HelpUiState.Loading)
    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

    private val allFaqs = listOf(
        FAQ(
            id = "faq_1",
            question = "Apa itu Jayra?",
            answer = "Jayra adalah aplikasi perpesanan instan yang berfokus pada kecepatan, keamanan enkripsi militer tingkat tinggi, dan privasi penuh tanpa pelacakan iklan.",
            category = "Umum"
        ),
        FAQ(
            id = "faq_2",
            question = "Bagaimana enkripsi pesan di Jayra bekerja?",
            answer = "Semua obrolan rahasia menggunakan enkripsi End-to-End (E2EE) dengan AES-256-GCM dan kurva eliptis Diffie-Hellman. Hanya Anda dan lawan bicara yang memegang kunci dekripsi.",
            category = "Keamanan"
        ),
        FAQ(
            id = "faq_3",
            question = "Bagaimana cara mengaktifkan Verifikasi Dua Langkah?",
            answer = "Buka Pengaturan -> Privasi & Keamanan -> Verifikasi Dua Langkah. Buat kata sandi tambahan dengan petunjuk dan email pemulihan untuk mengamankan akun Anda.",
            category = "Keamanan"
        ),
        FAQ(
            id = "faq_4",
            question = "Apakah panggilan suara dan video aman?",
            answer = "Ya, panggilan suara dan video menggunakan protokol WebRTC terenkripsi langsung peer-to-peer (P2P) dengan perlindungan emoji verifikasi kunci.",
            category = "Panggilan"
        ),
        FAQ(
            id = "faq_5",
            question = "Bagaimana cara mengelola penyimpanan memori?",
            answer = "Masuk ke Pengaturan -> Data & Penyimpanan. Anda dapat mengatur retensi cache media otomatis, membersihkan thumbnail, dan mengelola unduhan.",
            category = "Penyimpanan"
        ),
        FAQ(
            id = "faq_6",
            question = "Bagaimana cara menggunakan Asisten AI Jayra?",
            answer = "Buka obrolan Jayra AI dari menu Pengaturan atau daftar pesan. Asisten AI ditenagai oleh kecerdasan buatan aman untuk membantu menjawab pertanyaan Anda.",
            category = "Fitur AI"
        ),
        FAQ(
            id = "faq_7",
            question = "Apakah data saya dijual kepada pihak ketiga?",
            answer = "Tidak pernah. Jayra adalah proyek non-profit berbasis sumber terbuka. Kami tidak melacak analitik pihak ketiga dan tidak mengumpulkan riwayat pencarian Anda.",
            category = "Privasi"
        )
    )

    init {
        loadFaqs()
    }

    fun loadFaqs() {
        viewModelScope.launch {
            _uiState.value = HelpUiState.Success(
                faqs = allFaqs,
                searchQuery = "",
                filteredFaqs = allFaqs
            )
        }
    }

    fun searchFaqs(query: String) {
        val currentState = _uiState.value
        if (currentState is HelpUiState.Success) {
            val filtered = if (query.isBlank()) {
                allFaqs
            } else {
                allFaqs.filter {
                    it.question.contains(query, ignoreCase = true) ||
                    it.answer.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
                }
            }
            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredFaqs = filtered
            )
        }
    }
}
