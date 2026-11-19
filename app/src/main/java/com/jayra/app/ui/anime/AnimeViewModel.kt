package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.NexadevApi
import com.jayra.app.data.NexadevAnime
import com.jayra.app.data.NexadevAnimeListResponse
import com.jayra.app.data.NexadevAnimeDetailResponse
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
    // Sumber data baru: NexaDev API (hanya API yang diizinkan)
    private val api = NexadevApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val popular = runCatching {
                    api.getAnimeList(limit = 20, sort = "-userCount").body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val ongoing = runCatching {
                    api.getAnimeList(limit = 20, sort = "-userCount", status = "ongoing")
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val completed = runCatching {
                    api.getAnimeList(limit = 20, sort = "-averageRating", status = "completed")
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
                val res = api.getAnimeDetail(url)
                val data = res.body()?.data
                if (res.body()?.success == true && data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = AnimeDetailData(
                            title = data.title,
                            image = data.image,
                            synopsis = data.synopsis,
                            rating = data.rating?.toDoubleOrNull(),
                            totalEpisodes = data.totalEpisodes?.toIntOrNull(),
                            genres = data.genres,
                            info = buildMap {
                                data.status?.let { put("Status", it) }
                                data.startDate?.let { put("Mulai", it) }
                                data.totalEpisodes?.let { put("Episode", "$it") }
                            },
                            episodes = data.episodes?.map { ep ->
                                AnimeEpisode(
                                    episode = ep.episode,
                                    title = ep.title,
                                    episodeUrl = ep.episodeUrl
                                )
                            } ?: emptyList()
                        )
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
        // NexaDev menyediakan streaming URL
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.getAnimeStream(url)
                if (res.body()?.success == true && res.body()?.data?.streamUrl?.isNotBlank() == true) {
                    val streamUrl = res.body()?.data?.streamUrl
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streamUrl = streamUrl
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Tidak ada URL streaming tersedia"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat streaming"
                )
            }
        }
    }

    private fun NexadevAnime.toItem(): AnimeSearchItem = AnimeSearchItem(
        title = title,
        link = link,
        image = image,
        latestEpisode = totalEpisodes?.let { "$it eps" } ?: status,
        type = type
    )
}
