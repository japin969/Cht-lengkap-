package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.*
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
                val ongoing = api.ongoing().body()?.data?.animeList ?: emptyList()
                val popular = api.popular().body()?.data?.animeList ?: emptyList()
                val completed = api.completed().body()?.data?.animeList ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    ongoing = ongoing,
                    popular = popular,
                    completed = completed
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun loadDetail(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.detail(url)
                if (res.isSuccessful && res.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = res.body()?.data,
                        episodes = res.body()?.data?.episodes ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Gagal memuat detail")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun loadStream(url: String) {
        viewModelScope.launch {
            try {
                val res = api.stream(url)
                if (res.isSuccessful && res.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(streamUrl = res.body()?.data?.streamUrl)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }
}
