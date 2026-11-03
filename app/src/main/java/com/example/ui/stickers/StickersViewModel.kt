package com.example.ui.stickers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Gif
import com.example.model.Sticker
import com.example.model.StickersUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StickersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<StickersUiState>(StickersUiState.Loading)
    val uiState: StateFlow<StickersUiState> = _uiState.asStateFlow()

    private val defaultStickers = listOf(
        Sticker("s_1", "Senyum Hangat", "https://picsum.photos/seed/sticker1/200", "Populer", "😊"),
        Sticker("s_2", "Tertawa Lepas", "https://picsum.photos/seed/sticker2/200", "Populer", "😂"),
        Sticker("s_3", "Cinta & Hati", "https://picsum.photos/seed/sticker3/200", "Populer", "❤️"),
        Sticker("s_4", "Keren Berkacamata", "https://picsum.photos/seed/sticker4/200", "Populer", "😎"),
        Sticker("s_5", "Kucing Santai", "https://picsum.photos/seed/sticker5/200", "Kucing Gemas", "🐱"),
        Sticker("s_6", "Kucing Marah", "https://picsum.photos/seed/sticker6/200", "Kucing Gemas", "😾"),
        Sticker("s_7", "Kucing Tidur", "https://picsum.photos/seed/sticker7/200", "Kucing Gemas", "🐾"),
        Sticker("s_8", "Kucing Melompat", "https://picsum.photos/seed/sticker8/200", "Kucing Gemas", "😻"),
        Sticker("s_9", "Koding Semangat", "https://picsum.photos/seed/sticker9/200", "Teknologi", "💻"),
        Sticker("s_10", "Bug Ditemukan", "https://picsum.photos/seed/sticker10/200", "Teknologi", "🐛"),
        Sticker("s_11", "Deploy Sukses", "https://picsum.photos/seed/sticker11/200", "Teknologi", "🚀"),
        Sticker("s_12", "Robot Pintar", "https://picsum.photos/seed/sticker12/200", "Teknologi", "🤖")
    )

    private val defaultGifs = listOf(
        Gif("g_1", "Tepuk Tangan Meriah", "https://picsum.photos/seed/gif1/300/300", "https://picsum.photos/seed/gif1/300/300"),
        Gif("g_2", "Menari Bahagia", "https://picsum.photos/seed/gif2/300/300", "https://picsum.photos/seed/gif2/300/300"),
        Gif("g_3", "Terkejut Kaget", "https://picsum.photos/seed/gif3/300/300", "https://picsum.photos/seed/gif3/300/300"),
        Gif("g_4", "Setuju Jempol", "https://picsum.photos/seed/gif4/300/300", "https://picsum.photos/seed/gif4/300/300"),
        Gif("g_5", "Kucing Lucu Meluncur", "https://picsum.photos/seed/gif5/300/300", "https://picsum.photos/seed/gif5/300/300"),
        Gif("g_6", "Mengantuk di Kantor", "https://picsum.photos/seed/gif6/300/300", "https://picsum.photos/seed/gif6/300/300"),
        Gif("g_7", "Perayaan Kembang Api", "https://picsum.photos/seed/gif7/300/300", "https://picsum.photos/seed/gif7/300/300"),
        Gif("g_8", "Semangat Kerja", "https://picsum.photos/seed/gif8/300/300", "https://picsum.photos/seed/gif8/300/300")
    )

    private val packs = listOf("Semua", "Populer", "Kucing Gemas", "Teknologi")

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = StickersUiState.Success(
                selectedTab = 0,
                stickerPacks = packs,
                selectedPack = "Semua",
                stickers = defaultStickers,
                gifs = defaultGifs,
                searchQuery = ""
            )
        }
    }

    fun selectTab(tabIndex: Int) {
        val currentState = _uiState.value
        if (currentState is StickersUiState.Success) {
            _uiState.value = currentState.copy(selectedTab = tabIndex)
        }
    }

    fun selectPack(pack: String) {
        val currentState = _uiState.value
        if (currentState is StickersUiState.Success) {
            val filteredStickers = if (pack == "Semua") {
                defaultStickers
            } else {
                defaultStickers.filter { it.packName == pack }
            }
            _uiState.value = currentState.copy(
                selectedPack = pack,
                stickers = filteredStickers
            )
        }
    }

    fun search(query: String) {
        val currentState = _uiState.value
        if (currentState is StickersUiState.Success) {
            val filteredStickers = if (query.isBlank()) {
                if (currentState.selectedPack == "Semua") defaultStickers else defaultStickers.filter { it.packName == currentState.selectedPack }
            } else {
                defaultStickers.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.emoji.contains(query) ||
                    it.packName.contains(query, ignoreCase = true)
                }
            }
            val filteredGifs = if (query.isBlank()) {
                defaultGifs
            } else {
                defaultGifs.filter { it.title.contains(query, ignoreCase = true) }
            }

            _uiState.value = currentState.copy(
                searchQuery = query,
                stickers = filteredStickers,
                gifs = filteredGifs
            )
        }
    }
}
