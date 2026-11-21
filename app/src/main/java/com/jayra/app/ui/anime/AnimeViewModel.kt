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
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.withContext

data class AnimeUiState(
    val isLoading: Boolean = false,
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val streamUrl: String? = null,
    /** Menandakan proses resolve stream sedang berjalan. */
    val streamLoading: Boolean = false,
    /** Pesan bila stream tidak bisa diputar. */
    val streamError: String? = null,
    val error: String? = null
)

class AnimeViewModel : ViewModel() {
    // Sumber data: Nexadev API (https://api.nexadev.my.id) — client & base URL
    // yang sama persis dengan fitur Music yang sudah jalan di project ini.
    private val api = NexadevApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    private var lastAnimeUrl: String? = null

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val popularRes = api.getAnimePopular()
                val ongoingRes = api.getAnimeOngoing()
                val completedRes = api.getAnimeCompleted()

                val popular = popularRes.body()?.data?.animeList.orEmpty()
                val ongoing = ongoingRes.body()?.data?.animeList.orEmpty()
                val completed = completedRes.body()?.data?.animeList.orEmpty()

                val hasData = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()

                if (hasData) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        popular = popular,
                        ongoing = ongoing,
                        completed = completed,
                        error = null
                    )
                } else {
                    // Susun pesan yang menyebutkan sebab konkret, bukan generik.
                    val codes = listOf(popularRes, ongoingRes, completedRes)
                        .joinToString { it.code().toString() }
                    val detail = popularRes.errorBody()?.string()
                        ?: ongoingRes.errorBody()?.string()
                        ?: completedRes.errorBody()?.string()
                    Timber.e(
                        "loadHome: semua daftar anime kosong. HTTP=%s body=%s",
                        codes,
                        detail?.take(300).orEmpty()
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        popular = emptyList(),
                        ongoing = emptyList(),
                        completed = emptyList(),
                        error = "Gagal memuat daftar anime (HTTP $codes, daftar kosong)"
                    )
                }
            } catch (e: Exception) {
                // JANGAN telan exception: tampilkan sebab asli (mis. SSL / timeout).
                Timber.e(e, "loadHome gagal: %s", e.javaClass.simpleName)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.javaClass.simpleName + ": " +
                        (e.localizedMessage ?: "Koneksi bermasalah")
                )
            }
        }
    }

    fun retry() = loadHome()

    fun loadDetail(url: String) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Anime tidak ditemukan"
            )
            return
        }
        lastAnimeUrl = url
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val data = api.getAnimeDetail(details = url).body()?.data
                if (data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = data,
                        episodes = data.episodes.orEmpty(),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat detail anime"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat detail anime"
                )
            }
        }
    }

    fun retryDetail() {
        lastAnimeUrl?.let { loadDetail(it) }
    }

    /**
     * Ambil link streaming episode dari Nexadev lalu ubah ke URL mp4 yang
     * bisa diputar ExoPlayer.
     *
     * Nexadev mengembalikan `streamUrl` berupa halaman Blogger. Di dalam
     * halaman itu ada URL googlevideo (video/mp4). Bila tidak ditemukan,
     * streamUrl dibiarkan null agar UI bisa menampilkan pesan.
     */
    /** Episode yang sedang diputar, dipakai untuk judul & navigasi prev/next. */
    private var lastStreamEpisodeUrl: String? = null

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

    /** Pencarian anime. */
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
                // Unescape HTML entity yang umum dipakai di halaman blogger.
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
        private const val USER_AGENT = "okhttp/4.12.0"
        private val GOOGLEVIDEO_REGEX = Regex(
            """https://rr\d+---[a-z0-9-]+\.googlevideo\.com/videoplayback\?[^"\\\s]*"""
        )
    }
}