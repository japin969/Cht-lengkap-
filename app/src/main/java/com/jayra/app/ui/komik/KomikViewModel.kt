package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikApi
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
class KomikViewModel : ViewModel() {
    private val api = KomikApi.create()
    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")

    init {
        loadHome(page = 1)
        viewModelScope.launch {
            _searchFlow
                .debounce(450L)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isNotBlank()) {
                        performSearch(q)
                    } else {
                        loadHome(page = 1)
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

    fun loadHome(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = page == 1,
                error = null,
                activePage = page
            )
            try {
                val res = withContext(Dispatchers.IO) {
                    api.list(mapOf("page" to page.toString()))
                }
                val body = res.body()
                if (res.isSuccessful && body?.success == true) {
                    val incoming = body.items
                    val merged = if (page == 1) incoming else (_uiState.value.items + incoming)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = merged,
                        activePage = page,
                        hasNext = body.hasNext == true,
                        error = if (merged.isEmpty()) "Data komik kosong" else null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat komik (HTTP ${res.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat komik"
                )
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.hasNext && !state.isLoading && state.items.isNotEmpty()) {
            loadHome(page = state.activePage + 1)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true, error = null)
        try {
            val res = withContext(Dispatchers.IO) { api.list(mapOf("q" to query)) }
            val body = res.body()
            if (res.isSuccessful && body?.success == true) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = body.items,
                    error = if (body.items.isEmpty()) "Komik \"$query\" tidak ditemukan" else null
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

    fun loadDetail(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(detailError = "Slug komik tidak valid")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetailLoading = true, detailError = null)
            try {
                // Detail endpoint returns the same daftar-manga list; match by slug.
                val res = withContext(Dispatchers.IO) { api.list(mapOf("slug" to slug)) }
                val body = res.body()
                val matched = body?.items?.firstOrNull { it.resolvedSlug == slug }
                if (res.isSuccessful && body?.success == true && matched != null) {
                    _uiState.value = _uiState.value.copy(
                        isDetailLoading = false,
                        komik = matched,
                        chapters = emptyList(),
                        detailError = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDetailLoading = false,
                        detailError = "Komik tidak ditemukan (HTTP ${res.code()})"
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

    fun clearDetail() {
        _uiState.value = _uiState.value.copy(komik = null, chapters = emptyList(), detailError = null)
    }
}

data class KomikUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val activePage: Int = 1,
    val hasNext: Boolean = false,
    val items: List<KomikItem> = emptyList(),
    val searchResults: List<KomikItem> = emptyList(),
    val komik: KomikItem? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val error: String? = null,
    val detailError: String? = null
)

data class KomikChapter(
    val title: String? = null,
    val slug: String? = null,
    val link: String? = null,
    val date: String? = null
)
