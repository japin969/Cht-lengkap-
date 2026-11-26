package com.jayra.app.ui.comic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.BkChapterResponse
import com.jayra.app.data.BkDetailResponse
import com.jayra.app.data.BkItem
import com.jayra.app.data.BkListResponse
import com.jayra.app.data.ComicApi
import com.jayra.app.data.MaidItem
import com.jayra.app.data.MaidMangaResponse
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
    val bkLatest: List<KomikItem> = emptyList(),
    val bkPopuler: List<KomikItem> = emptyList(),
    val maidLatest: List<KomikItem> = emptyList(),
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


    // ── BACA KOMIK (sankavollerei) ──
    fun loadBacakomik() {
        viewModelScope.launch {
            val latest = runCatching { api.bkLatest().body()?.komikList.orEmpty().map { it.toItem() } }.getOrDefault(emptyList())
            val populer = runCatching { api.bkPopuler().body()?.komikList.orEmpty().map { it.toItem() } }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(bkLatest = latest, bkPopuler = populer)
        }
    }

    // ── MAID (sankavollerei) ──
    fun loadMaid() {
        viewModelScope.launch {
            val items = runCatching { api.maidLatest(1).body()?.data.orEmpty().map { it.toItem() } }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(maidLatest = items)
        }
    }

    // ── BACA KOMIK detail/reader ──
    fun loadBkDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.bkDetail(slug)
                val d = res.body()?.detail
                if (res.isSuccessful && d != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = KomikItem(title = d.title, slug = slug, cover = d.cover, updatedAt = d.status, rating = d.rating),
                        synopsis = d.synopsis.orEmpty(),
                        chapters = d.chapters.orEmpty().map { KomikChapter(title = it.title, slug = it.slug, link = it.slug) },
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

    fun loadBkChapter(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, pages = emptyList())
            try {
                val res = api.bkChapter(slug)
                val imgs = res.body()?.images.orEmpty()
                val pages = imgs.mapIndexed { i, u -> KomikPage(page = i + 1, url = u) }
                _uiState.value = _uiState.value.copy(
                    isLoading = false, pages = pages,
                    error = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memuat chapter")
            }
        }
    }

    // ── MAID detail/reader ──
    fun loadMaidManga(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.maidManga(slug)
                val m = res.body()?.data
                if (res.isSuccessful && m != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = KomikItem(title = m.title, slug = slug, cover = m.banner ?: m.banner, updatedAt = m.status, rating = m.rating),
                        synopsis = m.description.orEmpty(),
                        chapters = m.chapters.orEmpty().map { KomikChapter(title = it.title, slug = it.slug, link = it.slug) },
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

    fun loadMaidChapter(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, pages = emptyList())
            try {
                val res = api.maidChapter(slug)
                val imgs = res.body()?.images.orEmpty()
                val pages = imgs.mapIndexed { i, u -> KomikPage(page = i + 1, url = u) }
                _uiState.value = _uiState.value.copy(
                    isLoading = false, pages = pages,
                    error = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memuat chapter")
            }
        }
    }

    private fun BkItem.toItem(): KomikItem = KomikItem(title = title, slug = slug, cover = cover.sanitizeCover(), updatedAt = chapter, rating = null)
    private fun MaidItem.toItem(): KomikItem = KomikItem(title = title, slug = slug, cover = banner.sanitizeCover(), updatedAt = latest_chapter?.title, rating = rating)

    /** Buang cover yang bukan URL gambar valid (data: URI placeholder / kosong) -> null agar MediaPoster tampilkan placeholder. */
    private fun String?.sanitizeCover(): String? = this?.takeIf { it.startsWith("http") }

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
        cover = thumbnail.sanitizeCover(),
        updatedAt = description,
        rating = null
    )

    private fun SankaComicGenreItem.toItem(): KomikItem = KomikItem(
        title = title,
        slug = link?.trimEnd('/')?.substringAfterLast('/'),
        cover = image.sanitizeCover(),
        updatedAt = chapter,
        rating = rating
    )
}