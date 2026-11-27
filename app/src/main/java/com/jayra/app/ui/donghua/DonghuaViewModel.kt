package com.jayra.app.ui.donghua

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DonghuaRepository
import com.jayra.app.data.DonghuaCardItem
import com.jayra.app.data.DonghuaDetailResult
import com.jayra.app.ui.donghua.DonghuaEpisodeItem
import com.jayra.app.data.DonghuaGenresResult
import com.jayra.app.data.DonghuaHomeResult
import com.jayra.app.data.DonghuaSearchResult
import com.jayra.app.data.DonghuaSectionResult
import com.jayra.app.data.DonghuaStreamUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DonghuaUiState(
    // Home
    val home: DonghuaHomeResult? = null,
    val isLoadingHome: Boolean = false,

    // Sections
    val ongoing: DonghuaSectionResult? = null,
    val completed: DonghuaSectionResult? = null,
    val latest: DonghuaSectionResult? = null,
    val popular: DonghuaSectionResult? = null,
    val movies: DonghuaSectionResult? = null,
    val schedule: DonghuaSectionResult? = null,
    val isLoadingSection: Boolean = false,

    // Genres
    val genres: DonghuaGenresResult? = null,

    // Search
    val searchResults: DonghuaSearchResult? = null,
    val isSearching: Boolean = false,

    // Detail
    val detail: DonghuaDetailResult? = null,
    val isLoadingDetail: Boolean = false,

    // Episode Stream
    val streamUrl: DonghuaStreamUrl? = null,
    val isLoadingStream: Boolean = false,

    // Error
    val error: String? = null
)

class DonghuaViewModel : ViewModel() {

    private val repository = DonghuaRepository()
    private val _uiState = MutableStateFlow(DonghuaUiState())
    val uiState: StateFlow<DonghuaUiState> = _uiState

    init {
        loadHome()
    }

    // ═══════════════════════════════════════════
    // HOME
    // ═══════════════════════════════════════════

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHome = true, error = null)
            try {
                val result = repository.getHome()
                _uiState.value = _uiState.value.copy(
                    isLoadingHome = false,
                    home = result,
                    error = if (!result.hasAnyData) "Gagal memuat beranda Donghua" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingHome = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    // ═══════════════════════════════════════════
    // SECTIONS
    // ═══════════════════════════════════════════

    fun loadOngoing(page: Int = 1) {
        loadSection(page, DonghuaRepository::getOngoing) { result ->
            _uiState.value = _uiState.value.copy(ongoing = result)
        }
    }

    fun loadCompleted(page: Int = 1) {
        loadSection(page, DonghuaRepository::getCompleted) { result ->
            _uiState.value = _uiState.value.copy(completed = result)
        }
    }

    fun loadLatest(page: Int = 1) {
        loadSection(page, DonghuaRepository::getLatest) { result ->
            _uiState.value = _uiState.value.copy(latest = result)
        }
    }

    fun loadPopular(page: Int = 1) {
        loadSection(page, DonghuaRepository::getPopular) { result ->
            _uiState.value = _uiState.value.copy(popular = result)
        }
    }

    fun loadMovies(page: Int = 1) {
        loadSection(page, DonghuaRepository::getMovies) { result ->
            _uiState.value = _uiState.value.copy(movies = result)
        }
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSection = true)
            try {
                val result = repository.getSchedule()
                _uiState.value = _uiState.value.copy(
                    isLoadingSection = false,
                    schedule = DonghuaSectionResult(
                        donghuaItems = result.donghuaData?.data?.flatMap { it.value }?.map { DonghuaCardItem.fromDonghua(it, com.jayra.app.data.DonghuaSource.DONGHUA) } ?: emptyList(),
                        donghubItems = result.donghubData?.data?.flatMap { it.value }?.map { DonghuaCardItem.fromDonghub(it, com.jayra.app.data.DonghuaSource.DONGHUB) } ?: emptyList()
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingSection = false)
            }
        }
    }

    fun loadGenres() {
        viewModelScope.launch {
            val result = repository.getGenres()
            _uiState.value = _uiState.value.copy(genres = result)
        }
    }

    private fun loadSection(
        page: Int,
        fetch: (DonghuaRepository, Int) -> DonghuaSectionResult,
        onResult: (DonghuaSectionResult) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSection = true)
            try {
                val result = fetch(repository, page)
                _uiState.value = _uiState.value.copy(isLoadingSection = false)
                onResult(result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingSection = false)
            }
        }
    }

    // ═══════════════════════════════════════════
    // SEARCH
    // ═══════════════════════════════════════════

    fun searchDonghua(keyword: String, page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            try {
                val result = repository.searchDonghua(keyword, page)
                _uiState.value = _uiState.value.copy(isSearching = false, searchResults = result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSearching = false, error = e.localizedMessage)
            }
        }
    }

    fun searchDonghub(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            try {
                val result = repository.searchDonghub(query)
                _uiState.value = _uiState.value.copy(isSearching = false, searchResults = result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSearching = false, error = e.localizedMessage)
            }
        }
    }

    // ═══════════════════════════════════════════
    // DETAIL
    // ═══════════════════════════════════════════

    fun loadDetail(slug: String, source: DonghuaSource = DonghuaSource.DONGHUA) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetail = true, error = null)
            try {
                val result = if (source == DonghuaSource.DONGHUA) {
                    repository.getDonghuaDetail(slug)
                } else {
                    repository.getDonghubDetail(slug)
                }
                _uiState.value = _uiState.value.copy(
                    isLoadingDetail = false,
                    detail = result,
                    error = if (!result.hasData) "Gagal memuat detail" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingDetail = false,
                    error = e.localizedMessage ?: "Gagal memuat detail"
                )
            }
        }
    }

    // ═══════════════════════════════════════════
    // STREAM
    // ═══════════════════════════════════════════

    fun loadStream(slug: String, source: DonghuaSource = DonghuaSource.DONGHUA) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStream = true)
            try {
                val streamUrl = if (source == DonghuaSource.DONGHUA) {
                    repository.getDonghuaEpisode(slug)
                } else {
                    repository.getDonghubEpisode(slug)
                }
                _uiState.value = _uiState.value.copy(
                    isLoadingStream = false,
                    streamUrl = streamUrl
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStream = false,
                    streamUrl = null
                )
            }
        }
    }

    fun retry() = loadHome()
}

import com.jayra.app.data.DonghuaSource