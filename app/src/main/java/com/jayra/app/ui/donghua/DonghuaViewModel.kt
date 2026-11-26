package com.jayra.app.ui.donghua

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.DramaItem
import com.jayra.app.data.TvmazeApi
import com.jayra.app.data.TvmazeShow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DonghuaUiState(
    val isLoading: Boolean = false,
    val featured: List<DramaItem> = emptyList(),
    val popular: List<DramaItem> = emptyList(),
    val latest: List<DramaItem> = emptyList(),
    val results: List<DramaItem> = emptyList(),
    val query: String = "",
    val activeGenre: String? = null,
    val detail: DramaItem? = null,
    val episodes: List<DramaEpisode> = emptyList(),
    val streamUrl: String? = null,
    val error: String? = null
)

/**
 * DonghuaViewModel — metadata TVMaze (SSL valid). Player memakai sample publik
 * agar donghua/drama selalu bisa diputar (tidak hitam).
 */
class DonghuaViewModel : ViewModel() {

    private val api = TvmazeApi.create()
    private val _uiState = MutableStateFlow(DonghuaUiState())
    val uiState: StateFlow<DonghuaUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val shows = api.shows(page = 0).body().orEmpty()
                val items = shows
                    .filter { !it.image?.original.isNullOrBlank() || !it.image?.medium.isNullOrBlank() }
                    .map { it.toItem() }
                val third = (items.size / 3).coerceAtLeast(1)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    featured = items.take(third).ifEmpty { items },
                    popular = items.drop(third).take(third),
                    latest = items.drop(third * 2),
                    error = if (items.isEmpty()) "Gagal memuat daftar donghua" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun retryHome() = loadHome()

    fun loadList(genre: String? = null) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeGenre = genre)
        viewModelScope.launch {
            try {
                val items: List<DramaItem> = if (genre != null) {
                    api.search(genre).body().orEmpty().mapNotNull { it.show?.toItem() }
                } else {
                    api.shows(page = 0).body().orEmpty()
                        .filter { !it.image?.original.isNullOrBlank() || !it.image?.medium.isNullOrBlank() }
                        .map { it.toItem() }
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = items,
                    error = if (items.isEmpty()) "Tidak ada donghua" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false, error = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = api.search(query).body().orEmpty().mapNotNull { it.show?.toItem() }
                _uiState.value = _uiState.value.copy(isLoading = false, results = items, error = if (items.isEmpty()) "Tidak ada hasil" else null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun retrySearch() = search(_uiState.value.query)

    fun loadDetail(url: String) {
        val id = url.toLongOrNull()
        if (id == null) {
            _uiState.value = _uiState.value.copy(error = "Donghua tidak ditemukan")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.detail(id)
                val show = res.body()
                if (res.isSuccessful && show != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = show.toItem(),
                        episodes = show.embedded?.episodes.orEmpty().map { ep ->
                            DramaEpisode(index = ep.number, name = ep.name, unlock = true, mp4 = null, videoUrl = id.toString())
                        },
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Gagal memuat detail donghua")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memuat detail")
            }
        }
    }

    fun retryDetail(url: String) = loadDetail(url)

    fun loadStream(episodeUrl: String?) {
        val url = episodeUrl?.takeIf { it.startsWith("http") } ?: SAMPLE_STREAM
        _uiState.value = _uiState.value.copy(streamUrl = url)
    }

    private fun TvmazeShow.toItem(): DramaItem = DramaItem(
        id = id?.toString(),
        title = name,
        cover = image?.original ?: image?.medium,
        intro = summary?.let { stripHtml(it) },
        chapterCount = embedded?.episodes?.size,
        url = id?.toString()
    )

    private fun stripHtml(raw: String): String =
        raw.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"").trim()

    companion object {
        const val SAMPLE_STREAM =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}