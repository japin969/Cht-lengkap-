package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikApi
import com.jayra.app.data.KomikItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class KomikUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val items: List<KomikItem> = emptyList(),
    val searchResults: List<KomikItem> = emptyList(),
    val detailItem: KomikItem? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val streamTitle: String? = null,
    val streamPages: List<String> = emptyList(),
    val error: String? = null,
    val hasNext: Boolean = false,
    val currentPage: Int = 1
)

class KomikViewModel : ViewModel() {
    private val api = KomikApi.create()
    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    init { loadHome(page = 1) }

    fun loadHome(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, currentPage = page)
            try {
                val res = api.list(page)
                val response = res.body()
                if (res.isSuccessful && response != null && response.success == true) {
                    val newItems = response.results.orEmpty()
                    val allItems = if (page == 1) newItems else _uiState.value.items + newItems
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = allItems,
                        hasNext = response.hasNext == true,
                        error = if (allItems.isEmpty()) "Belum ada komik" else null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat daftar komik: ${res.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun loadNextPage() {
        val nextPage = _uiState.value.currentPage + 1
        if (!_uiState.value.isLoading && _uiState.value.hasNext) {
            loadHome(nextPage)
        }
    }

    fun retry() = loadHome(page = 1)

    fun search(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            _uiState.value = _uiState.value.copy(searchQuery = "", searchResults = emptyList(), isSearching = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchQuery = cleanQuery, isSearching = true, error = null)
            try {
                val res = api.search(cleanQuery)
                val response = res.body()
                if (res.isSuccessful && response != null && response.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchResults = response.results.orEmpty(),
                        error = if (response.results.isNullOrEmpty()) "Tidak ditemukan hasil untuk \"$cleanQuery\"" else null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        error = "Pencarian gagal: ${res.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "", searchResults = emptyList(), isSearching = false)
    }

    fun loadDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // API returns list format, we filter by slug
                val res = api.detail(slug)
                val response = res.body()
                if (res.isSuccessful && response != null && response.success == true) {
                    val item = response.results?.firstOrNull { it.resolvedSlug == slug }
                    if (item != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            detailItem = item,
                            error = null
                        )
                    } else {
                        // Try to find in home items as fallback
                        val fallback = _uiState.value.items.firstOrNull { it.resolvedSlug == slug }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            detailItem = fallback,
                            error = if (fallback == null) "Komik tidak ditemukan" else null
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat detail: ${res.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat detail komik"
                )
            }
        }
    }

    fun loadStream(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // API returns list format, we filter by slug
                val res = api.stream(slug)
                val response = res.body()
                if (res.isSuccessful && response != null && response.success == true) {
                    val item = response.results?.firstOrNull { it.resolvedSlug == slug }
                    val title = item?.cleanTitle ?: _uiState.value.detailItem?.cleanTitle ?: slug
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streamTitle = title,
                        streamPages = emptyList(), // API doesn't provide chapter images
                        error = "API tidak menyediakan gambar chapter. Silakan buka link asli: ${item?.link ?: "tidak tersedia"}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streamTitle = _uiState.value.detailItem?.cleanTitle ?: slug,
                        streamPages = emptyList(),
                        error = "Gagal memuat chapter: ${res.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamTitle = _uiState.value.detailItem?.cleanTitle ?: slug,
                    streamPages = emptyList(),
                    error = e.localizedMessage ?: "Gagal memuat chapter"
                )
            }
        }
    }
}

// Re-export for UI convenience
typealias KomikChapter = com.jayra.app.data.KomikChapter