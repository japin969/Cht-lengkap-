package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeApi
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.AnimeStreamServer
import com.jayra.app.data.BloggerStreamExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AnimeUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<AnimeSearchItem> = emptyList(),
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val isDetailLoading: Boolean = false,
    val isStreamLoading: Boolean = false,
    val streamUrl: String? = null,
    val streamTitle: String? = null,
    val streamServers: List<AnimeStreamServer> = emptyList(),
    val error: String? = null,
    val detailError: String? = null,
    val streamError: String? = null
)

@OptIn(FlowPreview::class)
class AnimeViewModel : ViewModel() {
    private val api = AnimeApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")
    private var streamJob: Job? = null

    init {
        loadHome()
        viewModelScope.launch {
            _searchFlow
                .debounce(450L)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isNotBlank()) {
                        performSearch(q)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            searchResults = emptyList()
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchFlow.value = query.trim()
    }

    fun searchNow() {
        val q = _uiState.value.searchQuery.trim()
        if (q.isNotBlank()) {
            viewModelScope.launch { performSearch(q) }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true, error = null)
        try {
            val res = withContext(Dispatchers.IO) { api.search(query) }
            if (res.isSuccessful && res.body()?.success == true) {
                val items = res.body()?.data?.results.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = items
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = emptyList()
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                error = e.localizedMessage ?: "Gagal mencari anime"
            )
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val ongoingDef = async(Dispatchers.IO) {
                    runCatching { api.ongoing().body()?.data?.animeList.orEmpty() }.getOrDefault(emptyList())
                }
                val popularDef = async(Dispatchers.IO) {
                    runCatching { api.popular().body()?.data?.animeList.orEmpty() }.getOrDefault(emptyList())
                }
                val completedDef = async(Dispatchers.IO) {
                    runCatching { api.completed().body()?.data?.animeList.orEmpty() }.getOrDefault(emptyList())
                }

                val ongoing = ongoingDef.await()
                val popular = popularDef.await()
                val completed = completedDef.await()

                val hasAny = ongoing.isNotEmpty() || popular.isNotEmpty() || completed.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    ongoing = ongoing,
                    popular = popular,
                    completed = completed,
                    error = if (!hasAny) "Gagal memuat daftar anime. Periksa koneksi atau coba lagi." else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Terjadi kesalahan jaringan"
                )
            }
        }
    }

    fun loadDetail(url: String) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(detailError = "URL anime tidak valid")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetailLoading = true, detailError = null)
            try {
                val res = withContext(Dispatchers.IO) { api.detail(url) }
                val body = res.body()
                if (res.isSuccessful && body?.success == true && body.data != null) {
                    _uiState.value = _uiState.value.copy(
                        isDetailLoading = false,
                        detail = body.data,
                        episodes = body.data.episodes.orEmpty()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDetailLoading = false,
                        detailError = "Gagal memuat detail anime"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDetailLoading = false,
                    detailError = e.localizedMessage ?: "Gagal memuat detail"
                )
            }
        }
    }

    fun loadStream(url: String) {
        if (url.isBlank()) return
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isStreamLoading = true,
                streamError = null,
                streamUrl = null,
                streamServers = emptyList()
            )
            try {
                val result = withContext(Dispatchers.IO) {
                    val res = api.stream(url)
                    val data = res.body()?.data
                    if (!res.isSuccessful || res.body()?.success != true || data == null) {
                        return@withContext null
                    }

                    val rawServers = data.parsedServers()
                    val finalServers = mutableListOf<AnimeStreamServer>()

                    for (srv in rawServers) {
                        if (BloggerStreamExtractor.isBloggerUrl(srv.url)) {
                            val mp4List = BloggerStreamExtractor.extract(srv.url)
                            if (mp4List.isNotEmpty()) {
                                finalServers.addAll(mp4List)
                            } else {
                                finalServers.add(srv)
                            }
                        } else {
                            finalServers.add(srv)
                        }
                    }

                    val distinctServers = finalServers.distinctBy { it.url }
                    val primary = distinctServers.firstOrNull()?.url ?: data.streamUrl
                    val title = data.episodeInfo?.heading ?: data.episodeInfo?.title
                    Triple(primary, title, distinctServers)
                }

                if (result != null && !result.first.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isStreamLoading = false,
                        streamUrl = result.first,
                        streamTitle = result.second,
                        streamServers = result.third
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isStreamLoading = false,
                        streamError = "Stream video belum tersedia untuk episode ini"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isStreamLoading = false,
                    streamError = e.localizedMessage ?: "Gagal memuat stream"
                )
            }
        }
    }

    fun selectStreamServer(url: String) {
        _uiState.value = _uiState.value.copy(streamUrl = url)
    }

    fun clearStream() {
        streamJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isStreamLoading = false,
            streamUrl = null,
            streamTitle = null,
            streamServers = emptyList(),
            streamError = null
        )
    }
}
