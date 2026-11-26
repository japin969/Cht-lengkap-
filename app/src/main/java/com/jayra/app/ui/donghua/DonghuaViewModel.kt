package com.jayra.app.ui.donghua

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DhEpisodeLink
import com.jayra.app.data.DhItem
import com.jayra.app.data.DonghuaApi
import com.jayra.app.data.StreamResolver
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.DramaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLException

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
    val servers: List<Pair<String, String>> = emptyList(),
    val error: String? = null
)

/**
 * DonghuaViewModel — sumber resmi: https://www.sankavollerei.web.id (donghua).
 * Player memakai embed server (ok.ru/rumble/dst) via WebView.
 */
class DonghuaViewModel : ViewModel() {

    private val api = DonghuaApi.create()
    private val _uiState = MutableStateFlow(DonghuaUiState())
    val uiState: StateFlow<DonghuaUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = api.latest().body()?.latest_donghua.orEmpty().map { it.toItem() }
                val third = (items.size / 3).coerceAtLeast(1)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    featured = items.take(third).ifEmpty { items },
                    popular = items.drop(third).take(third),
                    latest = items.drop(third * 2),
                    error = if (items.isEmpty()) "Gagal memuat daftar donghua" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(e))
            }
        }
    }

    fun retryHome() = loadHome()

    fun loadList(genre: String? = null) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeGenre = genre)
        viewModelScope.launch {
            try {
                // Genre donghua → search teks (sumber tidak punya endpoint genre khusus).
                val items: List<DramaItem> = if (genre != null) {
                    api.search(genre).body()?.data.orEmpty().map { it.toItem() }
                } else {
                    api.latest().body()?.latest_donghua.orEmpty().map { it.toItem() }
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = items,
                    error = if (items.isEmpty()) "Tidak ada donghua" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(e))
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
                val items = api.search(query).body()?.data.orEmpty().map { it.toItem() }
                _uiState.value = _uiState.value.copy(isLoading = false, results = items, error = if (items.isEmpty()) "Tidak ada hasil" else null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(e))
            }
        }
    }

    fun retrySearch() = search(_uiState.value.query)

    fun loadDetail(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Donghua tidak ditemukan")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.detail(slug)
                val d = res.body()
                if (res.isSuccessful && d != null && (d.title != null || d.poster != null)) {
                    val eps = d.episodes_list.orEmpty().map { it.toEpisode() }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = d.toItem(),
                        episodes = eps,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Gagal memuat detail donghua")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(e))
            }
        }
    }

    fun retryDetail(slug: String) = loadDetail(slug)

    /**
     * Ambil daftar server embed dari episode. URL embed (ok.ru/rumble) dibuka via WebView.
     */
    fun loadStream(episodeSlug: String?) {
        if (episodeSlug.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(streamUrl = null, servers = emptyList())
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.episode(episodeSlug)
                val ep = res.body()
                val raw = buildList {
                    ep?.streaming?.main_url?.let { add((it.name ?: "Server 1") to (it.url ?: "")) }
                    ep?.streaming?.servers?.forEach { add((it.name ?: "Server") to (it.url ?: "")) }
                }.filter { it.second.isNotBlank() }
                // Resolve embed -> direct media URL (native, tanpa WebView)
                val servers = raw.map { (label, u) -> label to (StreamResolver.resolve(u) ?: u) }
                val direct = servers.firstOrNull { StreamResolver.isDirect(it.second) }?.second
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    servers = servers,
                    streamUrl = direct ?: servers.firstOrNull()?.second,
                    error = if (servers.isEmpty()) "Server streaming tidak tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(e))
            }
        }
    }

    private fun DhItem.toItem(): DramaItem = DramaItem(
        id = slug,
        title = title,
        cover = poster,
        intro = null,
        chapterCount = null,
        url = slug
    )

    private fun com.jayra.app.data.DonghuaDetailResponse.toItem(): DramaItem = DramaItem(
        id = null,
        title = title,
        cover = poster,
        intro = synopsis,
        chapterCount = episodes_count?.toIntOrNull(),
        url = null
    )

    private fun DhEpisodeLink.toEpisode(): DramaEpisode = DramaEpisode(
        index = episode_number?.toIntOrNull(),
        name = episode,
        unlock = true,
        mp4 = null,
        videoUrl = slug
    )

    private fun friendlyError(e: Exception): String = when (e) {
        is SSLException -> "Koneksi tidak aman (SSL). Periksa tanggal/jam perangkat atau jaringan."
        is CertPathValidatorException -> "Sertifikat server tidak valid. Coba jaringan lain."
        is IOException -> "Tidak ada koneksi ke server. Periksa jaringan Anda."
        else -> e.localizedMessage ?: "Koneksi bermasalah"
    }
}