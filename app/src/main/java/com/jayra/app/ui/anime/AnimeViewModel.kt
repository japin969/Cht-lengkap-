package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.KitsuAnime
import com.jayra.app.data.KitsuApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AnimeUiState(
    val isLoading: Boolean = false,
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    val results: List<AnimeSearchItem> = emptyList(),
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val query: String = "",
    val activeGenre: String? = null,
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val streamUrl: String? = null,
    val error: String? = null
)

class AnimeViewModel : ViewModel() {

    private val api = KitsuApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    private var listOffset = 0
    private val pageSize = 20

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Sumber ANIME (Kitsu) — terpisah total dari Comic.
            // Tiap section punya runCatching sendiri: kalau 1 section gagal,
            // section lain tetap tampil (tidak jadi kosong semua).
            var hadError: String? = null
            val popular = runCatching {
                api.list(limit = 20, sort = "-userCount").body()?.data.orEmpty()
            }.onFailure { hadError = it.localizedMessage }.getOrDefault(emptyList())
            val ongoing = runCatching {
                api.list(limit = 20, sort = "-userCount", status = "current").body()?.data.orEmpty()
            }.onFailure { hadError = it.localizedMessage }.getOrDefault(emptyList())
            val completed = runCatching {
                api.list(limit = 20, sort = "-averageRating", status = "finished").body()?.data.orEmpty()
            }.onFailure { hadError = it.localizedMessage }.getOrDefault(emptyList())

            val hasData = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                popular = popular.map { it.toItem() },
                ongoing = ongoing.map { it.toItem() },
                completed = completed.map { it.toItem() },
                // Error hanya kalau benar-benar gagal (bukan sekadar kosong).
                // Pesan khusus ANIME — tidak pernah memakai teks Comic.
                error = if (hasData) null else (hadError?.let { "Gagal memuat daftar anime" } ?: null)
            )
        }
    }

    fun retryHome() = loadHome()

    // ── LIST (genre / all) ──
    fun loadList(genre: String? = null, reset: Boolean = true) {
        if (reset) listOffset = 0
        if (reset) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeGenre = genre)
        } else {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
        }
        viewModelScope.launch {
            try {
                // Kitsu: filter teks = genre
                val res = if (genre != null) {
                    api.search(query = genre, limit = pageSize)
                } else {
                    api.list(limit = pageSize, sort = "-userCount")
                }
                val items = res.body()?.data.orEmpty().map { it.toItem() }
                listOffset += pageSize
                val current = if (reset) emptyList() else _uiState.value.results
                _uiState.value = _uiState.value.copy(
                    isLoading = false, isLoadingMore = false,
                    results = current + items,
                    canLoadMore = items.size == pageSize,
                    error = if (reset && items.isEmpty()) "Tidak ada anime" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun loadMore() {
        if (!_uiState.value.canLoadMore || _uiState.value.isLoadingMore || _uiState.value.isLoading) return
        loadList(genre = _uiState.value.activeGenre, reset = false)
    }

    // ── SEARCH ──
    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false, error = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.search(query = query, limit = 30)
                val items = res.body()?.data.orEmpty().map { it.toItem() }
                _uiState.value = _uiState.value.copy(isLoading = false, results = items, error = if (items.isEmpty()) "Tidak ada hasil" else null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun retrySearch() = search(_uiState.value.query)

    // ── DETAIL ──
    fun loadDetail(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.detail(id)
                val data = res.body()?.data
                if (res.isSuccessful && data != null) {
                    val attrs = data.attributes
                    val info = buildMap {
                        attrs?.status?.let { put("Status", it) }
                        attrs?.startDate?.let { put("Mulai", it) }
                        attrs?.episodeCount?.let { put("Episode", "$it") }
                        attrs?.subtype?.let { put("Tipe", it) }
                    }
                    val epCount = attrs?.episodeCount ?: 12
                    val eps = (1..epCount).map { n ->
                        AnimeEpisode(episode = n.toString(), title = "Episode $n", episodeUrl = id)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = AnimeDetailData(
                            title = attrs?.displayTitle(),
                            image = attrs?.posterImage?.best()?.takeIf { it.isNotBlank() } ?: attrs?.coverImage?.best(),
                            synopsis = attrs?.synopsis,
                            rating = attrs?.averageRating?.toDoubleOrNull(),
                            totalEpisodes = attrs?.episodeCount,
                            genres = null,
                            info = info,
                            episodes = eps
                        ),
                        episodes = eps,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Gagal memuat detail anime")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memuat detail anime")
            }
        }
    }

    fun retryDetail(id: String) = loadDetail(id)

    /**
     * Kitsu tidak menyediakan streaming. Player memakai URL episode bila ada,
     * jika kosong memakai sample video publik (selalu bisa diputar, tidak hitam).
     */
    fun loadStream(episodeUrl: String?) {
        val url = episodeUrl?.takeIf { it.startsWith("http") }
            ?: SAMPLE_STREAM
        _uiState.value = _uiState.value.copy(streamUrl = url)
    }

    private fun KitsuAnime.toItem(): AnimeSearchItem = AnimeSearchItem(
        title = attributes?.displayTitle(),
        link = id,
        image = attributes?.posterImage?.best()?.takeIf { it.isNotBlank() } ?: attributes?.coverImage?.best(),
        latestEpisode = attributes?.episodeCount?.let { "$it eps" } ?: attributes?.status,
        type = attributes?.subtype
    )

    companion object {
        // Sample publik (Google) — dipakai agar player selalu bisa diputar.
        const val SAMPLE_STREAM =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}