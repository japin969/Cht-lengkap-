package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.AnimasuRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

data class AnimeUiState(
    // --- Home (Tab-based) ---
    val isLoading: Boolean = false,
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val schedule: Map<String, List<AnimeSearchItem>> = emptyMap(),
    val ongoingPage: Int = 1,
    val completedPage: Int = 1,
    val popularPage: Int = 1,
    val hasMoreOngoing: Boolean = true,
    val hasMoreCompleted: Boolean = true,
    val hasMorePopular: Boolean = true,
    /** true = sedang menampilkan data contoh karena server gagal. */
    val isFallback: Boolean = false,
    val error: String? = null,

    // --- Search ---
    val isSearchLoading: Boolean = false,
    val searchResults: List<AnimeSearchItem> = emptyList(),
    val searchError: String? = null,

    // --- Detail ---
    val isDetailLoading: Boolean = false,
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val detailError: String? = null,

    // --- Stream ---
    val streamUrl: String? = null,
    val isStreamLoading: Boolean = false,
    val streamError: String? = null
)

data class SectionsData(
    val ongoing: List<AnimeSearchItem>,
    val completed: List<AnimeSearchItem>,
    val popular: List<AnimeSearchItem>,
    val schedule: Map<String, List<AnimeSearchItem>>
)

class AnimeViewModel : ViewModel() {

    private val repository = AnimasuRepository()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    // Search query flow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var searchJob: kotlinx.coroutines.Job? = null

    init { 
        loadHome()
        observeSearchQuery()
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500) // 500ms debounce
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        executeSearch(query)
                    } else {
                        _uiState.update { it.copy(searchResults = emptyList(), searchError = null, isSearchLoading = false) }
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isFallback = false) }
            val result = runCatching { loadAllSections() }
            val data = result.getOrNull()
            if (data != null) {
                val hasData = data.ongoing.isNotEmpty() || data.completed.isNotEmpty() || data.popular.isNotEmpty() || data.schedule.isNotEmpty()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        ongoing = data.ongoing,
                        completed = data.completed,
                        popular = data.popular,
                        schedule = data.schedule,
                        ongoingPage = 1,
                        completedPage = 1,
                        popularPage = 1,
                        hasMoreOngoing = data.ongoing.size >= 20,
                        hasMoreCompleted = data.completed.size >= 20,
                        hasMorePopular = data.popular.size >= 20,
                        isFallback = false,
                        error = null
                    )
                }
                return@launch
            }

            // Fallback data
            _uiState.update {
                it.copy(
                    isLoading = false,
                    ongoing = fallbackAnime,
                    completed = emptyList(),
                    popular = emptyList(),
                    schedule = emptyMap(),
                    isFallback = true,
                    error = result.exceptionOrNull()?.userMessage() ?: "Server anime tidak merespons. Menampilkan data contoh."
                )
            }
        }
    }

    private suspend fun loadAllSections(): SectionsData =
        coroutineScope {
            val ongoingDeferred = async { loadOngoing(1) }
            val completedDeferred = async { loadCompleted(1) }
            val popularDeferred = async { loadPopular(1) }
            val scheduleDeferred = async { loadSchedule() }

            val ongoing = runCatching { ongoingDeferred.await() }.getOrDefault(emptyList())
            val completed = runCatching { completedDeferred.await() }.getOrDefault(emptyList())
            val popular = runCatching { popularDeferred.await() }.getOrDefault(emptyList())
            val schedule = runCatching { scheduleDeferred.await() }.getOrDefault(emptyMap())

            SectionsData(ongoing, completed, popular, schedule)
        }

    private suspend fun loadOngoing(page: Int): List<AnimeSearchItem> {
        val response = repository.getOngoing(page)
        return if (response.isSuccess) {
            response.getOrNull()?.data?.animeList?.map { it.toAnimeSearchItem() } ?: emptyList()
        } else emptyList()
    }

    private suspend fun loadCompleted(page: Int): List<AnimeSearchItem> {
        val response = repository.getCompleted(page)
        return if (response.isSuccess) {
            response.getOrNull()?.data?.animeList?.map { it.toAnimeSearchItem() } ?: emptyList()
        } else emptyList()
    }

    private suspend fun loadPopular(page: Int): List<AnimeSearchItem> {
        val response = repository.getPopular(page)
        return if (response.isSuccess) {
            response.getOrNull()?.data?.animeList?.map { it.toAnimeSearchItem() } ?: emptyList()
        } else emptyList()
    }

    private suspend fun loadSchedule(): Map<String, List<AnimeSearchItem>> {
        val response = repository.getSchedule()
        return if (response.isSuccess) {
            response.getOrNull()?.data?.schedule?.mapValues { (_, list) ->
                list.map { it.toAnimeSearchItem() }
            } ?: emptyMap()
        } else emptyMap()
    }

    fun loadMoreOngoing() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.isLoading || !current.hasMoreOngoing) return@launch
            val nextPage = current.ongoingPage + 1
            val response = repository.getOngoing(nextPage)
            if (response.isSuccess) {
                val newItems = response.getOrNull()?.data?.animeList?.map { it.toAnimeSearchItem() } ?: emptyList()
                if (newItems.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            ongoing = it.ongoing + newItems,
                            ongoingPage = nextPage,
                            hasMoreOngoing = newItems.size >= 20
                        )
                    }
                } else {
                    _uiState.update { it.copy(hasMoreOngoing = false) }
                }
            }
        }
    }

    fun loadMoreCompleted() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.isLoading || !current.hasMoreCompleted) return@launch
            val nextPage = current.completedPage + 1
            val response = repository.getCompleted(nextPage)
            if (response.isSuccess) {
                val newItems = response.getOrNull()?.data?.animeList?.map { it.toAnimeSearchItem() } ?: emptyList()
                if (newItems.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            completed = it.completed + newItems,
                            completedPage = nextPage,
                            hasMoreCompleted = newItems.size >= 20
                        )
                    }
                } else {
                    _uiState.update { it.copy(hasMoreCompleted = false) }
                }
            }
        }
    }

    fun loadMorePopular() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.isLoading || !current.hasMorePopular) return@launch
            val nextPage = current.popularPage + 1
            val response = repository.getPopular(nextPage)
            if (response.isSuccess) {
                val newItems = response.getOrNull()?.data?.animeList?.map { it.toAnimeSearchItem() } ?: emptyList()
                if (newItems.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            popular = it.popular + newItems,
                            popularPage = nextPage,
                            hasMorePopular = newItems.size >= 20
                        )
                    }
                } else {
                    _uiState.update { it.copy(hasMorePopular = false) }
                }
            }
        }
    }

    fun retry() = loadHome()

    private fun executeSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoading = true, searchError = null) }
            val response = repository.search(query, 1)
            if (response.isSuccess) {
                val results = response.getOrNull()?.data?.results?.map { it.toAnimeSearchItem() } ?: emptyList()
                _uiState.update { it.copy(isSearchLoading = false, searchResults = results, searchError = null) }
            } else {
                _uiState.update { 
                    it.copy(
                        isSearchLoading = false, 
                        searchError = response.exceptionOrNull()?.userMessage() ?: "Gagal mencari anime"
                    ) 
                }
            }
        }
    }

    fun retrySearch() {
        val query = _searchQuery.value
        if (query.isNotBlank()) executeSearch(query)
    }

    /** Terima URL detail dari Animasu (contoh: https://v1.animasu.work/anime/jujutsu-kaisen-subtitle-indo/) */
    fun loadDetail(detailUrl: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isDetailLoading = true, detailError = null, detail = null, episodes = emptyList())
            }
            if (detailUrl.isBlank()) {
                _uiState.update { it.copy(isDetailLoading = false, detailError = "Anime tidak ditemukan") }
                return@launch
            }

            val response = repository.getDetail(detailUrl)
            if (response.isSuccess) {
                val detailData = response.getOrNull()?.data?.toAnimeDetailData()
                if (detailData != null) {
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            detail = detailData,
                            episodes = detailData.episodes ?: emptyList(),
                            detailError = null
                        )
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    isDetailLoading = false,
                    detailError = response.exceptionOrNull()?.userMessage() ?: "Gagal memuat detail anime"
                )
            }
        }
    }

    fun retryDetail(detailUrl: String) = loadDetail(detailUrl)

    /** Terima URL episode dari Animasu (contoh: https://v1.animasu.work/nonton-jujutsu-kaisen-tv-episode-10-indo/) */
    fun loadStream(episodeUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStreamLoading = true, streamError = null, streamUrl = null) }
            if (episodeUrl.isBlank()) {
                _uiState.update { it.copy(isStreamLoading = false, streamError = "URL episode kosong") }
                return@launch
            }

            val response = repository.getStream(episodeUrl)
            if (response.isSuccess) {
                val streamUrl = response.getOrNull()?.data?.getBestStreamUrl()
                if (streamUrl != null && streamUrl.isNotBlank()) {
                    _uiState.update {
                        it.copy(isStreamLoading = false, streamUrl = streamUrl, streamError = null)
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    isStreamLoading = false,
                    streamError = response.exceptionOrNull()?.userMessage() ?: "Gagal memuat stream video"
                )
            }
        }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is java.net.UnknownHostException -> "Tidak ada koneksi internet"
        is java.net.SocketTimeoutException -> "Server anime lambat / timeout. Coba lagi."
        is javax.net.ssl.SSLException -> "Koneksi aman ke server anime gagal"
        is java.io.IOException -> localizedMessage ?: "Koneksi bermasalah"
        else -> localizedMessage ?: "Koneksi bermasalah"
    }

    companion object {
        /**
         * Data contoh terakhir kalau server Animasu tidak bisa dihubungi.
         */
        private val fallbackAnime: List<AnimeSearchItem> = listOf(
            fallback("Attack on Titan", "https://v1.animasu.work/anime/attack-on-titan-subtitle-indo/", "https://media.kitsu.io/anime/poster_images/7442/large.jpg"),
            fallback("Jujutsu Kaisen", "https://v1.animasu.work/anime/jujutsu-kaisen-subtitle-indo/", "https://media.kitsu.io/anime/poster_images/41370/large.jpg"),
            fallback("Demon Slayer", "https://v1.animasu.work/anime/kimetsu-no-yaiba-subtitle-indo/", "https://media.kitsu.io/anime/poster_images/40028/large.jpg"),
            fallback("One Piece", "https://v1.animasu.work/anime/one-piece-subtitle-indo/", "https://media.kitsu.io/anime/poster_images/19/large.jpg"),
            fallback("Naruto", "https://v1.animasu.work/anime/naruto-subtitle-indo/", "https://media.kitsu.io/anime/poster_images/11/large.jpg"),
            fallback("Spy x Family", "https://v1.animasu.work/anime/spy-x-family-subtitle-indo/", "https://media.kitsu.io/anime/poster_images/44511/large.jpg"),
            fallback("Chainsaw Man", "https://v1.animasu.work/anime/chainsaw-man-subtitle-indo/", "https://media.kitsu.io/anime/poster_images/47819/large.jpg"),
            fallback("Blue Lock", "https://v1.animasu.work/anime/blue-lock-subtitle-indo/", "https://media.kitsu.io/anime/poster_images/47778/large.jpg")
        )

        private fun fallback(title: String, link: String, image: String) = AnimeSearchItem(
            title = title,
            link = link,
            image = image,
            latestEpisode = "Contoh",
            type = "TV"
        )
    }
}