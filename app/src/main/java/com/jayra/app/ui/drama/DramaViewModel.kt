package com.jayra.app.ui.drama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaDetail
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.DramaItem
import com.jayra.app.data.DramaRepository
import com.jayra.app.data.DramaStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DramaUiState(
    val isLoading: Boolean = false,
    val featured: List<DramaItem> = emptyList(),
    val donghua: List<DramaItem> = emptyList(),
    val donghub: List<DramaItem> = emptyList(),
    // detail
    val detail: DramaDetail? = null,
    val episodes: List<DramaEpisode> = emptyList(),
    // stream
    val streams: List<DramaStream> = emptyList(),
    val isResolving: Boolean = false,
    val error: String? = null
)

class DramaViewModel : ViewModel() {

    private val repo = DramaRepository()
    private val _uiState = MutableStateFlow(DramaUiState())
    val uiState: StateFlow<DramaUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repo.loadHome()
            val total = result.donghua.size + result.donghub.size
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                featured = result.featured,
                donghua = result.donghua,
                donghub = result.donghub,
                error = if (total == 0) "Gagal memuat drama dari server" else null
            )
        }
    }

    fun retry() = loadHome()

    /** slug + source dari Home -> Detail. */
    fun loadDetail(slug: String, source: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, detail = null, episodes = emptyList())
            val detail = repo.loadDetail(slug, source)
            if (detail == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Detail tidak ditemukan")
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = detail,
                    episodes = detail.episodes
                )
            }
        }
    }

    /** Resolve stream untuk episode terpilih -> URL m3u8/mp4 siap putar. */
    fun loadStreams(slug: String, source: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResolving = true, streams = emptyList(), error = null)
            val streams = repo.loadStreams(slug, source)
            _uiState.value = _uiState.value.copy(
                isResolving = false,
                streams = streams,
                error = if (streams.isEmpty()) "Server streaming tidak tersedia untuk episode ini" else null
            )
        }
    }

    fun clearStreams() {
        _uiState.value = _uiState.value.copy(streams = emptyList(), error = null)
    }
}