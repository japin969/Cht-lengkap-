package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.KitsuAnime
import com.jayra.app.data.KitsuApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AnimeUiState(
    val isLoading: Boolean = false,
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val streamUrl: String? = null,
    val error: String? = null
)

class AnimeViewModel : ViewModel() {
    // Sumber data baru: Kitsu API (API lama apii.nexadev.my.id sudah mati / 503).
    private val api = KitsuApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val popular = runCatching {
                    api.list(limit = 20, sort = "-userCount").body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val ongoing = runCatching {
                    api.list(limit = 20, sort = "-userCount", status = "current")
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val completed = runCatching {
                    api.list(limit = 20, sort = "-averageRating", status = "finished")
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                val hasData = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    popular = popular.map { it.toItem() },
                    ongoing = ongoing.map { it.toItem() },
                    completed = completed.map { it.toItem() },
                    error = if (hasData) null else "Gagal memuat daftar anime"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun retry() = loadHome()

    fun loadDetail(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.detail(url)
                val data = res.body()?.data
                if (res.isSuccessful && data != null) {
                    val attrs = data.attributes
                    val info = buildMap {
                        attrs?.status?.let { put("Status", it) }
                        attrs?.startDate?.let { put("Mulai", it) }
                        attrs?.episodeCount?.let { put("Episode", "$it") }
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = AnimeDetailData(
                            title = attrs?.displayTitle(),
                            image = attrs?.posterImage?.best()?.takeIf { it.isNotBlank() }
                                ?: attrs?.coverImage?.best(),
                            synopsis = attrs?.synopsis,
                            rating = attrs?.averageRating?.toDoubleOrNull(),
                            totalEpisodes = attrs?.episodeCount,
                            genres = null,
                            info = info,
                            episodes = emptyList()
                        ),
                        episodes = emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat detail anime"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat detail anime"
                )
            }
        }
    }

    fun loadStream(url: String) {
        // Kitsu tidak menyediakan streaming; detail tetap bisa dibuka.
        _uiState.value = _uiState.value.copy(streamUrl = null)
    }

    private fun KitsuAnime.toItem(): AnimeSearchItem = AnimeSearchItem(
        title = attributes?.displayTitle(),
        link = id,
        image = attributes?.posterImage?.best()?.takeIf { it.isNotBlank() }
            ?: attributes?.coverImage?.best(),
        latestEpisode = attributes?.episodeCount?.let { "$it eps" } ?: attributes?.status,
        type = attributes?.subtype
    )
}
