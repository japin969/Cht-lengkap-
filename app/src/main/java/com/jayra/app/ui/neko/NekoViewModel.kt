package com.jayra.app.ui.neko

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.NekopoiApi
import com.jayra.app.data.NkItem
import com.jayra.app.data.StreamResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NekoUiState(
    val isLoading: Boolean = false,
    val latest: List<AnimeSearchItem> = emptyList(),
    val recommended: List<AnimeSearchItem> = emptyList(),
    val detail: AnimeSearchItem? = null,
    val episodes: List<AnimeSearchItem> = emptyList(),
    val streamUrl: String? = null,
    val servers: List<Pair<String, String>> = emptyList(),
    val error: String? = null
)

/**
 * NekoViewModel — fitur STANDALONE. Sumber: NekopoiApi. PARELAL.
 */
class NekoViewModel : ViewModel() {

    private val api = NekopoiApi.create()
    private val _uiState = MutableStateFlow(NekoUiState())
    val uiState: StateFlow<NekoUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val latestD = async { runCatching { api.latestHentai().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
            val homeD = async { runCatching { api.home().body()?.data }.getOrNull() }
            val latest = latestD.await().map { it.toSearch() }
            val home = homeD.await()
            val recommended = home?.recommended.orEmpty().map { it.toSearch() }
            val hasData = latest.isNotEmpty() || recommended.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                latest = latest,
                recommended = recommended,
                error = if (hasData) null else "Gagal memuat data nekopoi"
            )
        }
    }

    fun retry() = load()

    fun loadDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val d = api.detail(slug).body()?.data
                if (d != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = d.toSearch(),
                        episodes = d.episodes.orEmpty().map { it.toSearch() },
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Detail tidak ditemukan")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memuat detail")
            }
        }
    }

    fun loadStream(episodeSlug: String?) {
        if (episodeSlug.isNullOrBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val ep = api.episode(episodeSlug).body()?.data
                val servers = ep?.streams.orEmpty().mapNotNull { s ->
                    val u = s.url ?: return@mapNotNull null
                    (s.server ?: "Server") to (StreamResolver.resolve(u) ?: u)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    servers = servers,
                    streamUrl = servers.firstOrNull()?.second,
                    error = if (servers.isEmpty()) "Server tidak tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memuat")
            }
        }
    }

    private fun NkItem.toSearch(): AnimeSearchItem = AnimeSearchItem(
        title = title, link = slug, image = thumbnail, latestEpisode = null, type = "Nekopoi"
    )
    private fun com.jayra.app.data.NkDetail.toSearch(): AnimeSearchItem = AnimeSearchItem(
        title = title, link = slug, image = thumbnail, latestEpisode = null, type = "Nekopoi"
    )
}