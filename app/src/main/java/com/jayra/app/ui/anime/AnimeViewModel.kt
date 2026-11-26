package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeApi
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
    // nexadev (animasu)
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    // animekuindo
    val hero: AnimeSearchItem? = null,
    val latest: List<AnimeSearchItem> = emptyList(),
    val movie: List<AnimeSearchItem> = emptyList(),
    val schedule: List<KuindoDay> = emptyList(),
    val genres: List<KuindoGenre> = emptyList(),
    val seasons: List<KuindoGenre> = emptyList(),
    // list/search/detail/player
    val results: List<AnimeSearchItem> = emptyList(),
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val query: String = "",
    val activeGenre: String? = null,
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val streamUrl: String? = null,
    val servers: List<Pair<String, String>> = emptyList(),
    val error: String? = null
)

/**
 * AnimeViewModel — SEMUA sumber PARELAL (async), tidak ada fallback/hapus:
 *  - NexaDev (animasu)      -> ongoing/popular/completed
 *  - Animekuindo (sankavollerei) -> hero/latest/movie/schedule/genres/seasons
 */
class AnimeViewModel : ViewModel() {

    private val nexa = AnimeApi.create()
    private val kuindo = AnimeKuindoApi.create()

    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    private var listPage = 1
    private val pageSize = 20

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // PARELAL — semua sumber sekaligus
            val popularD = async { runCatching { nexa.popular(page = 1).body()?.data?.animeList.orEmpty() }.getOrDefault(emptyList()) }
            val ongoingD = async { runCatching { nexa.ongoing(page = 1).body()?.data?.animeList.orEmpty() }.getOrDefault(emptyList()) }
            val completedD = async { runCatching { nexa.completed(page = 1).body()?.data?.animeList.orEmpty() }.getOrDefault(emptyList()) }

            val latestD = async { runCatching { kuindo.latest().body()?.data.orEmpty().map { it.toSearch() } }.getOrDefault(emptyList()) }
            val movieD = async { runCatching { kuindo.movie().body()?.data.orEmpty().map { it.toSearch() } }.getOrDefault(emptyList()) }
            val homeD = async { runCatching { kuindo.home().body()?.data }.getOrNull() }

            val popular = popularD.await()
            val ongoing = ongoingD.await()
            val completed = completedD.await()
            val latest = latestD.await()
            val movie = movieD.await()
            val hero = homeD.await()?.hero?.toSearch()

            val hasData = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty() ||
                latest.isNotEmpty() || movie.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                popular = popular,
                ongoing = ongoing,
                completed = completed,
                latest = latest,
                movie = movie,
                hero = hero,
                error = if (hasData) null else "Gagal memuat daftar anime"
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
                val items: List<AnimeSearchItem> = if (genre != null) {
                    kuindo.genre(genre).body()?.data.orEmpty().map { it.toSearch() }
                } else {
                    kuindo.latest().body()?.data.orEmpty().map { it.toSearch() }
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

    // ── SEARCH (nexadev + kuindo PARELAL) ──
    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false, error = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val a = async { runCatching { nexa.search(query).body()?.data?.results.orEmpty() }.getOrDefault(emptyList()) }
            val b = async { runCatching { kuindo.search(query).body()?.data.orEmpty().map { it.toSearch() } }.getOrDefault(emptyList()) }
            val items = a.await() + b.await()
            _uiState.value = _uiState.value.copy(isLoading = false, results = items, error = if (items.isEmpty()) "Tidak ada hasil" else null)
        }
    }

    fun retrySearch() = search(_uiState.value.query)

    // ── DETAIL (kuindo slug) ──
    fun loadDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = kuindo.detail(slug)
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

    // ── STREAM (kuindo episode) ──
    fun loadStream(episodeSlug: String?) {
        if (episodeSlug.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(streamUrl = null, servers = emptyList())
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val ep = kuindo.episode(episodeSlug).body()?.data
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
            val sched = async { runCatching { kuindo.schedule().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val genres = async { runCatching { kuindo.genres().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val seasons = async { runCatching { kuindo.seasons().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            _uiState.value = _uiState.value.copy(schedule = sched.await(), genres = genres.await(), seasons = seasons.await())
        }
    }


    private fun KuindoItem.toSearch(): AnimeSearchItem = AnimeSearchItem(
        title = title, link = slug, image = poster,
        latestEpisode = episode ?: status, type = type
    )

    companion object {
        const val SAMPLE_STREAM =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}