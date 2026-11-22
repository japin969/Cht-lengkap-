package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.NexadevApi
import kotlinx.coroutines.Dispatchers
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
    val detailError: String? = null
)

class AnimeViewModel : ViewModel() {

    private val api = NexadevApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState.asStateFlow()

    /** Genre & rating per link anime, diisi bertahap dari endpoint detail. */
    private val _enrichment = MutableStateFlow<Map<String, AnimeEnrichment>>(emptyMap())
    val enrichment: StateFlow<Map<String, AnimeEnrichment>> = _enrichment.asStateFlow()

    private val enriching = mutableSetOf<String>()
    private var lastAnimeUrl: String? = null
    private var lastStreamEpisodeUrl: String? = null

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            try {
                val popular = runCatching {
                    api.getAnimePopular().body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())
                val ongoing = runCatching {
                    api.getAnimeOngoing().body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())
                val completed = runCatching {
                    api.getAnimeCompleted().body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())
                val recent = runCatching {
                    api.getAnimeRecent().body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())

                val hasData = popular.isNotEmpty() || ongoing.isNotEmpty() ||
                    completed.isNotEmpty() || recent.isNotEmpty()

                val msg = if (hasData) null else "Gagal memuat daftar anime"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    popular = popular,
                    ongoing = ongoing,
                    completed = completed,
                    recent = recent,
                    error = msg,
                    homeError = msg
                )
            } catch (e: Exception) {
                val msg = e.javaClass.simpleName + ": " +
                    (e.localizedMessage ?: "Koneksi bermasalah")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = msg,
                    homeError = msg
                )
            }
        }
    }

    fun retry() = loadHome()

    /**
     * Ambil genre + rating sebuah anime dari endpoint detail, lalu simpan di cache.
     * Aman dipanggil berkali-kali: hanya request pertama yang benar-benar jalan.
     */
    fun ensureEnriched(link: String?) {
        if (link.isNullOrBlank()) return
        if (_enrichment.value.containsKey(link)) return
        if (!enriching.add(link)) return
        viewModelScope.launch {
            val result = runCatching { api.getAnimeDetail(details = link).body()?.data }
                .getOrNull()
            enriching.remove(link)
            if (result != null) {
                _enrichment.value = _enrichment.value + mapOf(
                    link to AnimeEnrichment(
                        genres = result.genres.orEmpty(),
                        rating = result.rating
                    )
                )
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
            try {
                val data = api.getAnimeDetail(details = url).body()?.data
                if (data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = data,
                        episodes = data.episodes.orEmpty(),
                        detailError = null
                    )
                    // Simpan genre & rating ke cache agar kartu terkait ikut terbarui.
                    _enrichment.value = _enrichment.value + mapOf(
                        url to AnimeEnrichment(
                            genres = data.genres.orEmpty(),
                            rating = data.rating
                        )
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detailError = "Gagal memuat detail anime"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detailError = e.localizedMessage ?: "Gagal memuat detail anime"
                )
            }
        }
    }

    fun retryDetail() {
        lastAnimeUrl?.let { loadDetail(it) }
    }

    /**
     * Ambil link streaming lalu ubah ke URL mp4 yang bisa diputar ExoPlayer.
     *
     * PENTING: googlevideo menolak User-Agent selain okhttp, jadi URL hasil
     * resolve harus diputar dengan UA okhttp (lihat AnimePlayerScreen).
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
            try {
                val data = api.getAnimeStream(stream = url).body()?.data
                val pageUrl = data?.streamUrl
                val mp4 = if (pageUrl.isNullOrBlank()) null else resolvePlayableUrl(pageUrl)
                _uiState.value = _uiState.value.copy(
                    streamUrl = mp4,
                    streamLoading = false,
                    streamError = if (mp4 == null) "Sumber video belum tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    streamUrl = null,
                    streamLoading = false,
                    streamError = e.localizedMessage ?: "Gagal memuat video"
                )
            }
        }
    }

    fun retryStream() {
        lastStreamEpisodeUrl?.let { loadStream(it) }
    }

    suspend fun search(query: String): List<AnimeSearchItem> {
        if (query.isBlank()) return emptyList()
        return runCatching {
            api.searchAnime(query.trim()).body()?.data?.results.orEmpty()
        }.getOrElse { emptyList() }
    }

    /**
     * Ubah halaman blogger menjadi URL mp4 langsung.
     * Mengembalikan null bila tidak ada URL video yang bisa diputar.
     */
    private suspend fun resolvePlayableUrl(pageUrl: String): String? =
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

    companion object {
        /** googlevideo hanya menerima User-Agent okhttp. */
        const val USER_AGENT = "okhttp/4.12.0"

        /** Regex di PCRE: cocokkan URL googlevideo tanpa escaping berlebih. */
        private val GOOGLEVIDEO_REGEX = Regex(
            """https://rr\d+---[a-z0-9-]+\.googlevideo\.com/videoplayback\?[^"\\\s]*"""
        )
    }
}