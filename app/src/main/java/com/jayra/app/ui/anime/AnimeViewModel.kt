package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeApi
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeListData
import com.jayra.app.data.AnimeSearchData
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.AnimeSearchResponse
import com.jayra.app.data.AnimeStreamResponse
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
) {
    val hasAnyData: Boolean
        get() = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()
}

class AnimeViewModel : ViewModel() {
    private val api = AnimeApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val popularRes = api.popular(on = 1, page = 1)
                val ongoingRes = api.ongoing(on = 1, page = 1)
                val completedRes = api.completed(on = 1, page = 1)

                val popular = popularRes.body()?.data?.animeList.orEmpty()
                val ongoing = ongoingRes.body()?.data?.animeList.orEmpty()
                val completed = completedRes.body()?.data?.animeList.orEmpty()

                val hasData = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    popular = popular,
                    ongoing = ongoing,
                    completed = completed,
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
                    val info = buildMap {
                        data.info?.forEach { (k, v) -> put(k, v) }
                    }
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
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val res = api.stream(url)
                val data = res.body()?.data
                val streamUrl = data?.streamUrl
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamUrl = streamUrl
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamUrl = null
                )
            }
        }
    }
}