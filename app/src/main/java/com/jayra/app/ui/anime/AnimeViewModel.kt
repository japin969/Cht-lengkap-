package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeRepository
import com.jayra.app.data.AnimeRepository.AnimeDetail
import com.jayra.app.data.AnimeRepository.AnimeEpisode
import com.jayra.app.data.AnimeRepository.AnimeItem
import com.jayra.app.data.AnimeRepository.StreamUrl
import com.jayra.app.data.BaseRepository.RepositoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AnimeUiState(
    val isLoading: Boolean = false,
    val popular: List<AnimeItem> = emptyList(),
    val ongoing: List<AnimeItem> = emptyList(),
    val completed: List<AnimeItem> = emptyList(),
    val detail: AnimeDetail? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val streamUrl: StreamUrl? = null,
    val error: String? = null
) {
    val hasAnyData: Boolean
        get() = popular.isNotEmpty() || ongoing.isNotEmpty() || completed.isNotEmpty()
}

class AnimeViewModel : ViewModel() {
    private val repository = AnimeRepository()
    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.fetchHomeSections()
            
            // Combine all three section results
            val popularState = result.popular
            val ongoingState = result.ongoing
            val completedState = result.completed
            
            val allItems = (popularState.data + ongoingState.data + completedState.data).distinctBy { it.id ?: it.link ?: it.title ?: "" }
            val hasData = allItems.isNotEmpty()
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                popular = popularState.data,
                ongoing = ongoingState.data,
                completed = completedState.data,
                error = if (hasData) null else "Gagal memuat daftar anime dari semua sumber"
            )
            
            // Log merge stats
            logMergeStats("anime_home", popularState.data.size, ongoingState.data.size, completedState.data.size, allItems.size)
        }
    }

    fun retry() = loadHome()

    fun loadDetail(animeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.fetchDetail(animeId)
            
            if (result is AnimeRepository.RepositoryResult.Success) {
                val detail = result.data.firstOrNull()
                val episodes = detail?.episodes ?: emptyList()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = detail,
                    episodes = episodes,
                    error = if (detail != null) null else "Detail anime tidak ditemukan"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun loadStream(episodeUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.fetchStream(episodeUrl)
            
            if (result is AnimeRepository.RepositoryResult.Success) {
                val stream = result.data.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamUrl = stream
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamUrl = null
                )
            }
        }
    }

    private fun logMergeStats(tag: String, popular: Int, ongoing: Int, completed: Int, total: Int) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("AnimeMerge", "[$tag] popular=$popular, ongoing=$ongoing, completed=$completed, merged=$total")
        }
    }
}

import android.os.BuildConfig