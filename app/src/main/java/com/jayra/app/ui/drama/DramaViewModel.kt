package com.jayra.app.ui.drama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.DramaItem
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
                if (res.isSuccessful && res.body()?.ok == true) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        featured = res.body()?.featured ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Gagal memuat drama")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun loadDetail(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.getDramaDetail(url)
                if (res.isSuccessful && res.body()?.ok == true) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = res.body()?.book,
                        episodes = res.body()?.episodes ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Gagal memuat detail")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }
}
