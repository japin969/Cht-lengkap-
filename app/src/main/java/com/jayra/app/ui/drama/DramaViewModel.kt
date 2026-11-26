package com.jayra.app.ui.drama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.BaseRepository
import com.jayra.app.data.DramaRepository
import com.jayra.app.data.DramaRepository.DramaDetail
import com.jayra.app.data.DramaRepository.DramaEpisode
import com.jayra.app.data.DramaRepository.DramaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DramaUiState(
    val isLoading: Boolean = false,
    val featured: List<DramaItem> = emptyList(),
    val detail: DramaDetail? = null,
    val episodes: List<DramaEpisode> = emptyList(),
    val error: String? = null
)

class DramaViewModel : ViewModel() {
    private val repository = DramaRepository()
    private val _uiState = MutableStateFlow(DramaUiState())
    val uiState: StateFlow<DramaUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.fetchFeatured()

            if (result is DramaRepository.RepositoryResult.Success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    featured = result.data,
                    error = if (result.data.isEmpty()) "Gagal memuat daftar drama" else null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun retry() = loadHome()

    fun loadDetail(dramaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.fetchDetail(dramaId)

            if (result is DramaRepository.RepositoryResult.Success) {
                val detail = result.data.firstOrNull()
                val episodes = detail?.episodes ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = detail,
                    episodes = episodes,
                    error = if (detail != null) null else "Gagal memuat detail drama"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }
}