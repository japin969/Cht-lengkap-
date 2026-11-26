package com.jayra.app.ui.drama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaItem
import com.jayra.app.data.DramaDetailResponse
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.NexadevApi
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
    private val tvmazeApi = TvmazeApi.create()
    private val nexadevApi = NexadevApi.create()
    private val _uiState = MutableStateFlow(DramaUiState())
    val uiState: StateFlow<DramaUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = tvmazeApi.shows(page = 0)
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
                
                // Fetch from both APIs in parallel
                val tvmazeResponse = tvmazeApi.detail(id, embed = "episodes")
                val nexadevResponse = nexadevApi.getDramaDetail(url = dramaId)
                
                val tvmazeShow = tvmazeResponse.body()
                val nexadevDetail = nexadevResponse.body()
                
                if (tvmazeResponse.isSuccessful && tvmazeShow != null) {
                    // Merge episodes with video URLs from nexadev
                    val tvmazeEpisodes = tvmazeShow.embedded?.episodes.orEmpty()
                    val nexadevEpisodes = nexadevDetail?.episodes.orEmpty()
                    
                    val mergedEpisodes = mergeEpisodesWithVideoUrls(tvmazeEpisodes, nexadevEpisodes)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = tvmazeShow.toDramaDetail(),
                        episodes = mergedEpisodes,
                        error = null
                    )
                } else if (nexadevResponse.isSuccessful && nexadevDetail != null) {
                    // Fallback to nexadev only
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = nexadevDetail,
                        episodes = nexadevDetail.episodes.orEmpty(),
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

    private fun mergeEpisodesWithVideoUrls(
        tvmazeEpisodes: List<com.jayra.app.data.TvmazeEpisode>,
        nexadevEpisodes: List<com.jayra.app.data.DramaEpisode>
    ): List<com.jayra.app.data.DramaEpisode> {
        // Create a map of nexadev episodes by index/title for lookup
        val nexadevMap = mutableMapOf<Int, com.jayra.app.data.DramaEpisode>()
        nexadevEpisodes.forEach { ep ->
            ep.index?.let { nexadevMap[it] = ep }
            // Also try matching by name if index is null
            ep.name?.let { name -> 
                if (ep.index == null) {
                    nexadevMap[name.hashCode()] = ep
                }
            }
        }
        
        return tvmazeEpisodes.map { tvEp ->
            val videoEp = nexadevMap[tvEp.number] ?: nexadevMap[tvEp.name?.hashCode() ?: -1]
            com.jayra.app.data.DramaEpisode(
                index = tvEp.number,
                name = tvEp.name,
                unlock = videoEp?.unlock ?? false,
                mp4 = videoEp?.mp4,
                videoUrl = videoEp?.videoUrl
            )
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