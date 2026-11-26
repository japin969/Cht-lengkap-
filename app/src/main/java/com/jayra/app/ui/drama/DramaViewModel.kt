package com.jayra.app.ui.drama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaItem
import com.jayra.app.data.DramaDetailResponse
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.TvmazeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DramaUiState(
    val isLoading: Boolean = false,
    val featured: List<DramaItem> = emptyList(),
    val detail: DramaDetailResponse? = null,
    val episodes: List<DramaEpisode> = emptyList(),
    val error: String? = null
)

class DramaViewModel : ViewModel() {
    private val api = TvmazeApi.create()
    private val _uiState = MutableStateFlow(DramaUiState())
    val uiState: StateFlow<DramaUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.shows(page = 0)
                val shows = response.body().orEmpty()
                val items = shows
                    .filter { it.image?.original?.isNotBlank() == true || it.image?.medium?.isNotBlank() == true }
                    .map { it.toDramaItem() }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    featured = items,
                    error = if (items.isEmpty()) "Gagal memuat daftar drama" else null
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

    fun loadDetail(dramaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val id = dramaId.toLongOrNull()
                if (id == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Drama tidak ditemukan"
                    )
                    return
                }
                val response = api.detail(id, embed = "episodes")
                val show = response.body()
                if (response.isSuccessful && show != null) {
                    val episodes = show.embedded?.episodes.orEmpty().map { ep ->
                        DramaEpisode(
                            index = ep.number,
                            name = ep.name,
                            unlock = false,
                            mp4 = null,
                            videoUrl = null
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = show.toDramaDetail(),
                        episodes = episodes,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat detail drama"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat detail drama"
                )
            }
        }
    }

    private fun TvmazeShow.toDramaDetail(): DramaDetailResponse {
        return DramaDetailResponse(
            ok = true,
            book = this.toDramaItem(),
            episodes = embedded?.episodes?.map { ep ->
                DramaEpisode(
                    index = ep.number,
                    name = ep.name,
                    unlock = false,
                    mp4 = null,
                    videoUrl = null
                )
            } ?: emptyList()
        )
    }
}