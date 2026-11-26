package com.jayra.app.ui.comic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.ComicApi
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikPage
import com.jayra.app.data.SankaComicGenreItem
import com.jayra.app.data.SankaComicItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ComicUiState(
    val isLoading: Boolean = false,
    val latest: List<KomikItem> = emptyList(),
    val popular: List<KomikItem> = emptyList(),
    val trending: List<KomikItem> = emptyList(),
    val results: List<KomikItem> = emptyList(),
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val query: String = "",
    val activeTag: String? = null,
    val tags: List<com.jayra.app.data.MdxTag> = emptyList(),
    val detail: KomikItem? = null,
    val synopsis: String = "",
    val chapters: List<KomikChapter> = emptyList(),
    val streamTitle: String? = null,
    val pages: List<KomikPage> = emptyList(),
    val error: String? = null
)

/**
 * ComicViewModel — sumber resmi: api.nexadev.my.id (komik) + www.sankavollerei.web.id (comic).
 * Detail/stream komik di kedua host sedang bermasalah → tampilkan error jelas, tidak crash.
 */
class ComicViewModel : ViewModel() {

    private val api = ComicApi.create()
    private val _uiState = MutableStateFlow(ComicUiState())
    val uiState: StateFlow<ComicUiState> = _uiState

    private var listPage = 1

    init { loadHome() }

    // ── HOME ──
    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            var hadError: String? = null
            val latest = runCatching {
                api.listNexadev(page = 1).body()?.results.orEmpty()
            }.onFailure { hadError = it.localizedMessage }.getOrDefault(emptyList())
            val popular = runCatching {
                api.listNexadev(page = 2).body()?.results.orEmpty()
            }.onFailure { hadError = it.localizedMessage }.getOrDefault(emptyList())
            val trending = runCatching {
                api.genreSanka("action").body()?.comics.orEmpty().map { it.toItem() }
            }.onFailure { hadError = it.localizedMessage }.getOrDefault(emptyList())

            val hasData = latest.isNotEmpty() || popular.isNotEmpty() || trending.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                latest = latest,
                popular = popular,
                trending = trending,
                error = if (hasData) null else (hadError?.let { "Gagal memuat daftar komik" } ?: null)
            )
        }
    }

    fun retryHome() = loadHome()

    // ── TAGS / GENRES (statis, tidak dipakai endpoint eksternal) ──
    fun loadTags() {
        if (_uiState.value.tags.isNotEmpty()) return
        _uiState.value = _uiState.value.copy(tags = emptyList())
    }

    // ── LIST (genre / all) ──
    fun loadList(tagId: String? = null, reset: Boolean = true) {
        if (reset) listPage = 1
        if (reset) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeTag = tagId)
        } else {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
        }
        viewModelScope.launch {
            try {
                val items: List<KomikItem> = if (tagId != null) {
                    api.genreSanka(tagId).body()?.comics.orEmpty().map { it.toItem() }
                } else {
                    api.listNexadev(page = listPage).body()?.results.orEmpty()
                }
                listPage++
                val current = if (reset) emptyList() else _uiState.value.results
                _uiState.value = _uiState.value.copy(
                    isLoading = false, isLoadingMore = false,
                    results = current + items,
                    canLoadMore = tagId == null && items.isNotEmpty(),
                    error = if (reset && items.isEmpty()) "Tidak ada komik" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun loadMore() {
        if (!_uiState.value.canLoadMore || _uiState.value.isLoadingMore || _uiState.value.isLoading) return
        if (_uiState.value.activeTag != null) return
        loadList(tagId = null, reset = false)
    }

    // ── SEARCH (debounce di UI). Coba sankavollerei dulu, fallback nexadev. ──
    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false, error = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val sanka = runCatching { api.searchSanka(query).body()?.data.orEmpty() }.getOrDefault(emptyList())
                val items = if (sanka.isNotEmpty()) {
                    sanka.map { it.toItem() }
                } else {
                    api.searchNexadev(query).body()?.results.orEmpty()
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = items,
                    error = if (items.isEmpty()) "Tidak ada hasil" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun retrySearch() = search(_uiState.value.query)

    // ── DETAIL (upstream bermasalah → error jelas) ──
    fun loadDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Kedua host detail komik sedang 500/403. Tampilkan error jelas.
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Detail komik belum tersedia dari sumber saat ini."
            )
        }
    }

    fun retryDetail(slug: String) = loadDetail(slug)

    // ── READER (upstream bermasalah → error jelas) ──
    fun loadStream(chapterSlug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, pages = emptyList())
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Pembaca komik belum tersedia dari sumber saat ini."
            )
        }
    }

    fun retryStream(chapterSlug: String) = loadStream(chapterSlug)

    private fun SankaComicItem.toItem(): KomikItem = KomikItem(
        title = title,
        slug = slug,
        cover = thumbnail,
        updatedAt = description,
        rating = null
    )

    private fun SankaComicGenreItem.toItem(): KomikItem = KomikItem(
        title = title,
        slug = link?.trimEnd('/')?.substringAfterLast('/'),
        cover = image,
        updatedAt = chapter,
        rating = rating
    )
}