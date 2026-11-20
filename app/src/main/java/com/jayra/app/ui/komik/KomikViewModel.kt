package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikApi
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikDetailResponse
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class KomikUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val activeCategory: String = "home",
    val activeGenre: String = "action",
    val currentPage: Int = 1,
    val hasNext: Boolean = false,
    val popular: List<KomikItem> = emptyList(),
    val latest: List<KomikItem> = emptyList(),
    val items: List<KomikItem> = emptyList(),
    val searchResults: List<KomikItem> = emptyList(),
    val detail: KomikDetailResponse? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val isDetailLoading: Boolean = false,
    val isStreamLoading: Boolean = false,
    val streamTitle: String? = null,
    val comicTitle: String? = null,
    val images: List<String> = emptyList(),
    val navigation: KomikNavigation? = null,
    val error: String? = null,
    val detailError: String? = null,
    val streamError: String? = null
)

@OptIn(FlowPreview::class)
class KomikViewModel : ViewModel() {
    private val api = KomikApi.create()
    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")

    init {
        loadCategory("home", page = 1)
        viewModelScope.launch {
            _searchFlow
                .debounce(450L)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isNotBlank()) {
                        performSearch(q)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            searchResults = emptyList(),
                            error = null
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchFlow.value = query.trim()
    }

    fun searchNow() {
        val q = _uiState.value.searchQuery.trim()
        if (q.isNotBlank()) {
            viewModelScope.launch { performSearch(q) }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true, error = null)
        try {
            val res = withContext(Dispatchers.IO) { api.search(q = query) }
            val body = res.body()
            if (res.isSuccessful && body?.success == true) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = body.results.orEmpty()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = "Gagal mencari komik (HTTP ${res.code()})"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                error = e.localizedMessage ?: "Gagal mencari komik"
            )
        }
    }

    fun loadCategory(category: String, page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = page == 1,
                isLoadingMore = page > 1,
                activeCategory = category,
                error = null
            )
            try {
                if (category == "home") {
                    val res = withContext(Dispatchers.IO) { api.home() }
                    val body = res.body()
                    if (res.isSuccessful && body?.success == true) {
                        val pop = body.sections?.popular.orEmpty()
                        val lat = body.sections?.latest.orEmpty()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            popular = pop,
                            latest = lat,
                            hasNext = false,
                            error = if (pop.isEmpty() && lat.isEmpty()) "Beranda komik kosong" else null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = "Gagal memuat komik (HTTP ${res.code()})"
                        )
                    }
                    return@launch
                }

                val res = withContext(Dispatchers.IO) {
                    if (category == "genre") api.genre(genre = _uiState.value.activeGenre, page = page)
                    else api.list(endpoint = category, page = page)
                }
                val body = res.body()
                if (res.isSuccessful && body?.success == true) {
                    val incoming = body.results.orEmpty()
                    val merged = if (page == 1) incoming else (_uiState.value.items + incoming).distinctBy { it.resolvedSlug }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        currentPage = page,
                        hasNext = body.hasNext == true,
                        items = merged,
                        error = if (merged.isEmpty()) "Daftar komik kosong" else null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = "Gagal memuat komik (HTTP ${res.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.localizedMessage ?: "Gagal memuat komik"
                )
            }
        }
    }

    fun selectGenre(genre: String) {
        _uiState.value = _uiState.value.copy(activeGenre = genre)
        loadCategory("genre", page = 1)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (!state.isLoading && !state.isLoadingMore && state.hasNext) {
            loadCategory(state.activeCategory, page = state.currentPage + 1)
        }
    }

    fun loadDetail(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(detailError = "Slug komik tidak valid")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetailLoading = true, detailError = null)
            try {
                val res = withContext(Dispatchers.IO) { api.detail(slug = slug) }
                val body = res.body()
                if (res.isSuccessful && body?.success == true && !body.title.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isDetailLoading = false,
                        detail = body,
                        chapters = body.chapters.orEmpty()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDetailLoading = false,
                        detailError = "Gagal memuat detail komik (HTTP ${res.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDetailLoading = false,
                    detailError = e.localizedMessage ?: "Gagal memuat detail komik"
                )
            }
        }
    }

    fun loadStream(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(streamError = "Slug chapter tidak valid")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStreamLoading = true, streamError = null)
            try {
                val res = withContext(Dispatchers.IO) { api.stream(slug = slug) }
                val body = res.body()
                val imgs = body?.images?.filter { it.isNotBlank() }.orEmpty()
                if (res.isSuccessful && body?.success == true && imgs.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isStreamLoading = false,
                        streamTitle = body.displayTitle,
                        comicTitle = body.comicTitle,
                        images = imgs,
                        navigation = body.navigation
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isStreamLoading = false,
                        streamError = "Halaman chapter tidak tersedia"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isStreamLoading = false,
                    streamError = e.localizedMessage ?: "Gagal memuat halaman chapter"
                )
            }
        }
    }
}
