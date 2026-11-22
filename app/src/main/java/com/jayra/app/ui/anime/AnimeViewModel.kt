package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.NexadevApi
import com.jayra.app.data.SankavollereiAnimeItem
import com.jayra.app.data.SankavollereiApi
import com.jayra.app.data.SankavollereiAnimeHomeData
import com.jayra.app.data.SankavollereiOploverzItem
import com.jayra.app.data.SankavollereiOploverzDetail
import com.jayra.app.data.SankavollereiNimegamiItem
import com.jayra.app.data.SankavollereiNimegamiDetail
import com.jayra.app.data.SankavollereiSamehadakuDetail
import com.jayra.app.data.SankavollereiGenreItem
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
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val streamUrl: String? = null,
    val streamLoading: Boolean = false,
    val streamError: String? = null,
    val error: String? = null,
    val homeError: String? = null,
    val detailError: String? = null,
    /** Status per sumber, dipakai untuk banner "sebagian sumber gagal". */
    val sources: List<AnimeSourceResult> = emptyList()
)

class AnimeViewModel : ViewModel() {

    // Sumber 1 (lama) — tetap dipakai.
    private val nexadev = NexadevApi.create()
    // Sumber 2 (baru) — ditambahkan di sampingnya.
    private val sankavollerei = SankavollereiApi.create()

    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState.asStateFlow()

    private val _enrichment = MutableStateFlow<Map<String, AnimeEnrichment>>(emptyMap())
    val enrichment: StateFlow<Map<String, AnimeEnrichment>> = _enrichment.asStateFlow()

    /** Cache hasil search agar tidak nembak API berulang untuk query yang sama. */
    private val searchCache = LinkedHashMap<String, List<AnimeSearchItem>>()
    private val enriching = mutableSetOf<String>()
    private var lastAnimeUrl: String? = null
    private var lastStreamEpisodeUrl: String? = null
    private var homeLoaded = false

    init { loadHome() }

    fun loadHome(force: Boolean = false) {
        if (homeLoaded && !force) return
        homeLoaded = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            // Kedua sumber dipanggil PARALEL. Kegagalan satu tidak
            // menggagalkan yang lain.
            val results = coroutineScope {
                val a = async { fetchNexadevHome() }
                val b = async { fetchSankavollereiHome() }
                listOf(a.await(), b.await())
            }
            mergeHome(results)
        }
    }

    private suspend fun fetchNexadevHome(): AnimeSourceResult {
        return try {
            val ongoing = nexadev.getAnimeOngoing().body()?.data?.animeList.orEmpty()
            val popular = nexadev.getAnimePopular().body()?.data?.animeList.orEmpty()
            val completed = nexadev.getAnimeCompleted().body()?.data?.animeList.orEmpty()
            AnimeSourceResult(
                source = "nexadev",
                items = (ongoing + popular + completed).distinctBy { it.link ?: it.title },
                error = if (ongoing + popular + completed == emptyList<AnimeSearchItem>())
                    "kosong" else null
            )
        } catch (e: Exception) {
            AnimeSourceResult("nexadev", emptyList(), e.localizedMessage ?: "gagal")
        }
    }

    private suspend fun fetchSankavollereiHome(): AnimeSourceResult {
        return try {
            // Ambil SEMUA sumber anime Sankavollerei secara PARALEL.
            val sources: List<List<AnimeSearchItem>> = coroutineScope {
                val a = async {
                    fetchHomeFromSankavollerei()
                }
                val b = async {
                    fetchHomeFromSamehadaku()
                }
                val c = async {
                    fetchHomeFromOploverz()
                }
                val d = async {
                    fetchHomeFromNimegami()
                }
                listOf(a.await(), b.await(), c.await(), d.await())
            }
            val merged = sources.flatten().distinctBy { it.link ?: it.title }
            AnimeSourceResult(
                source = "sankavollerei",
                items = merged,
                error = if (merged.isEmpty()) "kosong" else null
            )
        } catch (e: Exception) {
            AnimeSourceResult("sankavollerei", emptyList(), e.localizedMessage ?: "gagal")
        }
    }

    private suspend fun fetchHomeFromSankavollerei(): List<AnimeSearchItem> {
        val out: MutableList<AnimeSearchItem> = mutableListOf()
        val data: SankavollereiAnimeHomeData? =
            runCatching { sankavollerei.animeHome().body()?.data }.getOrNull()
        if (data != null) {
            data.ongoing?.animeList?.forEach { item: SankavollereiAnimeItem ->
                out.add(item.toItem())
            }
            data.completed?.animeList?.forEach { item: SankavollereiAnimeItem ->
                out.add(item.toItem())
            }
        }
        return out
    }

    private suspend fun fetchHomeFromSamehadaku(): List<AnimeSearchItem> {
        return try {
            val items: List<SankavollereiAnimeItem>? =
                sankavollerei.samehadakuHome().body()?.allItems
            items?.map { item: SankavollereiAnimeItem -> item.toItem("Ongoing") } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchHomeFromOploverz(): List<AnimeSearchItem> {
        return try {
            val items: List<SankavollereiOploverzItem>? =
                sankavollerei.oploverzHome().body()?.allItems
            items?.map { item: SankavollereiOploverzItem -> item.toItem() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchHomeFromNimegami(): List<AnimeSearchItem> {
        return try {
            val items: List<SankavollereiNimegamiItem>? =
                sankavollerei.nimegamiHome().body()?.allItems
            items?.map { item: SankavollereiNimegamiItem -> item.toItem() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mergeHome(results: List<AnimeSourceResult>) {
        val all = results.flatMap { it.items }
            .distinctBy { it.link ?: it.title }

        // Ongoing / completed dipisah berdasarkan sumber asalnya, tapi
        // digabung antar sumber tanpa duplikat.
        val ongoing = buildList {
            addAll(results.firstOrNull { it.source == "nexadev" }
                ?.items?.filter { it.status.equals("Ongoing", true) }.orEmpty())
            addAll(results.firstOrNull { it.source == "sankavollerei" }
                ?.items?.filter { it.status.equals("Ongoing", true) }.orEmpty())
        }.distinctBy { it.link ?: it.title }

        val completed = buildList {
            addAll(results.firstOrNull { it.source == "nexadev" }
                ?.items?.filter { !it.status.equals("Ongoing", true) }.orEmpty())
            addAll(results.firstOrNull { it.source == "sankavollerei" }
                ?.items?.filter { !it.status.equals("Ongoing", true) }.orEmpty())
        }.distinctBy { it.link ?: it.title }

        val popular = all
        val recent = all

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            ongoing = ongoing.ifEmpty { all },
            completed = completed,
            popular = popular,
            recent = recent,
            sources = results
        )
    }

    fun retry() {
        homeLoaded = false
        loadHome(force = true)
    }

    /**
     * Ambil genre + rating sebuah anime dari detail (dua sumber, paralel).
     * Hasil di-cache supaya tidak request berulang.
     */
    fun ensureEnriched(link: String?) {
        if (link.isNullOrBlank()) return
        if (_enrichment.value.containsKey(link)) return
        if (!enriching.add(link)) return
        viewModelScope.launch {
            val result = coroutineScope {
                val a = async { runCatching { nexadev.getAnimeDetail(details = link).body()?.data }
                    .getOrNull()?.let { it.genres.orEmpty() to it.rating } }
                val b = async {
                    runCatching {
                        sankavollerei.animeDetail(animeId = link.removePrefix("/anime/anime/"))
                            .body()?.data
                    }.getOrNull()?.let { it.genres.orEmpty() to it.score?.toDoubleOrNull() }
                }
                listOfNotNull(a.await(), b.await())
            }
            enriching.remove(link)
            if (result.isNotEmpty()) {
                val genres = result.flatMap { it.first }.distinct()
                val rating = result.mapNotNull { it.second }.firstOrNull()
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, detailError = null)
            val data: List<AnimeDetailData> = coroutineScope {
                val a = async {
                    try {
                        nexadev.getAnimeDetail(details = url).body()?.data
                    } catch (e: Exception) {
                        null
                    }
                }
                val b = async {
                    try {
                        sankavollerei.animeDetail(animeId = url.removePrefix("/anime/anime/"))
                            .body()?.data?.asAnimeDetail()
                    } catch (e: Exception) {
                        null
                    }
                }
                val c = async { fetchSamehadakuDetail(url) }
                val d = async { fetchOploverzDetail(url) }
                val e = async { fetchNimegamiDetail(url) }
                listOfNotNull(a.await(), b.await(), c.await(), d.await(), e.await())
            }
            if (data.isNotEmpty()) {
                val first = data.first()
                val mergedGenres = data.flatMap { it.genres.orEmpty() }.distinct()
                val mergedEpisodes = data.flatMap { it.episodes.orEmpty() }
                    .distinctBy { it.episodeUrl ?: it.episode }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = first.copy(
                        genres = mergedGenres.ifEmpty { first.genres },
                        episodes = mergedEpisodes
                    ),
                    episodes = mergedEpisodes,
                    detailError = null
                )
                _enrichment.value = _enrichment.value + mapOf(
                    url to AnimeEnrichment(
                        genres = mergedGenres,
                        rating = first.rating
                    )
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detailError = "Gagal memuat detail anime"
                )
            }
        }
    }

    /** Detail samehadaku: slug diambil dari href. */
    private suspend fun fetchSamehadakuDetail(url: String): AnimeDetailData? {
        val slug = url.substringAfter("/anime/").substringAfter("samehadaku/anime/")
            .ifBlank { url.substringAfterLast("/") }
        return try {
            sankavollerei.samehadakuDetail(slug = slug).body()?.data?.asAnimeDetail()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchOploverzDetail(url: String): AnimeDetailData? {
        val slug = url.substringAfter("oploverz/anime/").ifBlank { url.substringAfterLast("/") }
        return try {
            sankavollerei.oploverzDetail(slug = slug).body()?.data?.asAnimeDetail()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchNimegamiDetail(url: String): AnimeDetailData? {
        val slug = url.substringAfter("nimegami/detail/").ifBlank { url.substringAfterLast("/") }
        return try {
            val resp = sankavollerei.nimegamiDetail(slug = slug).body()
            resp?.detail?.asAnimeDetail()
        } catch (e: Exception) {
            null
        }
    }

    private fun SankavollereiSamehadakuDetail.asAnimeDetail(): AnimeDetailData = AnimeDetailData(
        title = title,
        image = poster,
        sinopsis = plainSynopsis,
        rating = null,
        totalEpisodes = episodeList?.size,
        genres = genreNames,
        info = buildMap { status?.let { put("Status", it) } },
        episodes = episodeList.orEmpty().map { ep ->
            AnimeEpisode(
                episode = ep.eps?.toString(),
                title = ep.title,
                episodeUrl = ep.href ?: ep.episodeId
            )
        }
    )

    private fun SankavollereiOploverzDetail.asAnimeDetail(): AnimeDetailData = AnimeDetailData(
        title = title,
        image = poster,
        sinopsis = synopsis,
        rating = rating?.toDoubleOrNull(),
        totalEpisodes = episodes?.toIntOrNull(),
        genres = genres,
        info = buildMap {
            status?.let { put("Status", it) }
            type?.let { put("Tipe", it) }
        },
        episodes = episode_list.orEmpty().map { ep ->
            AnimeEpisode(
                episode = ep.episode,
                title = ep.title,
                episodeUrl = ep.slug?.let { "/anime/oploverz/episode/$it" }
            )
        }
    )

    private fun SankavollereiNimegamiDetail.asAnimeDetail(): AnimeDetailData = AnimeDetailData(
        title = title,
        image = poster,
        sinopsis = plainSynopsis,
        rating = null,
        totalEpisodes = episodeCount,
        genres = genreNames,
        info = info,
        episodes = emptyList()
    )

    fun retryDetail() {
        lastAnimeUrl?.let { loadDetail(it) }
    }

    /**
     * Ambil link streaming lalu ubah ke URL mp4 yang bisa diputar ExoPlayer.
     * Sumber Nexadev (Blogger→googlevideo) dan Sankavollerei (embed) dicoba
     * berurutan; yang pertama berhasil dipakai.
     */
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
            val mp4 = coroutineScope {
                val a = async { resolveNexadevStream(url) }
                val b = async { resolveSankavollereiStream(url) }
                a.await() ?: b.await()
            }
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

    private suspend fun resolveSankavollereiStream(url: String): String? = runCatching {
        val episodeId = url.substringAfterLast("/").trimEnd('/')
        val data = sankavollerei.animeEpisode(episodeId = episodeId).body()?.data
        val direct = data?.defaultStreamingUrl
        if (direct.isNullOrBlank()) null else resolveDesustream(direct)
    }.getOrNull()

    fun retryStream() {
        lastStreamEpisodeUrl?.let { loadStream(it) }
    }

    /** Pencarian PARALEL dari semua sumber, hasil digabung, duplikat dibuang. */
    suspend fun search(query: String, force: Boolean = false): List<AnimeSearchItem> {
        if (query.isBlank()) return emptyList()
        val key = query.trim().lowercase()
        if (!force) searchCache[key]?.let { return it }

        val q = query.trim()
        val results = coroutineScope {
            val a = async {
                runCatching {
                    nexadev.searchAnime(q).body()?.data?.results.orEmpty()
                }.getOrElse { emptyList() }
            }
            val b = async {
                runCatching {
                    sankavollerei.animeSearch(q).body()?.data?.animeList
                        .orEmpty().map { it.toItem() }
                }.getOrElse { emptyList() }
            }
            val c = async {
                runCatching {
                    sankavollerei.samehadakuSearch(q).body()?.allItems
                        .orEmpty().map { it.toItem("Ongoing") }
                }.getOrElse { emptyList() }
            }
            val d = async {
                runCatching {
                    sankavollerei.oploverzSearch(q).body()?.allItems
                        .orEmpty().map { it.toItem() }
                }.getOrElse { emptyList() }
            }
            val e = async {
                runCatching {
                    sankavollerei.nimegamiSearch(q).body()?.allItems
                        .orEmpty().map { it.toItem() }
                }.getOrElse { emptyList() }
            }
            listOf(a.await(), b.await(), c.await(), d.await(), e.await())
        }
        val merged = results.flatten().distinctBy { it.link ?: it.title }
        searchCache[key] = merged
        return merged
    }

    /** Genre list untuk filter (gabungan semua sumber). */
    suspend fun loadGenres(): List<String> {
        val fromSankaku = runCatching {
            sankavollerei.samehadakuGenres().body()?.all?.mapNotNull { it.label }.orEmpty()
        }.getOrElse { emptyList() }
        return (fromSankaku + FALLBACK_GENRES).distinct()
    }

    private val FALLBACK_GENRES = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural"
    )

    /** Versi tanpa cache (tombol coba lagi). */
    suspend fun searchFresh(query: String): List<AnimeSearchItem> {
        searchCache.remove(query.trim().lowercase())
        return search(query, force = true)
    }

    /** Ubah halaman blogger menjadi URL mp4 langsung. */
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

    /**
     * Halaman desustream adalah HTML; URL mp4 ada di dalamnya
     * (src / file / sources). Bila tidak ditemukan, kembalikan null.
     */
    private suspend fun resolveDesustream(pageUrl: String): String? =
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
                    if (!response.isSuccessful) return@use null
                    val html = response.body?.string() ?: return@use null
                    val decoded = html.replace("\\u0026", "&").replace("\\/", "/")
                    MP4_REGEX.find(decoded)?.value
                        ?: M3U8_REGEX.find(decoded)?.value
                }
            }.getOrNull()
        }

    private fun SankavollereiAnimeItem.toItem(): AnimeSearchItem = AnimeSearchItem(
        title = title,
        link = detailHref,
        image = poster,
        latestEpisode = episodes?.let { "$it eps" },
        type = null,
        rating = score,
        status = status
    )

    private fun SankavollereiAnimeItem.toItem(statusOverride: String): AnimeSearchItem =
        toItem().let { if (it.status.isNullOrBlank()) it.copy(status = statusOverride) else it }

    /** Oploverz memakai field `episode` (String) dan `oploverz_url`. */
    private fun SankavollereiOploverzItem.toItem(): AnimeSearchItem = AnimeSearchItem(
        title = title,
        link = slug?.let { "/anime/oploverz/anime/$it" },
        image = poster,
        latestEpisode = episode,
        type = type,
        rating = rating,
        status = status
    )

    /** Nimegami memakai `episode` sebagai label. */
    private fun SankavollereiNimegamiItem.toItem(): AnimeSearchItem = AnimeSearchItem(
        title = title,
        link = slug?.let { "/anime/nimegami/detail/$it" },
        image = poster,
        latestEpisode = episode,
        type = type,
        rating = null,
        status = status
    )

    /** Konversi detail Sankavollerei ke model yang dipakai UI. */
    private fun com.jayra.app.data.SankavollereiAnimeDetailData.asAnimeDetail(): AnimeDetailData =
        AnimeDetailData(
            title = title,
            image = poster,
            sinopsis = plainSynopsis,
            rating = score?.toDoubleOrNull(),
            totalEpisodes = episodes,
            genres = genres,
            info = buildMap {
                status?.let { put("Status", it) }
                studio?.let { put("Studio", it) }
                duration?.let { put("Durasi", it) }
                episodes?.let { put("Episode", "$it") }
            },
            episodes = episodeList.orEmpty().map { ep ->
                AnimeEpisode(
                    episode = ep.eps?.toString(),
                    title = ep.title,
                    episodeUrl = ep.href ?: ep.episodeId
                )
            }
        )

    companion object {
        /** googlevideo hanya menerima User-Agent okhttp. */
        const val USER_AGENT = "okhttp/4.12.0"

        private val GOOGLEVIDEO_REGEX = Regex(
            """https://rr\d+---[a-z0-9-]+\.googlevideo\.com/videoplayback\?[^"\\\s]*"""
        )
        private val MP4_REGEX = Regex("""https?://[^"'\s]+\.mp4[^"'\s]*""")
        private val M3U8_REGEX = Regex("""https?://[^"'\s]+\.m3u8[^"'\s]*""")
    }
}