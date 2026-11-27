package com.jayra.app.ui.drama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaApi
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.DramaItem
import com.jayra.app.data.DramaGenericItem
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
                val donghuaHome = async { runCatching { api.donghuaHome().body()?.results ?: api.donghuaHome().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
                val donghubHome = async { runCatching { api.donghubHome().body()?.results ?: api.donghubHome().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
                val donghuaLatest = async { runCatching { api.donghuaLatest().body()?.results ?: api.donghuaLatest().body()?.data.orEmpty() }.getOrDefault(emptyList()) }
                val donghubLatest = async { runCatching { api.donghubLatest().body()?.results ?: api.donghubLatest().body()?.data.orEmpty() }.getOrDefault(emptyList()) }

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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dh = async { runCatching { api.donghuaDetail(url).body() }.getOrNull() }
                val db = async { runCatching { api.donghubDetail(url).body() }.getOrNull() }
                val (d1, d2) = awaitAll(dh, db)
                val detail = d1 ?: d2
                if (detail != null) {
                    val item = DramaItem(
                        id = url,
                        title = detail.title,
                        cover = detail.poster ?: detail.image,
                        intro = detail.synopsis,
                        url = url
                    )
                    val eps = detail.episodes.orEmpty().mapIndexed { i, e ->
                        DramaEpisode(
                            index = i + 1,
                            name = e.title,
                            unlock = true,
                            mp4 = e.url ?: e.slug,
                            videoUrl = e.url ?: e.slug
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

    private fun DramaGenericItem.toDramaItem(): DramaItem = DramaItem(
        id = slug ?: url ?: title,
        title = title,
        cover = poster ?: image ?: cover ?: thumbnail,
        intro = null,
        chapterCount = null,
        url = slug ?: url
    )
}