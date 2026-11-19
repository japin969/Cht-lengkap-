package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.KitsuAnime
import com.jayra.app.data.KitsuApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnimeUiState(
    // --- Home ---
    val isLoading: Boolean = false,
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    /** true = sedang menampilkan data contoh karena semua server gagal. */
    val isFallback: Boolean = false,
    val error: String? = null,

    // --- Detail ---
    val isDetailLoading: Boolean = false,
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val detailError: String? = null,

    val streamUrl: String? = null
)

class AnimeViewModel : ViewModel() {

    // Sumber utama: Kitsu. Mirror kitsu.app dipakai kalau kitsu.io diblokir / down.
    private val sources: List<KitsuApi> = listOf(
        KitsuApi.create(KitsuApi.BASE_URL),
        KitsuApi.create(KitsuApi.BASE_URL_MIRROR)
    )

    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isFallback = false) }
            var lastError: String? = null

            for (api in sources) {
                val result = runCatching { fetchHome(api) }
                val data = result.getOrNull()
                if (data != null) {
                    val (popular, ongoing, completed) = data
                    if (popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                popular = popular,
                                ongoing = ongoing,
                                completed = completed,
                                isFallback = false,
                                error = null
                            )
                        }
                        return@launch
                    }
                } else {
                    lastError = result.exceptionOrNull()?.userMessage() ?: "Koneksi bermasalah"
                }
            }

            // Semua sumber gagal → jangan biarkan layar kosong/loading terus.
            // Tampilkan data contoh + pesan jelas + tombol Coba Lagi.
            _uiState.update {
                it.copy(
                    isLoading = false,
                    popular = fallbackAnime,
                    ongoing = emptyList(),
                    completed = emptyList(),
                    isFallback = true,
                    error = lastError ?: "Server anime tidak merespons. Menampilkan data contoh."
                )
            }
        }
    }

    private suspend fun fetchHome(api: KitsuApi): Triple<List<AnimeSearchItem>, List<AnimeSearchItem>, List<AnimeSearchItem>> =
        coroutineScope {
            val popularDeferred = async {
                api.list(limit = 20, sort = "-userCount").body()?.data.orEmpty()
            }
            val ongoingDeferred = async {
                api.list(limit = 20, sort = "-userCount", status = "current").body()?.data.orEmpty()
            }
            val completedDeferred = async {
                api.list(limit = 20, sort = "-averageRating", status = "finished").body()?.data.orEmpty()
            }
            val popular = runCatching { popularDeferred.await() }.getOrDefault(emptyList())
            val ongoing = runCatching { ongoingDeferred.await() }.getOrDefault(emptyList())
            val completed = runCatching { completedDeferred.await() }.getOrDefault(emptyList())
            Triple(
                popular.map { it.toItem() },
                ongoing.map { it.toItem() },
                completed.map { it.toItem() }
            )
        }

    fun retry() = loadHome()

    /** Terima id Kitsu ("7442") atau URL penuh ("https://kitsu.io/anime/attack-on-titan"). */
    fun loadDetail(url: String) {
        val id = url.trim().substringAfterLast('/')
        viewModelScope.launch {
            _uiState.update {
                it.copy(isDetailLoading = true, detailError = null, detail = null, episodes = emptyList())
            }
            if (id.isBlank()) {
                _uiState.update {
                    it.copy(isDetailLoading = false, detailError = "Anime tidak ditemukan")
                }
                return@launch
            }

            var lastError: String? = null
            for (api in sources) {
                val result = runCatching { api.detail(id) }
                val res = result.getOrNull()
                val data = res?.body()?.data
                if (res != null && res.isSuccessful && data != null) {
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            detail = data.toDetail(),
                            episodes = emptyList(),
                            detailError = null
                        )
                    }
                    return@launch
                }
                lastError = when {
                    result.exceptionOrNull() != null ->
                        result.exceptionOrNull()?.userMessage() ?: "Koneksi bermasalah"
                    res != null -> "Server anime mengembalikan HTTP ${res.code()}"
                    else -> "Gagal memuat detail anime"
                }
            }

            _uiState.update {
                it.copy(
                    isDetailLoading = false,
                    detailError = lastError ?: "Gagal memuat detail anime"
                )
            }
        }
    }

    fun retryDetail(url: String) = loadDetail(url)

    fun loadStream(url: String) {
        // Kitsu tidak menyediakan streaming; detail tetap bisa dibuka.
        _uiState.update { it.copy(streamUrl = null) }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is java.net.UnknownHostException -> "Tidak ada koneksi internet"
        is java.net.SocketTimeoutException -> "Server anime lambat / timeout. Coba lagi."
        is javax.net.ssl.SSLException -> "Koneksi aman ke server anime gagal"
        is java.io.IOException -> localizedMessage ?: "Koneksi bermasalah"
        else -> localizedMessage ?: "Koneksi bermasalah"
    }

    private fun KitsuAnime.toItem(): AnimeSearchItem = AnimeSearchItem(
        title = attributes?.displayTitle(),
        link = id,
        image = attributes?.posterImage?.best()?.takeIf { it.isNotBlank() }
            ?: attributes?.coverImage?.best(),
        latestEpisode = attributes?.episodeCount?.let { "$it eps" } ?: attributes?.status,
        type = attributes?.subtype
    )

    private fun KitsuAnime.toDetail(): AnimeDetailData {
        val attrs = attributes
        val info = buildMap {
            attrs?.status?.let { put("Status", it) }
            attrs?.startDate?.let { put("Mulai", it) }
            attrs?.episodeCount?.let { put("Episode", "$it") }
            attrs?.averageRating?.let { put("Rating", it) }
        }
        return AnimeDetailData(
            title = attrs?.displayTitle(),
            image = attrs?.posterImage?.best()?.takeIf { it.isNotBlank() } ?: attrs?.coverImage?.best(),
            synopsis = attrs?.synopsis,
            rating = attrs?.averageRating?.toDoubleOrNull(),
            totalEpisodes = attrs?.episodeCount,
            genres = null,
            info = info,
            episodes = emptyList()
        )
    }

    companion object {
        /**
         * Data contoh terakhir kalau Kitsu utama + mirror sama-sama tidak bisa dihubungi.
         * Poster memakai CDN Kitsu; kalau CDN juga terblokir, kartu tetap tampil (tanpa gambar)
         * dan tombol Coba Lagi tetap tersedia.
         */
        private val fallbackAnime: List<AnimeSearchItem> = listOf(
            fallback("Attack on Titan", "7442"),
            fallback("Boku no Hero Academia", "11469"),
            fallback("Boku no Hero Academia 2", "12268"),
            fallback("One Punch Man", "10740"),
            fallback("Boku no Hero Academia 3", "13881"),
            fallback("Death Note", "1376"),
            fallback("Fullmetal Alchemist: Brotherhood", "3936"),
            fallback("Kimi no Na wa.", "11614"),
            fallback("Hunter x Hunter (2011)", "6448"),
            fallback("Kimetsu no Yaiba", "41370")
        )

        private fun fallback(title: String, id: String) = AnimeSearchItem(
            title = title,
            link = id,
            image = "https://media.kitsu.app/anime/poster_images/$id/large.jpg",
            latestEpisode = "Contoh",
            type = "TV"
        )
    }
}
