package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeApi
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
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
    private val api = AnimeApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val popularRes = runCatching { api.popular(page = 1).body()?.data?.animeList.orEmpty() }.getOrDefault(emptyList())
                val ongoingRes = runCatching { api.ongoing(page = 1).body()?.data?.animeList.orEmpty() }.getOrDefault(emptyList())
                val completedRes = runCatching { api.completed(page = 1).body()?.data?.animeList.orEmpty() }.getOrDefault(emptyList())

                val hasData = popularRes.isNotEmpty() || ongoingRes.isNotEmpty() || completedRes.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    popular = popularRes,
                    ongoing = ongoingRes,
                    completed = completedRes,
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
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = data,
                        episodes = data.episodes.orEmpty(),
                        error = null
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, streamUrl = null)
            try {
                val res = api.stream(url)
                val stream = res.body()?.data?.streamUrl
                    ?: res.body()?.data?.alternativeLinks?.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamUrl = stream
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat stream"
                )
            }
        }
    }
}