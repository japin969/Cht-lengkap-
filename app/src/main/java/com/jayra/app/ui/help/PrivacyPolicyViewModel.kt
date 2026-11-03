package com.jayra.app.ui.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.model.PrivacyPolicyUiState
import com.jayra.app.model.PrivacySection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrivacyPolicyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PrivacyPolicyUiState>(PrivacyPolicyUiState.Loading)
    val uiState: StateFlow<PrivacyPolicyUiState> = _uiState.asStateFlow()

    init {
        loadPrivacyPolicy()
    }

    fun loadPrivacyPolicy() {
        viewModelScope.launch {
            val sections = listOf(
                PrivacySection(
                    id = "p_1",
                    title = "1. Prinsip Dasar Privasi Kami",
                    content = "Jayra dibangun dengan satu prinsip utama: data pribadi Anda adalah milik Anda sepenuhnya. Kami tidak pernah memonetisasi data pribadi, tidak menyisipkan pelacak analitik pihak ketiga, dan tidak menjual informasi ke pengiklan."
                ),
                PrivacySection(
                    id = "p_2",
                    title = "2. Data yang Kami Simpan",
                    content = "Kami hanya menyimpan nomor telepon atau identitas akun yang Anda gunakan untuk pendaftaran. Pesan cloud disimpan dalam bentuk terenkripsi di server terdistribusi. Obrolan rahasia (Secret Chats) tidak pernah disimpan di server mana pun dan hanya berada di perangkat pengirim dan penerima."
                ),
                PrivacySection(
                    id = "p_3",
                    title = "3. Standar Enkripsi Militer",
                    content = "Penyimpanan lokal di perangkat dilindungi dengan modul perangkat keras Android Keystore menggunakan AES-256-GCM. Kata sandi tambahan diverifikasi menggunakan hashing satu arah SHA-256 dengan garam acak (salt) unik."
                ),
                PrivacySection(
                    id = "p_4",
                    title = "4. Panggilan dan Media Real-time",
                    content = "Panggilan suara dan video didukung oleh protokol WebRTC peer-to-peer terenkripsi langsung antar perangkat. Tidak ada rekaman audio atau video yang pernah disimpan di server perantara."
                ),
                PrivacySection(
                    id = "p_5",
                    title = "5. Hak Pengguna & Penghapusan Akun",
                    content = "Anda berhak menghapus akun, riwayat pesan, dan seluruh data yang terafiliasi kapan saja melalui menu Pengaturan Privasi. Penghapusan akun bersifat permanen dan tidak dapat dipulihkan."
                )
            )

            _uiState.value = PrivacyPolicyUiState.Success(
                title = "Kebijakan Privasi Jayra",
                lastUpdated = "10 November 2025",
                intro = "Kebijakan Privasi ini menjelaskan bagaimana Jayra mengumpulkan, mengenkripsi, dan melindungi data saat Anda menggunakan layanan kami.",
                sections = sections
            )
        }
    }
}
