package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeApi
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AnimeTab { POPULAR, COMPLETED, SCHEDULE, SEARCH }

data class AnimeUiState(
    val isLoading: Boolean = false,
    val isAppending: Boolean = false,
    val tab: AnimeTab = AnimeTab.POPULAR,
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    val schedule: Map<String, List<AnimeSearchItem>> = emptyMap(),
    val searchResults: List<AnimeSearchItem> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val popularPage: Int = 1,
    val completedPage: Int = 1,
    val hasMorePopular: Boolean = true,
    val hasMoreCompleted: Boolean = true,
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val streamUrl: String? = null,
    val isLoadingStream: Boolean = false,
    val error: String? = null
) {
    /** List yang sedang ditampilkan sesuai tab aktif. */
    val currentList: List<AnimeSearchItem>
        get() = when (tab) {
            AnimeTab.POPULAR -> popular
            AnimeTab.COMPLETED -> completed
            AnimeTab.SCHEDULE -> schedule.values.flatten()
            AnimeTab.SEARCH -> searchResults
        }
}

/**
 * Sumber data Anime: https://api.nexadev.my.id/animasu
 * (search / details / stream / popular / completed / schedule).
 * Semua JSON diparse ke model kita sendiri lalu dirender ulang dengan Composable.
 */
class AnimeViewModel : ViewModel() {
    private val api = AnimeApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    private var searchJob: Job? = null

    init {
        // Populer langsung dimuat saat menu Anime dibuka (jangan kosong).
        loadPopular(reset = true)
    }

    fun selectTab(tab: AnimeTab) {
        if (_uiState.value.tab == tab) return
        _uiState.value = _uiState.value.copy(tab = tab, error = null)
        when (tab) {
            AnimeTab.POPULAR -> if (_uiState.value.popular.isEmpty()) loadPopular(reset = true)
            AnimeTab.COMPLETED -> if (_uiState.value.completed.isEmpty()) loadCompleted(reset = true)
            AnimeTab.SCHEDULE -> if (_uiState.value.schedule.isEmpty()) loadSchedule()
            AnimeTab.SEARCH -> Unit
        }
    }

    fun loadPopular(reset: Boolean) {
        val page = if (reset) 1 else _uiState.value.popularPage + 1
        if (!reset && (!_uiState.value.hasMorePopular || _uiState.value.isAppending)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = reset,
                isAppending = !reset,
                error = null
            )
            try {
                val res = api.popular(page = page)
                val items = res.body()?.data?.animeList.orEmpty()
                val merged = if (reset) items else _uiState.value.popular + items
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAppending = false,
                    popular = merged,
                    popularPage = page,
                    hasMorePopular = items.isNotEmpty(),
                    error = if (merged.isEmpty()) "Gagal memuat anime populer" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAppending = false,
                    error = if (_uiState.value.popular.isEmpty()) humanError(e) else null
                )
            }
        }
    }

    fun loadCompleted(reset: Boolean) {
        val page = if (reset) 1 else _uiState.value.completedPage + 1
        if (!reset && (!_uiState.value.hasMoreCompleted || _uiState.value.isAppending)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = reset,
                isAppending = !reset,
                error = null
            )
            try {
                val res = api.completed(page = page)
                val items = res.body()?.data?.animeList.orEmpty()
                val merged = if (reset) items else _uiState.value.completed + items
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAppending = false,
                    completed = merged,
                    completedPage = page,
                    hasMoreCompleted = items.isNotEmpty(),
                    error = if (merged.isEmpty()) "Gagal memuat anime tamat" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAppending = false,
                    error = if (_uiState.value.completed.isEmpty()) humanError(e) else null
                )
            }
        }
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.schedule()
                val schedule = res.body()?.data?.schedule.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    schedule = schedule,
                    error = if (schedule.isEmpty()) "Jadwal anime belum tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = humanError(e)
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, tab = AnimeTab.SEARCH)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false, error = null)
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            try {
                val res = api.search(query.trim())
                val items = res.body()?.data?.results.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = items,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = emptyList(),
                    error = humanError(e)
                )
            }
        }
    }

    /** Dipanggil saat user scroll mendekati bawah list (infinite scroll). */
    fun loadMore() {
        when (_uiState.value.tab) {
            AnimeTab.POPULAR -> loadPopular(reset = false)
            AnimeTab.COMPLETED -> loadCompleted(reset = false)
            else -> Unit
        }
    }

    fun retry() {
        when (_uiState.value.tab) {
            AnimeTab.POPULAR -> loadPopular(reset = true)
            AnimeTab.COMPLETED -> loadCompleted(reset = true)
            AnimeTab.SCHEDULE -> loadSchedule()
            AnimeTab.SEARCH -> onSearchQueryChange(_uiState.value.searchQuery)
        }
    }

    fun loadDetail(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, detail = null, episodes = emptyList())
            try {
                val res = api.detail(url)
                val data = res.body()?.data
                if (res.isSuccessful && data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = data,
                        episodes = data.episodes.orEmpty()
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
                    error = humanError(e)
                )
            }
        }
    }

    fun loadStream(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStream = true, streamUrl = null, error = null)
            try {
                val res = api.stream(url)
                val stream = res.body()?.data?.streamUrl
                _uiState.value = _uiState.value.copy(
                    isLoadingStream = false,
                    streamUrl = stream,
                    error = if (stream.isNullOrBlank()) "Video tidak tersedia untuk episode ini" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStream = false,
                    error = humanError(e)
                )
            }
        }
    }

    private fun humanError(e: Exception): String {
        val raw = e.localizedMessage.orEmpty()
        return when {
            raw.contains("UnknownHost", true) || raw.contains("Unable to resolve", true) ->
                "Tidak ada koneksi internet"
            raw.contains("timeout", true) || raw.contains("timed out", true) ->
                "Server terlalu lama merespons"
            raw.contains("Certificate", true) || raw.contains("SSL", true) ->
                "Koneksi aman ke server gagal"
            else -> "Gagal memuat data anime"
        }
    }
}
