package com.jayra.app.ui.drama

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.DramaItem
import com.jayra.app.data.DramaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DramaUiState(
    val isLoading: Boolean = false,
    /** Section home: nama -> daftar drama. */
    val sections: Map<String, List<DramaItem>> = emptyMap(),
    val offline: Boolean = false,
    val homeError: String? = null,

    // Search
    val searchResults: List<DramaItem> = emptyList(),
    val searchOffline: Boolean = false,

    // Detail
    val detailLoading: Boolean = false,
    val detail: DramaItem? = null,
    val episodes: List<DramaEpisode> = emptyList(),
    val detailOffline: Boolean = false,
    val detailError: String? = null,

    // Play
    val streamLoading: Boolean = false,
    val streamUrl: String? = null,
    val streamError: String? = null
)

class DramaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DramaRepository.get(app)

    private val _uiState = MutableStateFlow(DramaUiState())
    val uiState: StateFlow<DramaUiState> = _uiState.asStateFlow()

    private var homeLoaded = false
    private var lastDramaId: String? = null
    private var lastStreamKey: String? = null

    init { loadHome() }

    fun loadHome(force: Boolean = false) {
        if (homeLoaded && !force) return
        homeLoaded = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            val result = repo.home()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                sections = result.sections,
                offline = result.offline,
                homeError = if (result.empty) "Gagal memuat daftar drama" else null
            )
        }
    }

    fun retry() {
        homeLoaded = false
        loadHome(force = true)
    }

    // ── Search ──────────────────────────────────────────────────────────

    suspend fun search(query: String): List<DramaItem> {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return emptyList()
        }
        val result = repo.search(query)
        _uiState.value = _uiState.value.copy(
            searchResults = result.items,
            searchOffline = result.offline
        )
        return result.items
    }

    // ── Detail ──────────────────────────────────────────────────────────

    fun loadDetail(id: String) {
        if (id.isBlank()) {
            _uiState.value = _uiState.value.copy(
                detailLoading = false,
                detailError = "Drama tidak ditemukan"
            )
            return
        }
        lastDramaId = id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLoading = true, detailError = null)
            val result = repo.detail(id)
            _uiState.value = _uiState.value.copy(
                detailLoading = false,
                detail = result.detail,
                episodes = result.episodes,
                detailOffline = result.offline,
                detailError = if (result.empty) "Gagal memuat detail drama" else null
            )
        }
    }

    fun retryDetail() { lastDramaId?.let { loadDetail(it) } }

    // ── Episode / Play ──────────────────────────────────────────────────

    /** Resolve URL mp4 untuk satu episode (lewat rate limiter + cache). */
    fun loadStream(dramaId: String, ep: Int) {
        val key = "$dramaId:$ep"
        if (lastStreamKey == key && _uiState.value.streamUrl != null) return
        lastStreamKey = key
        _uiState.value = _uiState.value.copy(
            streamLoading = true,
            streamUrl = null,
            streamError = null
        )
        viewModelScope.launch {
            val url = repo.episode(dramaId, ep)
            _uiState.value = _uiState.value.copy(
                streamLoading = false,
                streamUrl = url,
                streamError = if (url == null) "Sumber video belum tersedia" else null
            )
        }
    }

    fun retryStream() {
        val id = lastDramaId ?: return
        val ep = lastStreamKey?.substringAfterLast(":")?.toIntOrNull() ?: 1
        lastStreamKey = null
        loadStream(id, ep)
    }

    fun clearStream() {
        lastStreamKey = null
        _uiState.value = _uiState.value.copy(streamUrl = null, streamError = null)
    }
}