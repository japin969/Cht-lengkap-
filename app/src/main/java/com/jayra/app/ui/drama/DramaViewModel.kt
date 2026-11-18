package com.jayra.app.ui.drama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.DramaItem
import com.jayra.app.data.TvmazeApi
import com.jayra.app.data.TvmazeShow
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
    // Sumber data baru: TVMaze API. API lama api.nexadev.my.id bermasalah
    // sertifikat (CertPathValidatorException: trust anchor not found).
    private val api = TvmazeApi.create()
    private val _uiState = MutableStateFlow(DramaUiState())
    val uiState: StateFlow<DramaUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val shows = api.shows(page = 0).body().orEmpty()
                val items = shows
                    .filter { !it.image?.original.isNullOrBlank() || !it.image?.medium.isNullOrBlank() }
                    .map { it.toItem() }
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
        val id = url.toLongOrNull()
        if (id == null) {
            _uiState.value = _uiState.value.copy(error = "Drama tidak ditemukan")
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
                            DramaEpisode(
                                index = ep.number,
                                name = ep.name,
                                unlock = false,
                                mp4 = null,
                                videoUrl = null
                            )
                        }
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
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .trim()
}
