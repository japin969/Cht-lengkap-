package com.jayra.app.ui.neko

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.NekoApi
import com.jayra.app.data.NekoDetailData
import com.jayra.app.data.NekoEpisode
import com.jayra.app.data.NekoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.runCatching

data class NekoUiState(
    val isLoading: Boolean = false,
    val ongoing: List<NekoItem> = emptyList(),
    val popular: List<NekoItem> = emptyList(),
    val completed: List<NekoItem> = emptyList(),
    val detail: NekoDetailData? = null,
    val episodes: List<NekoEpisode> = emptyList(),
    val streamUrl: String? = null,
    val error: String? = null
) {
    val hasAnyData: Boolean
        get() = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()
}

class NekoViewModel : ViewModel() {
    private val api = NekoApi.create()
    private val _uiState = MutableStateFlow(NekoUiState())
    val uiState: StateFlow<NekoUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val popular = runCatching {
                    api.popular(page = 1).body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())
                val ongoing = runCatching {
                    api.ongoing(page = 1).body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())
                val completed = runCatching {
                    api.completed(page = 1).body()?.data?.animeList.orEmpty()
                }.getOrDefault(emptyList())

                val hasData = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    popular = popular,
                    ongoing = ongoing,
                    completed = completed,
                    error = if (hasData) null else "Gagal memuat daftar neko"
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
                val res = api.detail(url)
                val data = res.body()?.data
                if (res.isSuccessful && data != null) {
                    val info = buildMap {
                        data.info?.forEach { (k, v) -> put(k, v) }
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = data,
                        episodes = data.episodes.orEmpty(),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat detail neko"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat detail neko"
                )
            }
        }
    }

    fun loadStream(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val res = api.stream(url)
                val data = res.body()?.data
                val streamUrl = data?.streamUrl
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamUrl = streamUrl
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamUrl = null
                )
            }
        }
    }
}