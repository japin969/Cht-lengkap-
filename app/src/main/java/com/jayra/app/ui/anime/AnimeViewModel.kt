package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.AnimeStreamData
import com.jayra.app.data.NexadevApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AnimeUiState(
    val isLoading: Boolean = false,
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    val recent: List<AnimeSearchItem> = emptyList(),
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    /** Menandakan episode sedang dimuat. */
    val episodesLoading: Boolean = false,
    /** URL mp4 siap putar (hasil resolusi stream). */
    val streamUrl: String? = null,
    val streamLoading: Boolean = false,
    val streamError: String? = null,
    /** Error umum (home / detail). */
    val error: String? = null,
    val homeError: String? = null,
    val detailError: String? = null
)

class AnimeViewModel : ViewModel() {
    // Sumber data: Nexadev API (https://api.nexadev.my.id) — client & base URL
    // yang sudah ada di project, sama persis dengan fitur lain.
    private val api = NexadevApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    private var lastAnimeUrl: String? = null

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            try {
                val ongoing = runCatching {
                    api.getAnimeOngoing().body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())
                val popular = runCatching {
                    api.getAnimePopular().body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())
                val completed = runCatching {
                    api.getAnimeCompleted().body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())
                val recent = runCatching {
                    api.getAnimeRecent().body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())

                val hasData = ongoing.isNotEmpty() || popular.isNotEmpty() ||
                    completed.isNotEmpty() || recent.isNotEmpty()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    ongoing = ongoing,
                    popular = popular,
                    completed = completed,
                    recent = recent,
                    homeError = if (hasData) null else "Gagal memuat daftar anime"
                )
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Koneksi bermasalah"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = msg,
                    homeError = msg
                )
            }
        }
    }

    fun retry() = loadHome()

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
                        episodesLoading = false,
                        detailError = null
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

    /** Muat ulang daftar episode saja. */
    fun retryEpisodes() {
        lastAnimeUrl?.let { loadDetail(it) }
    }

    /**
     * Ambil link streaming episode lalu ubah ke URL mp4 yang bisa diputar ExoPlayer.
     *
     * Nexadev mengembalikan `streamUrl` berupa halaman Blogger; di dalamnya ada
     * URL googlevideo (video/mp4). Bila tidak ditemukan, `streamUrl` dibiarkan
     * null dan UI menampilkan pesan — bukan crash.
     */
    fun loadStream(episodeUrl: String) {
        if (episodeUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(
                streamLoading = false,
                streamError = "Episode tidak ditemukan"
            )
            return
        }
        lastStreamEpisodeUrl = episodeUrl
        _uiState.value = _uiState.value.copy(
            streamLoading = true,
            streamError = null,
            streamUrl = null
        )
        viewModelScope.launch {
            try {
                val data = api.getAnimeStream(stream = episodeUrl).body()?.data
                val pageUrl = data?.streamUrl
                if (pageUrl.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        streamLoading = false,
                        streamError = "Sumber video belum tersedia"
                    )
                    return@launch
                }
                val mp4 = resolvePlayableUrl(pageUrl)
                _uiState.value = _uiState.value.copy(
                    streamLoading = false,
                    streamUrl = mp4,
                    streamError = if (mp4 == null) "Sumber video belum tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    streamLoading = false,
                    streamError = e.localizedMessage ?: "Gagal memuat video"
                )
            }
        }
    }

    fun retryStream() {
        lastStreamEpisodeUrl?.let { loadStream(it) }
    }

    private var lastStreamEpisodeUrl: String? = null

    /** Pencarian anime. Return hasil agar screen bisa menampilkan state sendiri. */
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
    private suspend fun resolvePlayableUrl(pageUrl: String): String? = withContext(Dispatchers.IO) {
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
                // Unescape HTML entities yang umum dipakai di halaman blogger.
                val decoded = html
                    .replace("&quot;", "\"")
                    .replace("\\u0026", "&")
                    .replace("\\u003d", "=")
                    .replace("&amp;", "&")
                val match = GOOGLEVIDEO_REGEX.find(decoded)?.value ?: return@use null
                if (match.isBlank()) null else match
            }
        }.getOrNull()
    }

    companion object {
        private const val USER_AGENT = "okhttp/4.12.0"
        private val GOOGLEVIDEO_REGEX = Regex(
            """https://rr\d+---[a-z0-9-]+\.googlevideo\.com/videoplayback\?[^"\\\s]*"""
        )
    }
}