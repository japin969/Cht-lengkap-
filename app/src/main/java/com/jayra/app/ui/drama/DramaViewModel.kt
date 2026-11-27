package com.jayra.app.ui.drama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaItem
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.NexadevApi
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
    private val api = NexadevApi.create()
    private val _uiState = MutableStateFlow(DramaUiState())
    val uiState: StateFlow<DramaUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.getDramaHome()
                val body = res.body()
                val items = mutableListOf<DramaItem>()
                body?.featured?.let { items.addAll(it) }
                body?.sections?.forEach { sec -> sec.items?.let { items.addAll(it) } }
                val distinct = items.distinctBy { it.url ?: it.id ?: it.title }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    featured = distinct,
                    error = if (distinct.isEmpty()) "Gagal memuat daftar drama" else null
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
                val res = api.getDramaDetail(url)
                val body = res.body()
                val book = body?.book
                if (res.isSuccessful && book != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = book,
                        episodes = body.episodes.orEmpty(),
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
}