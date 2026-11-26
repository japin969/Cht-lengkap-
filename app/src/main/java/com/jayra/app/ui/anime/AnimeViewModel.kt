package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeKuindoApi
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.KuindoDay
import com.jayra.app.data.KuindoGenre
import com.jayra.app.data.KuindoItem
import kotlinx.coroutines.async
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
    val servers: List<Pair<String, String>> = emptyList(),
    val error: String? = null,
    // section tambahan (semua dari animekuindo, PARELAL)
    val hero: AnimeSearchItem? = null,
    val latest: List<AnimeSearchItem> = emptyList(),
    val movie: List<AnimeSearchItem> = emptyList(),
    val schedule: List<KuindoDay> = emptyList(),
    val genres: List<KuindoGenre> = emptyList(),
    val seasons: List<KuindoGenre> = emptyList()
)

/**
 * AnimeViewModel — SATU sumber: https://www.sankavollerei.web.id (animekuindo).
 * Semua section dimuat PARELAL (async), bukan fallback/berurutan.
 */
class AnimeViewModel : ViewModel() {

    private val api = AnimeKuindoApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    private var listPage = 1
    private val pageSize = 20

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // PARELAL: semua section sekaligus
            val popularD = async { runCatching { api.popular().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val latestD = async { runCatching { api.latest().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val ongoingD = async { runCatching { api.home().body()?.data?.latest.orEmpty() }.getOrDefault(emptyList()) }
            val movieD = async { runCatching { api.movie().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val homeD = async { runCatching { api.home().body()?.data }.getOrNull() }

            val popular = popularD.await().map { it.toSearch() }
            val latest = latestD.await().map { it.toSearch() }
            val ongoing = ongoingD.await().map { it.toSearch() }
            val movie = movieD.await().map { it.toSearch() }
            val hero = homeD.await()?.hero?.toSearch()

            val hasData = popular.isNotEmpty() || latest.isNotEmpty() || ongoing.isNotEmpty() || movie.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                popular = popular,
                latest = latest,
                ongoing = ongoing,
                movie = movie,
                hero = hero,
                completed = movie, // "movie" dipakai sebagai section selesai/film
                error = if (hasData) null else "Gagal memuat daftar anime"
            )
        }
    }

    fun retryHome() = loadHome()

    // ── LIST (genre / season / all) — PARELAL terhadap state ──
    fun loadList(genre: String? = null, reset: Boolean = true) {
        if (reset) listPage = 1
        if (reset) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeGenre = genre)
        } else {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
        }
        viewModelScope.launch {
            try {
                val items: List<AnimeSearchItem> = if (genre != null) {
                    api.genre(genre).body()?.data.orEmpty().map { it.toSearch() }
                } else {
                    api.latest().body()?.data.orEmpty().map { it.toSearch() }
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
        if (_uiState.value.activeGenre != null) return
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
                val items = api.search(query).body()?.data.orEmpty().map { it.toSearch() }
                _uiState.value = _uiState.value.copy(isLoading = false, results = items, error = if (items.isEmpty()) "Tidak ada hasil" else null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun retrySearch() = search(_uiState.value.query)

    // ── DETAIL ──
    fun loadDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.detail(slug)
                val d = res.body()?.data
                if (res.isSuccessful && d != null) {
                    val eps = d.episode_list.orEmpty().map {
                        AnimeEpisode(episode = it.episode, title = it.title, episodeUrl = it.slug)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = AnimeDetailData(
                            title = d.title,
                            image = d.poster,
                            synopsis = d.synopsis,
                            rating = d.rating?.toDoubleOrNull(),
                            totalEpisodes = eps.size,
                            genres = d.genres?.mapNotNull { it.name },
                            info = d.info,
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

    fun retryDetail(slug: String) = loadDetail(slug)

    // ── STREAM (player) — PARELAL resolve server ──
    fun loadStream(episodeSlug: String?) {
        if (episodeSlug.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(streamUrl = null, servers = emptyList())
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val ep = api.episode(episodeSlug).body()?.data
                val servers = ep?.streams.orEmpty().mapNotNull { s ->
                    val u = s.url ?: return@mapNotNull null
                    (s.server ?: "Server") to u
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    servers = servers,
                    streamUrl = servers.firstOrNull()?.second,
                    error = if (servers.isEmpty()) "Server streaming tidak tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    // ── schedule / genres / seasons (PARELAL) ──
    fun loadSchedule() {
        viewModelScope.launch {
            val sched = async { runCatching { api.schedule().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val genres = async { runCatching { api.genres().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val seasons = async { runCatching { api.seasons().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            _uiState.value = _uiState.value.copy(
                schedule = sched.await(), genres = genres.await(), seasons = seasons.await()
            )
        }
    }

    fun loadGenres() {
        viewModelScope.launch {
            val genres = async { runCatching { api.genres().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val seasons = async { runCatching { api.seasons().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            _uiState.value = _uiState.value.copy(genres = genres.await(), seasons = seasons.await())
        }
    }

    private fun KuindoItem.toSearch(): AnimeSearchItem = AnimeSearchItem(
        title = title,
        link = slug,
        image = poster,
        latestEpisode = episode ?: status,
        type = type
    )

    companion object {
        // Fallback legal (CC BY 3.0) hanya jika server kosong — bukan sumber utama.
        const val SAMPLE_STREAM =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}