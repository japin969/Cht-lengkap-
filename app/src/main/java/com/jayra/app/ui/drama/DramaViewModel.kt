package com.jayra.app.ui.drama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaApi
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.DramaItem
import com.jayra.app.data.DramaGenericItem
import com.jayra.app.data.DonghuaDetailResponse
import com.jayra.app.data.DonghubDetailResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DramaUiState(
    val isLoading: Boolean = false,
    val featured: List<DramaItem> = emptyList(),
    val detail: DramaItem? = null,
    val episodes: List<DramaEpisode> = emptyList(),
    val streamUrl: String? = null,
    val isLoadingStream: Boolean = false,
    val error: String? = null
)

class DramaViewModel : ViewModel() {
    private val api = DramaApi.create()
    private val _uiState = MutableStateFlow(DramaUiState())
    val uiState: StateFlow<DramaUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val donghuaHome = async { runCatching { api.donghuaHome().body()?.latestRelease.orEmpty() }.getOrDefault(emptyList()) }
                val donghubHome = async { runCatching { api.donghubHome().body()?.data?.slider.orEmpty() }.getOrDefault(emptyList()) }
                val donghuaLatest = async { runCatching { api.donghuaLatest().body()?.latestDonghua.orEmpty() }.getOrDefault(emptyList()) }
                val donghubLatest = async { runCatching { api.donghubLatest().body()?.data.orEmpty() }.getOrDefault(emptyList()) }

                val all = awaitAll(donghuaHome, donghubHome, donghuaLatest, donghubLatest).flatten()
                val items = all.map { it.toDramaItem() }.distinctBy { it.url ?: it.id ?: it.title }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    featured = items,
                    error = if (items.isEmpty()) "Gagal memuat daftar drama" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun retry() = loadHome()

    fun loadDetail(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, streamUrl = null)
            try {
                val d1 = async { runCatching { api.donghuaDetail(url).body() }.getOrNull() }.await()
                val d2 = async { runCatching { api.donghubDetail(url).body() }.getOrNull() }.await()
                if (d1 != null) {
                    val title = d1.title
                    val cover = d1.poster ?: d1.cover
                    val synopsis = d1.synopsis
                    val epsList = d1.episodesList
                    val item = DramaItem(
                        id = url,
                        title = title,
                        cover = cover,
                        intro = synopsis,
                        url = url
                    )
                    val eps = epsList.orEmpty().mapIndexed { i, e ->
                        DramaEpisode(
                            index = e.episodeNumber?.toIntOrNull() ?: (i + 1),
                            name = e.episode,
                            unlock = true,
                            mp4 = e.slug,
                            videoUrl = e.slug
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = item,
                        episodes = eps,
                        error = null
                    )
                } else if (d2 != null) {
                    val dd = d2.data
                    val item = DramaItem(
                        id = url,
                        title = dd?.title,
                        cover = dd?.poster,
                        intro = dd?.synopsis,
                        url = url
                    )
                    val eps = dd?.episodes.orEmpty().mapIndexed { i, e ->
                        DramaEpisode(
                            index = i + 1,
                            name = e.title,
                            unlock = true,
                            mp4 = e.slug,
                            videoUrl = e.slug
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = item,
                        episodes = eps,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat detail drama"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat detail drama"
                )
            }
        }
    }

    fun loadStream(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStream = true, streamUrl = null)
            try {
                val d1 = async { runCatching { api.donghuaEpisode(slug).body() }.getOrNull() }.await()
                val d2 = async { runCatching { api.donghubEpisode(slug).body() }.getOrNull() }.await()
                val url = d1?.streaming?.mainUrl?.url
                    ?: d1?.streaming?.servers?.firstOrNull()?.url
                    ?: d2?.data?.streams?.firstOrNull()?.url
                _uiState.value = _uiState.value.copy(
                    isLoadingStream = false,
                    streamUrl = url
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStream = false,
                    error = e.localizedMessage ?: "Gagal memuat stream"
                )
            }
        }
    }

    private fun DramaGenericItem.toDramaItem(): DramaItem = DramaItem(
        id = slug ?: title,
        title = title,
        cover = poster,
        intro = null,
        chapterCount = null,
        url = slug
    )
}