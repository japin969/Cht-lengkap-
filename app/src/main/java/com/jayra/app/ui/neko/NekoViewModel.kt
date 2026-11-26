package com.jayra.app.ui.neko

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.NekopoiApi
import com.jayra.app.data.NkItem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NekoUiState(
    val isLoading: Boolean = false,
    val latest: List<AnimeSearchItem> = emptyList(),
    val recommended: List<AnimeSearchItem> = emptyList(),
    val error: String? = null
)

/**
 * NekoViewModel — fitur STANDALONE (tidak digabung Anime/Stream).
 * Sumber: NekopoiApi (sankavollerei /anime/nekopoi). PARELAL.
 */
class NekoViewModel : ViewModel() {

    private val api = NekopoiApi.create()
    private val _uiState = MutableStateFlow(NekoUiState())
    val uiState: StateFlow<NekoUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val latestD = async { runCatching { api.latestHentai().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val homeD = async { runCatching { api.home().body()?.data }.getOrNull() }
            val latest = latestD.await().map { it.toSearch() }
            val home = homeD.await()
            val recommended = home?.recommended.orEmpty().map { it.toSearch() }
            val hasData = latest.isNotEmpty() || recommended.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                latest = latest,
                recommended = recommended,
                error = if (hasData) null else "Gagal memuat data nekopoi"
            )
        }
    }

    fun retry() = load()

    private fun NkItem.toSearch(): AnimeSearchItem = AnimeSearchItem(
        title = title, link = slug, image = thumbnail, latestEpisode = null, type = "Nekopoi"
    )
}