package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.BaseRepository
import com.jayra.app.data.KomikRepository
import com.jayra.app.data.KomikRepository.KomikChapter
import com.jayra.app.data.KomikRepository.KomikDetail
import com.jayra.app.data.KomikRepository.KomikItem
import com.jayra.app.data.KomikRepository.KomikPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class KomikUiState(
    val isLoading: Boolean = false,
    val latest: List<KomikItem> = emptyList(),
    val detail: KomikDetail? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val streamTitle: String? = null,
    val pages: List<KomikPage> = emptyList(),
    val error: String? = null
)

class KomikViewModel : ViewModel() {
    private val repository = KomikRepository()
    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.fetchLatest()
            
            if (result is KomikRepository.RepositoryResult.Success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    latest = result.data,
                    error = if (result.data.isEmpty()) "Gagal memuat daftar komik" else null
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

    fun loadDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.fetchDetail(slug)
            
            if (result is KomikRepository.RepositoryResult.Success) {
                val detail = result.data.firstOrNull()
                val chapters = detail?.chapters ?: emptyList()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = detail,
                    chapters = chapters,
                    error = if (detail != null) null else "Gagal memuat detail komik"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun loadStream(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.fetchChapterImages(slug)
            
            if (result is KomikRepository.RepositoryResult.Success) {
                val pages = result.data
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamTitle = _uiState.value.detail?.title,
                    pages = pages,
                    error = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
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