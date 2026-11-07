package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class KomikUiState(
    val isLoading: Boolean = false,
    val latest: List<KomikItem> = emptyList(),
    val detail: KomikDetailResponse? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val streamTitle: String? = null,
    val pages: List<KomikPage> = emptyList(),
    val error: String? = null
)

class KomikViewModel : ViewModel() {
    private val api = KomikApi.create()
    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.update()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    latest = res.body()?.results ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun loadDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.detail(slug)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = res.body(),
                    chapters = res.body()?.chapters ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun loadStream(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.stream(slug)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamTitle = res.body()?.chapterTitle,
                    pages = res.body()?.pages ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }
}
