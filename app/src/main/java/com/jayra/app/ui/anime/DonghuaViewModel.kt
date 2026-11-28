package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DonghuaDetail
import com.jayra.app.data.DonghuaEpisode
import com.jayra.app.data.DonghuaHome
import com.jayra.app.data.DonghuaItem
import com.jayra.app.data.DonghuaRepository
import com.jayra.app.data.DonghuaSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DonghuaUiState(
    val source: DonghuaSource = DonghuaSource.DONGHUA,
    val isLoading: Boolean = false,
    val home: DonghuaHome? = null,
    val homeError: String? = null,
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<DonghuaItem> = emptyList(),
    val searchError: String? = null,
    val detail: DonghuaDetail? = null,
    val isDetailLoading: Boolean = false,
    val detailError: String? = null,
    val episode: DonghuaEpisode? = null,
    val isEpisodeLoading: Boolean = false,
    val episodeError: String? = null
)

/** SATU ViewModel untuk fitur Donghua (donghua + donghub). */
class DonghuaViewModel(
    private val repository: DonghuaRepository = DonghuaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DonghuaUiState())
    val uiState: StateFlow<DonghuaUiState> = _uiState

    private var homeJob: Job? = null
    private var searchJob: Job? = null
    private var detailJob: Job? = null
    private var episodeJob: Job? = null

    fun setSource(source: DonghuaSource, reloadHome: Boolean = true) {
        if (_uiState.value.source == source) return
        _uiState.value = _uiState.value.copy(
            source = source,
            home = if (reloadHome) null else _uiState.value.home,
            homeError = null,
            results = if (_uiState.value.query.isBlank()) emptyList() else _uiState.value.results
        )
        if (!reloadHome) return
        loadHome()
        if (_uiState.value.query.isNotBlank()) search(_uiState.value.query)
    }

    fun loadHome() {
        homeJob?.cancel()
        homeJob = viewModelScope.launch {
            val source = _uiState.value.source
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            val result = repository.home(source)
            if (_uiState.value.source != source) return@launch
            result.fold(
                onSuccess = { home ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        home = home,
                        homeError = if (home.latest.isEmpty() && home.ongoing.isEmpty() && home.completed.isEmpty() && home.hero.isEmpty()) {
                            "Daftar kosong. Coba muat ulang."
                        } else null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        homeError = error.message ?: "Gagal memuat katalog"
                    )
                }
            )
        }
    }

    fun retry() = loadHome()

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), searchError = null, isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchError = null)
            val result = repository.search(_uiState.value.source, query)
            result.fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(isSearching = false, results = items)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        results = emptyList(),
                        searchError = error.message ?: "Pencarian gagal"
                    )
                }
            )
        }
    }

    fun loadDetail(slug: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetailLoading = true, detailError = null, detail = null)
            val result = repository.detail(_uiState.value.source, slug)
            result.fold(
                onSuccess = { detail ->
                    _uiState.value = _uiState.value.copy(isDetailLoading = false, detail = detail)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isDetailLoading = false,
                        detailError = error.message ?: "Gagal memuat detail"
                    )
                }
            )
        }
    }

    fun loadEpisode(slug: String) {
        episodeJob?.cancel()
        episodeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isEpisodeLoading = true,
                episodeError = null,
                episode = null
            )
            val result = repository.episode(_uiState.value.source, slug)
            result.fold(
                onSuccess = { episode ->
                    _uiState.value = _uiState.value.copy(isEpisodeLoading = false, episode = episode)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isEpisodeLoading = false,
                        episodeError = error.message ?: "Gagal memuat episode"
                    )
                }
            )
        }
    }

    fun clearEpisodeError() {
        _uiState.value = _uiState.value.copy(episodeError = null)
    }
}
