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

/**
 * Hasil satu sumber, dikelompokkan per kategori (tidak digabung) supaya
 * Home bisa menampilkan section yang berbeda-beda dan tetap terisi.
 */
data class AnimeFetch(
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val recent: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList()
) {
    val all: List<AnimeSearchItem>
        get() = (ongoing + popular + recent + completed)
            .distinctBy { it.link ?: it.title }
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
    /** Cache detail agar buka lagi tidak loading ulang dari nol. */
    private val detailCache = LinkedHashMap<String, AnimeDetailData>()
    private val episodeCache = LinkedHashMap<String, List<AnimeEpisode>>()
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
            // Genre (untuk section Genre) — best effort, tidak memblokir.
            val genres = runCatching { loadGenres() }.getOrDefault(emptyList())
            if (genres.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(genres = genres)
            }
        }
    }

    private suspend fun fetchNexadevHome(): AnimeSourceResult {
        return try {
            // Ambil tiap kategori terpisah; kegagalan satu kategori tidak
            // menggagalkan yang lain.
            val ongoing = runCatching {
                nexadev.getAnimeOngoing().body()?.data?.animeList.orEmpty()
            }.getOrDefault(emptyList())
            val popular = runCatching {
                nexadev.getAnimePopular().body()?.data?.animeList.orEmpty()
            }.getOrDefault(emptyList())
            val completed = runCatching {
                nexadev.getAnimeCompleted().body()?.data?.animeList.orEmpty()
            }.getOrDefault(emptyList())
            val recent = runCatching {
                nexadev.getAnimeRecent().body()?.data?.animeList.orEmpty()
            }.getOrDefault(emptyList())
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
        } catch (e: Exception) {
            AnimeSourceResult("nexadev", emptyList(), e.localizedMessage ?: "gagal")
        }
    }

    /** Cache kategori Nexadev terakhir (dipakai mergeHome). */
    private var _nexadevCache: AnimeFetch = AnimeFetch()

    private suspend fun fetchSankavollereiHome(): AnimeSourceResult {
        return try {
            // Ambil SEMUA sumber anime Sankavollerei secara PARALEL,
            // dikelompokkan per kategori agar section-nya berbeda.
            data class Grouped(
                val ongoing: List<AnimeSearchItem>,
                val popular: List<AnimeSearchItem>,
                val recent: List<AnimeSearchItem>,
                val completed: List<AnimeSearchItem>
            )
            val grouped = coroutineScope {
                val home = async {
                    val d = runCatching { sankavollerei.animeHome().body()?.data }.getOrNull()
                    Grouped(
                        ongoing = d?.ongoing?.animeList?.map { it.toItem() }.orEmpty(),
                        popular = emptyList(),
                        recent = emptyList(),
                        completed = d?.completed?.animeList?.map { it.toItem() }.orEmpty()
                    )
                }
                val samehadaku = async {
                    // samehadaku home punya recent/popular/ongoing/completed.
                    val d = runCatching { sankavollerei.samehadakuHome().body()?.data }.getOrNull()
                    Grouped(
                        ongoing = d?.ongoing?.animeList?.map { it.toItem("Ongoing") }.orEmpty(),
                        popular = d?.popular?.animeList?.map { it.toItem("Ongoing") }.orEmpty(),
                        recent = d?.recent?.animeList?.map { it.toItem("Ongoing") }.orEmpty(),
                        completed = d?.completed?.animeList?.map { it.toItem("Completed") }.orEmpty()
                    )
                }
                val oploverz = async {
                    Grouped(
                        ongoing = runCatching {
                            sankavollerei.oploverzOngoing().body()?.allItems
                                ?.map { it.toItem() }.orEmpty()
                        }.getOrDefault(emptyList()),
                        popular = emptyList(),
                        recent = emptyList(),
                        completed = runCatching {
                            sankavollerei.oploverzCompleted().body()?.allItems
                                ?.map { it.toItem() }.orEmpty()
                        }.getOrDefault(emptyList())
                    )
                }
                val nimegami = async {
                    Grouped(
                        ongoing = emptyList(),
                        popular = emptyList(),
                        recent = runCatching {
                            sankavollerei.nimegamiHome().body()?.allItems
                                ?.map { it.toItem() }.orEmpty()
                        }.getOrDefault(emptyList()),
                        completed = emptyList()
                    )
                }
                listOf(home.await(), samehadaku.await(), oploverz.await(), nimegami.await())
            }
            val fetch = AnimeFetch(
                ongoing = grouped.flatMap { it.ongoing },
                popular = grouped.flatMap { it.popular },
                recent = grouped.flatMap { it.recent },
                completed = grouped.flatMap { it.completed }
            )
            _sankaCache = fetch
            AnimeSourceResult(
                source = "sankavollerei",
                items = fetch.all,
                error = if (fetch.all.isEmpty()) "kosong" else null
            )
        } catch (e: Exception) {
            AnimeSourceResult("sankavollerei", emptyList(), e.localizedMessage ?: "gagal")
        }
    }

    private var _sankaCache: AnimeFetch = AnimeFetch()

    private fun mergeHome(results: List<AnimeSourceResult>) {
        val nexadev = _nexadevCache
        val sanka = _sankaCache

        // Gabungkan tiap kategori dari kedua sumber lalu dedup longgar.
        // Item dengan image terisi diprioritaskan.
        fun merge(vararg lists: List<AnimeSearchItem>): List<AnimeSearchItem> =
            mergeDedupe(
                lists = lists.toList(),
                keyOf = { normalizeKey(it.title) },
                scoreOf = { if (it.image.isNullOrBlank()) 0 else 1 }
            )

        val ongoing = merge(nexadev.ongoing, sanka.ongoing)
        val popular = merge(nexadev.popular, sanka.popular).ifEmpty { ongoing }
        val recent = merge(nexadev.recent, sanka.recent).ifEmpty { ongoing }
        val completed = merge(nexadev.completed, sanka.completed)
        val all = merge(nexadev.all, sanka.all)

        // Top Rated: yang punya rating, diurutkan tertinggi ke terendah.
        val topRated = all
            .filter { it.rating != null && it.rating!!.toDoubleOrNull() != null }
            .sortedByDescending { it.rating!!.toDoubleOrNull() ?: 0.0 }
            .ifEmpty { all }

        // Rekomendasi: gabungan populer + ongoing (diambil dari yang belum jadi hero).
        val recommendations = merge(popular, ongoing, recent).ifEmpty { all }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            ongoing = ongoing,
            completed = completed,
            popular = popular,
            recent = recent,
            topRated = topRated,
            recommendations = recommendations,
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
        // Buka lagi -> langsung tampil dari cache tanpa loading putih.
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
            // Semua sumber dicoba PARALEL. Tidak ada sumber yang diprioritaskan
            // secara eksklusif — begitu satu sumber mengembalikan detail valid,
            // itu dipakai; sumber lain tetap dilengkapi (genre/episode).
            // Kegagalan satu sumber TIDAK menggagalkan keseluruhan.
            val data: List<AnimeDetailData> = coroutineScope {
                val a = async { fetchNexadevDetail(url) }
                val b = async { fetchSankaAnimeDetail(url) }
                val c = async { fetchSamehadakuDetail(url) }
                val d = async { fetchOploverzDetail(url) }
                val e = async { fetchNimegamiDetail(url) }
                listOfNotNull(a.await(), b.await(), c.await(), d.await(), e.await())
                    .filter { it.title != null || it.episodes != null || it.sinopsis != null }
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
                detailCache[url] = _uiState.value.detail ?: first
                episodeCache[url] = mergedEpisodes
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detailError = "Gagal memuat detail anime"
                )
            }
        }
    }

    /** Detail Nexadev (animasu): menerima URL penuh animasu.love. */
    private suspend fun fetchNexadevDetail(url: String): AnimeDetailData? = try {
        nexadev.getAnimeDetail(details = url).body()?.data
            ?.takeIf { it.title != null || it.episodes != null || it.sinopsis != null }
    } catch (e: Exception) {
        null
    }

    /** Detail Sankavollerei (otakudesu): animeId diekstrak dari href. */
    private suspend fun fetchSankaAnimeDetail(url: String): AnimeDetailData? {
        val id = extractSlug(url, "/anime/anime/") ?: return null
        return try {
            sankavollerei.animeDetail(animeId = id).body()?.data?.asAnimeDetail()
                ?.takeIf { it.title != null || it.episodes != null || it.sinopsis != null }
        } catch (e: Exception) {
            null
        }
    }

    /** Detail samehadaku: slug diambil dari href. */
    private suspend fun fetchSamehadakuDetail(url: String): AnimeDetailData? {
        val slug = extractSlug(url, "samehadaku/anime/") ?: return null
        return try {
            sankavollerei.samehadakuDetail(slug = slug).body()?.data?.asAnimeDetail()
                ?.takeIf { it.title != null || it.episodes != null || it.sinopsis != null }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchOploverzDetail(url: String): AnimeDetailData? {
        val slug = extractSlug(url, "oploverz/anime/") ?: return null
        return try {
            sankavollerei.oploverzDetail(slug = slug).body()?.data?.asAnimeDetail()
                ?.takeIf { it.title != null || it.episodes != null || it.sinopsis != null }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchNimegamiDetail(url: String): AnimeDetailData? {
        val slug = extractSlug(url, "nimegami/detail/") ?: return null
        return try {
            val resp = sankavollerei.nimegamiDetail(slug = slug).body()
            resp?.detail?.asAnimeDetail()
                ?.takeIf { it.title != null || it.episodes != null || it.sinopsis != null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Ekstrak slug/id dari URL/link yang formatnya bisa berbeda antar sumber.
     * Mendukung URL penuh (https://animasu.love/anime/xxx/) maupun path relatif
     * (/anime/anime/xxx, /samehadaku/anime/xxx, dst).
     * Mengembalikan segmen terakhir yang bersih (tanpa query/slash).
     */
    private fun extractSlug(url: String, marker: String): String? {
        val afterMarker = if (url.contains(marker)) url.substringAfter(marker)
            else url.substringAfterLast("/")
        val cleaned = afterMarker
            .substringBefore("?")
            .substringBefore("#")
            .trim('/')
        return cleaned.ifBlank { null }
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
                episode = ep.episodeId?.substringAfterLast("-") ?: ep.plainTitle,
                title = ep.plainTitle,
                episodeUrl = ep.href ?: ep.episodeId
            )
        }
    )

    private fun SankavollereiOploverzDetail.asAnimeDetail(): AnimeDetailData = AnimeDetailData(
        title = title,
        image = poster,
        sinopsis = plainSynopsis,
        rating = ratingValue,
        totalEpisodes = episodeCount,
        genres = genreNames,
        info = buildMap {
            statusText?.let { put("Status", it) }
            type?.let { put("Tipe", it) }
        },
        episodes = episode_list.orEmpty().map { ep ->
            AnimeEpisode(
                episode = ep.episode,
                title = ep.title,
                episodeUrl = ep.url ?: ep.slug?.let { "/anime/oploverz/episode/$it" }
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
        val merged = mergeDedupe(
            lists = results,
            keyOf = { normalizeKey(it.title) },
            scoreOf = { if (it.image.isNullOrBlank()) 0 else 1 }
        )
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