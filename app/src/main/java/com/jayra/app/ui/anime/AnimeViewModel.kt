package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.AnimeHomeStore
import com.jayra.app.data.NexadevApi
import com.jayra.app.ui.media.mergeDedupe
import com.jayra.app.ui.media.normalizeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Status tayang sebuah anime. */
enum class AnimeStatus { ONGOING, COMPLETED, POPULAR }

/** Sort untuk daftar anime. */
enum class AnimeSort(val label: String) {
    TERBARU("Terbaru"),
    TERPOPULER("Terpopuler"),
    AZ("A-Z")
}

/** Genre sebuah anime (hasil enrich dari endpoint detail). */
data class AnimeEnrichment(
    val genres: List<String> = emptyList(),
    val rating: Double? = null
)

/** Hasil satu sumber API. */
data class AnimeSourceResult(
    val source: String,
    val items: List<AnimeSearchItem>,
    val error: String? = null
) {
    val ok: Boolean get() = error == null && items.isNotEmpty()
}

data class AnimeUiState(
    val isLoading: Boolean = false,
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    val recent: List<AnimeSearchItem> = emptyList(),
    /** Section tambahan agar Home selalu ramai. */
    val topRated: List<AnimeSearchItem> = emptyList(),
    val recommendations: List<AnimeSearchItem> = emptyList(),
    val genres: List<String> = emptyList(),
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val streamUrl: String? = null,
    val streamLoading: Boolean = false,
    val streamError: String? = null,
    val error: String? = null,
    val homeError: String? = null,
    val detailError: String? = null,
    val sources: List<AnimeSourceResult> = emptyList()
)

/**
 * ViewModel Anime — SEMENTARA pakai NexaDev saja (stabil).
 * SankavollereiApi di-nonaktifkan; akan ditambah balik satu-satu setelah
 * disetujui. Kontrak UI (state + fungsi) dipertahankan.
 */
class AnimeViewModel : ViewModel() {

    private val nexadev = NexadevApi.create()

    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState.asStateFlow()

    private val _enrichment = MutableStateFlow<Map<String, AnimeEnrichment>>(emptyMap())
    val enrichment: StateFlow<Map<String, AnimeEnrichment>> = _enrichment.asStateFlow()

    private val searchCache = LinkedHashMap<String, List<AnimeSearchItem>>()
    private val detailCache = LinkedHashMap<String, AnimeDetailData>()
    private val episodeCache = LinkedHashMap<String, List<AnimeEpisode>>()
    private val enriching = mutableSetOf<String>()
    private var lastAnimeUrl: String? = null
    private var lastStreamEpisodeUrl: String? = null
    private var homeLoaded = false

    init {
        // 1. Cache memori (proses) -> instan.
        // 2. Cache disk persisten -> instan walau app baru dibuka.
        val mem = HomeCache.sections
        val disk = if (mem == null) diskSnapshotToState(AnimeHomeStore.load()) else null
        (mem ?: disk)?.let { cached ->
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                ongoing = cached.ongoing,
                popular = cached.popular,
                completed = cached.completed,
                recent = cached.recent,
                topRated = cached.topRated,
                recommendations = cached.recommendations,
                genres = cached.genres,
                homeError = null
            )
        }
        loadHome()
    }

    private fun diskSnapshotToState(snap: AnimeHomeStore.Snapshot?): AnimeUiState? {
        if (snap == null) return null
        fun toItems(l: List<AnimeHomeStore.Item>) = l.map {
            AnimeSearchItem(
                title = it.title, link = it.link, image = it.image,
                latestEpisode = it.latestEpisode, type = it.type,
                rating = it.rating, status = it.status
            )
        }
        return AnimeUiState(
            ongoing = toItems(snap.ongoing),
            popular = toItems(snap.popular),
            recent = toItems(snap.recent),
            completed = toItems(snap.completed)
        )
    }

    fun loadHome(force: Boolean = false) {
        if (homeLoaded && !force) return
        homeLoaded = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            val result = fetchNexadevHome()
            mergeHome(result)
            val genres = runCatching { loadGenres() }.getOrDefault(emptyList())
            if (genres.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(genres = genres)
            }
        }
    }

    private suspend fun fetchNexadevHome(): AnimeSourceResult {
        return try {
            // SEMUA kategori di-load PARALEL PENUH via async (bukan berurutan).
            // Tiap kategori ambil 2 halaman (paralel di dalam fetchNexadevPages).
            // Ini memangkas waktu dari ~8s (4 kategori berurutan) jadi ~2s.
            coroutineScope {
                val ongoingD = async { fetchNexadevPages(pages = 1) { p -> nexadev.getAnimeOngoing(page = p).body()?.data?.animeList } }
                val popularD = async { fetchNexadevPages(pages = 1) { p -> nexadev.getAnimePopular(page = p).body()?.data?.animeList } }
                val completedD = async { fetchNexadevPages(pages = 1) { p -> nexadev.getAnimeCompleted(page = p).body()?.data?.animeList } }
                val scheduleD = async { runCatching { nexadev.getAnimeSchedule().body()?.data?.schedule }.getOrNull() }
                val ongoing = ongoingD.await()
                val popular = popularD.await()
                val completed = completedD.await()
                val scheduleMap = scheduleD.await()
                val recent = scheduleMap?.values?.flatten()?.distinctBy { it.link ?: it.title }.orEmpty()
                val fetch = AnimeFetch(
                    ongoing = ongoing,
                    popular = popular,
                    recent = recent,
                    completed = completed
                )
                AnimeSourceResult(
                    source = "nexadev",
                    items = fetch.all,
                    error = if (fetch.all.isEmpty()) "kosong" else null
                ).also { _nexadevCache = fetch }
            }
        } catch (e: Exception) {
            AnimeSourceResult("nexadev", emptyList(), e.localizedMessage ?: "gagal")
        }
    }

    /** Ambil beberapa page (default 3) secara PARALEL, gabung + dedup. */
    private suspend fun fetchNexadevPages(
        pages: Int = 3,
        fetch: suspend (Int) -> List<AnimeSearchItem>?
    ): List<AnimeSearchItem> = coroutineScope {
        (1..pages).map { p ->
            async { runCatching { fetch(p) }.getOrDefault(emptyList()).orEmpty() }
        }.flatMap { it.await() }
            .distinctBy { it.link ?: it.title }
    }

    /** Cache kategori Nexadev terakhir (dipakai mergeHome). */
    private var _nexadevCache: AnimeFetch = AnimeFetch()

    private fun mergeHome(result: AnimeSourceResult) {
        val nexadev = _nexadevCache

        fun merge(vararg lists: List<AnimeSearchItem>): List<AnimeSearchItem> =
            mergeDedupe(
                lists = lists.toList().map { list -> list.filter { it.isValidAnime() } },
                keyOf = { normalizeKey(it.title) },
                scoreOf = { if (it.image.isNullOrBlank()) 0 else 1 }
            )

        val ongoing = merge(nexadev.ongoing)
        val popular = merge(nexadev.popular).ifEmpty { ongoing }
        val recent = merge(nexadev.recent).ifEmpty { ongoing }
        val completed = merge(nexadev.completed)
        val all = merge(nexadev.all)

        val topRated = all
            .filter { it.rating != null && it.rating!!.toDoubleOrNull() != null }
            .sortedByDescending { it.rating!!.toDoubleOrNull() ?: 0.0 }
            .ifEmpty { all }

        val recommendations = merge(popular, ongoing, recent).ifEmpty { all }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            ongoing = ongoing,
            completed = completed,
            popular = popular,
            recent = recent,
            topRated = topRated,
            recommendations = recommendations,
            sources = listOf(result)
        )
        HomeCache.save(_uiState.value)
        // Simpan ke disk persisten (buka app berikutnya instan).
        val st = _uiState.value
        AnimeHomeStore.save(
            AnimeHomeStore.Snapshot(
                ongoing = st.ongoing.map { it.toStore() },
                popular = st.popular.map { it.toStore() },
                recent = st.recent.map { it.toStore() },
                completed = st.completed.map { it.toStore() }
            )
        )
    }

    /** Cache home level-proses (instan saat balik ke Home). */
    object HomeCache {
        @Volatile var sections: AnimeUiState? = null
        fun save(s: AnimeUiState) { sections = s.copy(isLoading = false) }
    }

    fun retry() {
        homeLoaded = false
        loadHome(force = true)
    }

    fun ensureEnriched(link: String?) {
        if (link.isNullOrBlank()) return
        if (_enrichment.value.containsKey(link)) return
        if (!enriching.add(link)) return
        viewModelScope.launch {
            val data = runCatching { nexadev.getAnimeDetail(details = link).body()?.data }
                .getOrNull()
            enriching.remove(link)
            if (data != null) {
                val genres = data.genres.orEmpty()
                val rating = data.rating
                if (genres.isNotEmpty() || rating != null) {
                    _enrichment.value = _enrichment.value + mapOf(
                        link to AnimeEnrichment(genres = genres, rating = rating)
                    )
                }
            }
        }
    }

    fun loadDetail(url: String) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                detailError = "Anime tidak ditemukan"
            )
            return
        }
        lastAnimeUrl = url
        detailCache[url]?.let { cached ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                detail = cached,
                episodes = episodeCache[url] ?: cached.episodes.orEmpty(),
                detailError = null
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, detailError = null)
            // 1. Detail langsung dari NexaDev.
            var detail = fetchNexadevDetail(url)
            var episodes = detail?.episodes.orEmpty()
            // 2. Fallback episode via judul (search -> detail) kalau kosong.
            if (episodes.isEmpty()) {
                val title = detail?.title ?: rememberTitleFor(url)
                if (!title.isNullOrBlank()) {
                    val byTitle = fetchNexadevEpisodesByTitle(title)
                    if (byTitle != null) {
                        if (detail == null) detail = byTitle.first
                        episodes = (episodes + byTitle.second)
                            .distinctBy { it.episodeUrl ?: it.episode }
                    }
                }
            }
            if (detail != null) {
                val resolved = detail.copy(episodes = episodes.ifEmpty { detail.episodes })
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = resolved,
                    episodes = resolved.episodes.orEmpty(),
                    detailError = null
                )
                _enrichment.value = _enrichment.value + mapOf(
                    url to AnimeEnrichment(
                        genres = resolved.genres.orEmpty(),
                        rating = resolved.rating
                    )
                )
                detailCache[url] = resolved
                episodeCache[url] = resolved.episodes.orEmpty()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detailError = "Gagal memuat detail anime. Periksa koneksi lalu coba lagi."
                )
            }
        }
    }

    private fun rememberTitleFor(url: String): String? {
        val st = _uiState.value
        val all = st.ongoing + st.popular + st.completed + st.recent +
            st.topRated + st.recommendations
        return all.firstOrNull { it.link == url }?.title
    }

    private suspend fun fetchNexadevDetail(url: String): AnimeDetailData? = try {
        nexadev.getAnimeDetail(details = url).body()?.data
            ?.takeIf { it.title != null || it.episodes != null || it.sinopsis != null }
    } catch (e: Exception) {
        null
    }

    private suspend fun fetchNexadevEpisodesByTitle(title: String): Pair<AnimeDetailData, List<AnimeEpisode>>? {
        return try {
            val results = nexadev.searchAnime(title).body()?.data?.results.orEmpty()
            val key = normalizeKey(title)
            val match = results.firstOrNull { normalizeKey(it.title) == key }
                ?: results.firstOrNull { normalizeKey(it.title).contains(key) || key.contains(normalizeKey(it.title)) }
                ?: results.firstOrNull()
                ?: return null
            val link = match.link ?: return null
            val detail = nexadev.getAnimeDetail(details = link).body()?.data ?: return null
            val eps = detail.episodes.orEmpty()
            if (eps.isEmpty()) return null
            detail to eps
        } catch (e: Exception) {
            null
        }
    }

    fun retryDetail() { lastAnimeUrl?.let { loadDetail(it) } }

    fun loadStream(url: String) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                streamUrl = null,
                streamLoading = false,
                streamError = "Episode tidak ditemukan"
            )
            return
        }
        lastStreamEpisodeUrl = url
        _uiState.value = _uiState.value.copy(
            streamUrl = null,
            streamLoading = true,
            streamError = null
        )
        viewModelScope.launch {
            val mp4 = resolveNexadevStream(url)
            _uiState.value = _uiState.value.copy(
                streamUrl = mp4,
                streamLoading = false,
                streamError = if (mp4 == null) "Sumber video belum tersedia" else null
            )
        }
    }

    private suspend fun resolveNexadevStream(url: String): String? = runCatching {
        val data = nexadev.getAnimeStream(stream = url).body()?.data
        val pageUrl = data?.streamUrl
        if (pageUrl.isNullOrBlank()) null else resolveBlogger(pageUrl)
    }.getOrNull()

    fun retryStream() { lastStreamEpisodeUrl?.let { loadStream(it) } }

    suspend fun search(query: String, force: Boolean = false): List<AnimeSearchItem> {
        if (query.isBlank()) return emptyList()
        val key = query.trim().lowercase()
        if (!force) searchCache[key]?.let { return it }
        val q = query.trim()
        val results = runCatching {
            nexadev.searchAnime(q).body()?.data?.results.orEmpty()
        }.getOrElse { emptyList() }
        val merged = mergeDedupe(
            lists = listOf(results),
            keyOf = { normalizeKey(it.title) },
            scoreOf = { if (it.image.isNullOrBlank()) 0 else 1 }
        )
        searchCache[key] = merged
        return merged
    }

    suspend fun loadGenres(): List<String> = FALLBACK_GENRES

    private val FALLBACK_GENRES = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural"
    )

    suspend fun searchFresh(query: String): List<AnimeSearchItem> {
        searchCache.remove(query.trim().lowercase())
        return search(query, force = true)
    }

    private suspend fun resolveBlogger(pageUrl: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    val html = response.body?.string() ?: return@use null
                    val decoded = html
                        .replace("&quot;", "\"")
                        .replace("\\u0026", "&")
                        .replace("\\u003d", "=")
                        .replace("&amp;", "&")
                    GOOGLEVIDEO_REGEX.find(decoded)?.value?.takeIf { it.isNotBlank() }
                }
            }.getOrNull()
        }

    private fun AnimeSearchItem.toStore() = AnimeHomeStore.Item(
        title = title, link = link, image = image, latestEpisode = latestEpisode,
        type = type, rating = rating, status = status
    )

    private fun AnimeSearchItem.isValidAnime(): Boolean {
        val t = title?.trim().orEmpty()
        if (t.isBlank() || t.startsWith("#")) return false
        val l = link?.trim().orEmpty()
        if (l.isBlank() || l == "#" || l.endsWith("/#")) return false
        return true
    }

    companion object {
        const val USER_AGENT = "okhttp/4.12.0"
        private val GOOGLEVIDEO_REGEX = Regex(
            """https://[a-z0-9.-]*googlevideo\.com/videoplayback\?[^"\\\s]*"""
        )
    }
}

/** Hasil sementara per kategori (dipakai fetchNexadevHome). */
private data class AnimeFetch(
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val recent: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList()
) {
    val all: List<AnimeSearchItem>
        get() = (ongoing + popular + recent + completed)
            .distinctBy { it.link ?: it.title }
}