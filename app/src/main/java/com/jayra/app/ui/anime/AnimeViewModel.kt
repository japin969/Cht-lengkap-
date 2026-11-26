package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeApi
import com.jayra.app.data.AnimeSankaApi
import com.jayra.app.data.AnimeSankaItem
import com.jayra.app.data.AnimeDay
import com.jayra.app.data.AnimeGenreItem
import com.jayra.app.data.NkItem
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
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
    val error: String? = null,
    val sankaOngoing: List<AnimeSearchItem> = emptyList(),
    val schedule: List<AnimeDay> = emptyList(),
    val sankaGenres: List<AnimeGenreItem> = emptyList(),
    val nkLatest: List<AnimeSearchItem> = emptyList()
)

/**
 * AnimeViewModel — sumber resmi: https://api.nexadev.my.id (animasu).
 * Terpisah total dari Comic. Poster memakai field image/poster dari API (tidak diubah).
 */
class AnimeViewModel : ViewModel() {

    private val api = AnimeApi.create()
    private val sanka = AnimeSankaApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    private var listPage = 1
    private val pageSize = 20

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            var hadError: String? = null
            // Sumber utama: nexadev (animasu)
            val popular = runCatching {
                api.popular(page = 1).body()?.data?.animeList.orEmpty()
            }.onFailure { hadError = it.localizedMessage }.getOrDefault(emptyList())
            val ongoing = runCatching {
                api.ongoing(page = 1).body()?.data?.animeList.orEmpty()
            }.onFailure { hadError = it.localizedMessage }.getOrDefault(emptyList())
            val completed = runCatching {
                api.completed(page = 1).body()?.data?.animeList.orEmpty()
            }.onFailure { hadError = it.localizedMessage }.getOrDefault(emptyList())

            // Fallback: kalau nexadev kosong/gagal, pakai sankavollerei (/anime/*)
            var hasData = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()
            var fPopular = popular
            var fOngoing = ongoing
            var fCompleted = completed
            if (!hasData) {
                val sOngoing = runCatching { sanka.ongoing().body()?.data?.animeList.orEmpty().map { it.toSearch() } }.getOrDefault(emptyList())
                val sComplete = runCatching { sanka.complete().body()?.data?.animeList.orEmpty().map { it.toSearch() } }.getOrDefault(emptyList())
                val sHome = runCatching { sanka.home().body()?.data }.getOrNull()
                fOngoing = sOngoing.ifEmpty { sHome?.ongoing?.animeList?.map { it.toSearch() }.orEmpty() }
                fCompleted = sComplete.ifEmpty { sHome?.complete?.animeList?.map { it.toSearch() }.orEmpty() }
                fPopular = fOngoing
                hasData = fPopular.isNotEmpty() || fOngoing.isNotEmpty() || fCompleted.isNotEmpty()
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                popular = fPopular,
                ongoing = fOngoing,
                completed = fCompleted,
                error = if (hasData) null else (hadError?.let { "Gagal memuat daftar anime" } ?: "Gagal memuat daftar anime")
            )
        }
    }

    fun retryHome() = loadHome()

    // ── LIST (genre / all) ──
    fun loadList(genre: String? = null, reset: Boolean = true) {
        if (reset) listPage = 1
        if (reset) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeGenre = genre)
        } else {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
        }
        viewModelScope.launch {
            try {
                // Genre di animasu = search teks; "Semua" = popular.
                val items: List<AnimeSearchItem> = if (genre != null) {
                    api.search(genre).body()?.data?.results.orEmpty()
                } else {
                    api.popular(page = listPage).body()?.data?.animeList.orEmpty()
                }
                listPage++
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
        if (_uiState.value.activeGenre != null) return // search tidak dipaginasi
        loadList(genre = null, reset = false)
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
                val items = api.search(query).body()?.data?.results.orEmpty()
                _uiState.value = _uiState.value.copy(isLoading = false, results = items, error = if (items.isEmpty()) "Tidak ada hasil" else null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun retrySearch() = search(_uiState.value.query)

    // ── DETAIL ──
    fun loadDetail(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.detail(url)
                val data = res.body()?.data
                if (res.isSuccessful && data != null) {
                    val eps = data.episodes.orEmpty()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = AnimeDetailData(
                            title = data.title,
                            image = data.image,
                            synopsis = data.sinopsis ?: data.synopsis,
                            rating = data.rating,
                            totalEpisodes = data.totalEpisodes,
                            genres = data.genres,
                            info = data.info,
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

    fun retryDetail(url: String) = loadDetail(url)

    /**
     * Ambil URL stream dari animasu. Jika kosong / gagal, player memakai demo CC BY 3.0
     * (legal) supaya tidak hitam — bukan sumber bajakan.
     */
    fun loadStream(episodeUrl: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, streamUrl = null)
            val url = runCatching {
                api.stream(episodeUrl ?: "").body()?.data?.streamUrl
            }.getOrNull()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                streamUrl = url?.takeIf { it.startsWith("http") } ?: SAMPLE_STREAM
            )
        }
    }


    // ── sankavollerei anime (home/schedule/genre) ──
    fun loadSankaHome() {
        viewModelScope.launch {
            val ongoing = runCatching { sanka.ongoing().body()?.data?.animeList.orEmpty().map { it.toSearch() } }.getOrDefault(emptyList())
            val sched = runCatching { sanka.schedule().body()?.data.orEmpty() }.getOrDefault(emptyList())
            val genres = runCatching { sanka.genres().body()?.data?.genreList.orEmpty() }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(sankaOngoing = ongoing, schedule = sched, sankaGenres = genres)
        }
    }

    fun loadNekopoi() {
        viewModelScope.launch {
            val items = runCatching { sanka.nkHome().body()?.data?.latest.orEmpty().map { it.toSearch() } }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(nkLatest = items)
        }
    }

    fun searchSanka(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = sanka.search(query).body()?.data?.animeList.orEmpty().map { it.toSearch() }
                _uiState.value = _uiState.value.copy(isLoading = false, results = items, error = if (items.isEmpty()) "Tidak ada hasil" else null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    private fun AnimeSankaItem.toSearch(): AnimeSearchItem = AnimeSearchItem(
        title = title,
        link = animeId ?: slug,
        image = poster,
        latestEpisode = episodes?.let { "$it eps" },
        type = null
    )
    private fun NkItem.toSearch(): AnimeSearchItem = AnimeSearchItem(
        title = title,
        link = slug,
        image = thumbnail,
        latestEpisode = null,
        type = "Hentai"
    )

    companion object {
        // Demo legal (CC BY 3.0) — fallback agar player tidak hitam.
        const val SAMPLE_STREAM =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}