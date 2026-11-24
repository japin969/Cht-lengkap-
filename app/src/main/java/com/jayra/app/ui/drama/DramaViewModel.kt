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
    val sections: Map<String, List<DramaItem>> = emptyMap(),
    val offline: Boolean = false,
    val homeError: String? = null,

    val searchResults: List<DramaItem> = emptyList(),
    val searchOffline: Boolean = false,

    val detailLoading: Boolean = false,
    val detail: DramaItem? = null,
    val episodes: List<DramaEpisode> = emptyList(),
    val detailOffline: Boolean = false,
    val detailError: String? = null,

    val streamLoading: Boolean = false,
    val streamUrl: String? = null,
    val streamError: String? = null
)

/**
 * ViewModel Drama (Strem) — Sankavollerei donghua + donghub.
 * Cache-first (Repository). UI observe state.
 */
class DramaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DramaRepository.get(app)
    private val _uiState = MutableStateFlow(DramaUiState())
    val uiState: StateFlow<DramaUiState> = _uiState.asStateFlow()

    private var homeLoaded = false
    private var lastSlug: String? = null
    private var lastStreamSlug: String? = null

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
                homeError = if (result.empty) "Gagal memuat drama" else null
            )
        }
    }

    fun retry() { homeLoaded = false; loadHome(force = true) }

    suspend fun search(query: String): List<DramaItem> {
        if (query.isBlank()) { _uiState.value = _uiState.value.copy(searchResults = emptyList()); return emptyList() }
        val result = repo.search(query)
        _uiState.value = _uiState.value.copy(searchResults = result.items, searchOffline = result.offline)
        return result.items
    }

    fun loadDetail(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(detailLoading = false, detailError = "Drama tidak ditemukan")
            return
        }
        lastSlug = slug
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLoading = true, detailError = null)
            val result = repo.detail(slug)
            if (result.detail != null) {
                val d = result.detail
                _uiState.value = _uiState.value.copy(
                    detailLoading = false,
                    detail = DramaItem(
                        title = d.resolvedTitle, cover = d.resolvedCover,
                        synopsis = d.resolvedSynopsis, rating = d.resolvedRating,
                        genres = d.resolvedGenres, episodes = d.resolvedEpisodes,
                        status = d.status, slug = slug
                    ),
                    episodes = d.resolvedEpisodes,
                    detailOffline = result.offline,
                    detailError = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    detailLoading = false,
                    detailError = "Gagal memuat detail drama. Coba lagi."
                )
            }
        }
    }

    fun retryDetail() { lastSlug?.let { loadDetail(it) } }

    fun loadStream(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(streamLoading = false, streamError = "Episode tidak ditemukan")
            return
        }
        lastStreamSlug = slug
        _uiState.value = _uiState.value.copy(streamLoading = true, streamUrl = null, streamError = null)
        viewModelScope.launch {
            val url = repo.stream(slug)
            _uiState.value = _uiState.value.copy(
                streamLoading = false,
                streamUrl = url,
                streamError = if (url == null) "Sumber video belum tersedia" else null
            )
        }
    }

    fun retryStream() { lastStreamSlug?.let { loadStream(it) } }
}