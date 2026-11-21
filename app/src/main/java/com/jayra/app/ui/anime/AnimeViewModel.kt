package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.KitsuAnime
import com.jayra.app.data.KitsuApi
import com.jayra.app.data.KitsuEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AnimeUiState(
    val isLoading: Boolean = false,
    val ongoing: List<AnimeSearchItem> = emptyList(),
    val popular: List<AnimeSearchItem> = emptyList(),
    val completed: List<AnimeSearchItem> = emptyList(),
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    /** Menandakan episode sedang dimuat. */
    val episodesLoading: Boolean = false,
    val streamUrl: String? = null,
    /** Error umum (home / detail). */
    val error: String? = null,
    val homeError: String? = null,
    val detailError: String? = null
)

class AnimeViewModel : ViewModel() {
    // Sumber data baru: Kitsu API (API lama apii.nexadev.my.id sudah mati / 503).
    private val api = KitsuApi.create()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    private var lastAnimeId: String? = null

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            try {
                val popular = runCatching {
                    api.list(limit = 20, sort = "-userCount").body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val ongoing = runCatching {
                    api.list(limit = 20, sort = "-userCount", status = "current")
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val completed = runCatching {
                    api.list(limit = 20, sort = "-averageRating", status = "finished")
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                val mappedPopular = popular.map { it.toItem() }
                val mappedOngoing = ongoing.map { it.toItem() }
                val mappedCompleted = completed.map { it.toItem() }
                val hasData = mappedPopular.isNotEmpty() ||
                    mappedOngoing.isNotEmpty() ||
                    mappedCompleted.isNotEmpty()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    popular = mappedPopular,
                    ongoing = mappedOngoing,
                    completed = mappedCompleted,
                    error = if (hasData) null else "Gagal memuat daftar anime",
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
        lastAnimeId = url
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, detailError = null)
            try {
                val res = api.detail(url)
                val data = res.body()?.data
                if (res.isSuccessful && data != null) {
                    val attrs = data.attributes
                    val info = buildMap {
                        attrs?.status?.let { put("Status", it) }
                        attrs?.startDate?.let { put("Mulai", it) }
                        attrs?.episodeCount?.let { put("Episode", "$it") }
                        attrs?.subtype?.let { put("Tipe", it) }
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = AnimeDetailData(
                            title = attrs?.displayTitle(),
                            image = attrs?.posterImage?.best()?.takeIf { it.isNotBlank() }
                                ?: attrs?.coverImage?.best(),
                            synopsis = attrs?.synopsis,
                            rating = attrs?.averageRating?.toDoubleOrNull(),
                            totalEpisodes = attrs?.episodeCount,
                            genres = null,
                            info = info,
                            episodes = emptyList()
                        ),
                        episodes = emptyList(),
                        detailError = null
                    )
                    // Episode dimuat menyusul agar detail cepat tampil.
                    loadEpisodes(url)
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
        lastAnimeId?.let { loadDetail(it) }
    }

    /** Muat ulang hanya daftar episode (dipakai tombol Retry di section episode). */
    fun retryEpisodes() {
        lastAnimeId?.let { loadEpisodes(it) }
    }

    private fun loadEpisodes(animeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(episodesLoading = true)
            val list = runCatching {
                api.episodes(animeId).body()?.data.orEmpty()
            }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                episodesLoading = false,
                episodes = list.map { it.toEpisode() }
            )
        }
    }

    fun loadStream(url: String) {
        // Kitsu tidak menyediakan streaming; detail tetap bisa dibuka.
        _uiState.value = _uiState.value.copy(streamUrl = null)
    }

    /** Pencarian Kitsu. Return hasil agar screen bisa menampilkan state sendiri. */
    suspend fun search(query: String): List<AnimeSearchItem> {
        if (query.isBlank()) return emptyList()
        return runCatching {
            api.search(query.trim()).body()?.data.orEmpty().map { it.toItem() }
        }.getOrElse { emptyList() }
    }

    private fun KitsuAnime.toItem(): AnimeSearchItem = AnimeSearchItem(
        title = attributes?.displayTitle(),
        link = id,
        image = attributes?.posterImage?.best()?.takeIf { it.isNotBlank() }
            ?: attributes?.coverImage?.best(),
        latestEpisode = attributes?.episodeCount?.let { "$it eps" } ?: attributes?.status,
        type = attributes?.subtype
    )

    private fun KitsuEpisode.toEpisode(): AnimeEpisode {
        val number = attributes?.number
        return AnimeEpisode(
            episode = number?.toString(),
            title = attributes?.displayTitle()?.takeIf { it.isNotBlank() }
                ?: number?.let { "Episode $it" }
                ?: "Episode",
            episodeUrl = id
        )
    }
}