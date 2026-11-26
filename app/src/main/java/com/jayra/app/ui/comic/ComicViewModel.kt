package com.jayra.app.ui.comic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikPage
import com.jayra.app.data.MangaDexApi
import com.jayra.app.data.MdxChapter
import com.jayra.app.data.MdxManga
import com.jayra.app.data.MdxTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ComicUiState(
    val isLoading: Boolean = false,
    // Home
    val latest: List<KomikItem> = emptyList(),
    val popular: List<KomikItem> = emptyList(),
    val trending: List<KomikItem> = emptyList(),
    // List / Search / Genre
    val results: List<KomikItem> = emptyList(),
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val query: String = "",
    val activeTag: String? = null,
    val tags: List<MdxTag> = emptyList(),
    // Detail
    val detail: KomikItem? = null,
    val synopsis: String = "",
    val chapters: List<KomikChapter> = emptyList(),
    // Reader
    val streamTitle: String? = null,
    val pages: List<KomikPage> = emptyList(),
    val error: String? = null
)

class ComicViewModel : ViewModel() {

    private val api = MangaDexApi.create()
    private val _uiState = MutableStateFlow(ComicUiState())
    val uiState: StateFlow<ComicUiState> = _uiState

    private var listOffset = 0
    private val pageSize = 20

    init { loadHome() }

    // ── HOME ──
    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val latest = runCatching {
                    api.list(limit = 20, order = "latestUploadedChapter").body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val popular = runCatching {
                    api.list(limit = 20, order = "followedCount").body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val trending = runCatching {
                    api.list(limit = 20, order = "rating").body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val hasData = latest.isNotEmpty() || popular.isNotEmpty() || trending.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    latest = latest.map { it.toItem() },
                    popular = popular.map { it.toItem() },
                    trending = trending.map { it.toItem() },
                    error = if (hasData) null else "Gagal memuat daftar komik"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Koneksi bermasalah")
            }
        }
    }

    fun retryHome() = loadHome()

    // ── TAGS / GENRES ──
    fun loadTags() {
        if (_uiState.value.tags.isNotEmpty()) return
        viewModelScope.launch {
            val t = runCatching { api.tags().body()?.data.orEmpty() }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(tags = t)
        }
    }

    // ── LIST (genre / all) with pagination ──
    fun loadList(tagId: String? = null, reset: Boolean = true) {
        if (reset) listOffset = 0
        if (reset) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeTag = tagId)
        } else {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
        }
        viewModelScope.launch {
            try {
                val res = api.list(
                    limit = pageSize,
                    offset = listOffset,
                    order = "followedCount",
                    includedTags = if (tagId != null) listOf(tagId) else null
                )
                val items = res.body()?.data.orEmpty().map { it.toItem() }
                listOffset += pageSize
                val current = if (reset) emptyList() else _uiState.value.results
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    results = current + items,
                    canLoadMore = items.size == pageSize,
                    error = if (reset && items.isEmpty()) "Tidak ada komik" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, isLoadingMore = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun loadMore() {
        if (!_uiState.value.canLoadMore || _uiState.value.isLoadingMore || _uiState.value.isLoading) return
        loadList(tagId = _uiState.value.activeTag, reset = false)
    }

    // ── SEARCH (debounce dilakukan di UI) ──
    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false, error = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.search(title = query, limit = 30)
                val items = res.body()?.data.orEmpty().map { it.toItem() }
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

    fun retrySearch() { search(_uiState.value.query) }

    // ── DETAIL ──
    fun loadDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val detailRes = api.detail(slug)
                val manga = detailRes.body()?.data
                val chapterItems = runCatching {
                    api.chapters(slug).body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                if (detailRes.isSuccessful && manga != null) {
                    val item = manga.toItem()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = item,
                        synopsis = manga.attributes?.displayDescription().orEmpty(),
                        chapters = chapterItems.map { it.toChapter() },
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Gagal memuat detail komik")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memuat detail")
            }
        }
    }

    fun retryDetail(slug: String) = loadDetail(slug)

    // ── READER ──
    fun loadStream(chapterSlug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, pages = emptyList())
            try {
                val res = api.chapterImages(chapterSlug)
                val body = res.body()
                val base = body?.baseUrl
                val hash = body?.chapter?.hash
                val files = body?.chapter?.data.orEmpty()
                val pages = if (base != null && hash != null) {
                    files.mapIndexed { index, file -> KomikPage(page = index + 1, url = "$base/data/$hash/$file") }
                } else emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pages = pages,
                    streamTitle = _uiState.value.detail?.title,
                    error = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memuat chapter")
            }
        }
    }

    fun retryStream(chapterSlug: String) = loadStream(chapterSlug)

    private fun MdxManga.toItem(): KomikItem = KomikItem(
        title = attributes?.displayTitle(),
        slug = id,
        cover = coverUrl(),
        updatedAt = attributes?.lastChapter?.takeIf { it.isNotBlank() }?.let { "Ch. $it" }
            ?: attributes?.status,
        rating = null
    )

    private fun MdxChapter.toChapter(): KomikChapter {
        val number = attributes?.chapter?.takeIf { it.isNotBlank() }
        val title = attributes?.title?.takeIf { it.isNotBlank() }
        val label = when {
            number != null && title != null -> "Chapter $number · $title"
            number != null -> "Chapter $number"
            title != null -> title
            else -> "Chapter"
        }
        return KomikChapter(title = label, slug = id, link = id)
    }
}