package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikDetailResponse
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikPage
import com.jayra.app.data.NexadevApi
import com.jayra.app.data.SankavollereiApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Hasil satu sumber API komik. */
data class KomikSourceResult(
    val source: String,
    val items: List<KomikItem>,
    val error: String? = null
) {
    val ok: Boolean get() = error == null && items.isNotEmpty()
}

data class KomikUiState(
    val isLoading: Boolean = false,
    val latest: List<KomikItem> = emptyList(),
    val popular: List<KomikItem> = emptyList(),
    val detail: KomikDetailResponse? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val streamTitle: String? = null,
    val pages: List<KomikPage> = emptyList(),
    val error: String? = null,
    val detailError: String? = null,
    val detailLoading: Boolean = false,
    val homeError: String? = null,
    /** Status per sumber, dipakai untuk banner "sebagian sumber gagal". */
    val sources: List<KomikSourceResult> = emptyList()
)

class KomikViewModel : ViewModel() {
    // Sumber 1 (lama) — tetap dipakai.
    private val nexadev = NexadevApi.create()
    // Sumber 2 (baru) — ditambahkan di sampingnya.
    private val sankavollerei = SankavollereiApi.create()

    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    /** Cache search agar query yang sama tidak menembak API berulang. */
    private val searchCache = LinkedHashMap<String, List<KomikItem>>()
    private var lastSlug: String? = null
    private var homeLoaded = false

    init { loadHome() }

    fun loadHome(force: Boolean = false) {
        if (homeLoaded && !force) return
        homeLoaded = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            // Kedua sumber dipanggil PARALEL.
            val results = coroutineScope {
                val a = async { fetchNexadevList() }
                val b = async { fetchSankavollereiList() }
                listOf(a.await(), b.await())
            }
            mergeHome(results)
        }
    }

    private suspend fun fetchNexadevList(): KomikSourceResult {
        return try {
            val items = nexadev.getKomikList(page = 1).body()?.results.orEmpty()
            KomikSourceResult(
                source = "nexadev",
                items = items,
                error = if (items.isEmpty()) "kosong" else null
            )
        } catch (e: Exception) {
            KomikSourceResult("nexadev", emptyList(), e.localizedMessage ?: "gagal")
        }
    }

    private suspend fun fetchSankavollereiList(): KomikSourceResult {
        return try {
            val items = sankavollerei.comicLatest(page = 1, limit = 20)
                .body()?.allItems.orEmpty().map { it.toItem() }
            KomikSourceResult(
                source = "sankavollerei",
                items = items,
                error = if (items.isEmpty()) "kosong" else null
            )
        } catch (e: Exception) {
            KomikSourceResult("sankavollerei", emptyList(), e.localizedMessage ?: "gagal")
        }
    }

    private fun mergeHome(results: List<KomikSourceResult>) {
        val all = results.flatMap { it.items }
            .distinctBy { it.slug ?: it.title }

        val anyOk = results.any { it.ok }
        val msgs = results.filter { !it.ok }.mapNotNull { r ->
            r.error?.takeIf { it != "kosong" }?.let { "${r.source}: $it" }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            latest = all,
            popular = all,
            sources = results,
            homeError = if (anyOk || all.isNotEmpty()) null
            else msgs.firstOrNull() ?: "Gagal memuat daftar komik"
        )
    }

    /** Muat satu halaman daftar komik dari Nexadev. */
    fun loadPage(page: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            try {
                val items = nexadev.getKomikList(page = page).body()?.results.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    latest = items,
                    popular = items,
                    homeError = if (items.isEmpty()) "Daftar komik kosong" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    homeError = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun retry() {
        homeLoaded = false
        loadHome(force = true)
    }

    /**
     * Pencarian PARALEL dari kedua sumber, hasil digabung, duplikat dibuang.
     */
    suspend fun search(query: String, force: Boolean = false): List<KomikItem> {
        if (query.isBlank()) return emptyList()
        val key = query.trim().lowercase()
        if (!force) searchCache[key]?.let { return it }

        val results = coroutineScope {
            val a = async {
                runCatching {
                    nexadev.searchKomik(query.trim()).body()?.results.orEmpty()
                }.getOrElse { emptyList() }
            }
            val b = async {
                runCatching {
                    sankavollerei.comicSearch(query.trim(), page = 1, limit = 20)
                        .body()?.allItems.orEmpty().map { it.toItem() }
                }.getOrElse { emptyList() }
            }
            listOf(a.await(), b.await())
        }
        val merged = results.flatten().distinctBy { it.slug ?: it.title }
        searchCache[key] = merged
        return merged
    }

    /** Versi tanpa cache (tombol coba lagi). */
    suspend fun searchFresh(query: String): List<KomikItem> {
        searchCache.remove(query.trim().lowercase())
        return search(query, force = true)
    }

    fun loadDetail(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Komik tidak ditemukan"
            )
            return
        }
        lastSlug = slug
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val results = coroutineScope {
                val a = async {
                    runCatching { nexadev.getKomikDetail(slug = slug, type = "detail").body() }
                        .getOrNull()
                }
                val b = async {
                    runCatching {
                        sankavollerei.comicDetail(slug = slug).body()?.data
                            ?.let { KomikDetailResponse(success = true, title = it.title, cover = it.bestCover, synopsis = it.plainSynopsis, chapters = it.allChapters.map { c -> KomikChapter(title = c.title, link = c.link ?: c.href, slug = c.slug) }) }
                    }.getOrNull()
                }
                listOfNotNull(a.await(), b.await())
            }
            if (results.isNotEmpty()) {
                val first = results.first()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = first,
                    chapters = first.chapters.orEmpty(),
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Gagal memuat detail komik"
                )
            }
        }
    }

    fun retryDetail() {
        lastSlug?.let { loadDetail(it) }
    }

    fun loadStream(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Chapter tidak ditemukan"
            )
            return
        }
        lastSlug = slug
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Coba Nexadev dulu, lalu Sankavollerei.
            val nexadevResult = runCatching {
                nexadev.getKomikPages(slug = slug, type = "stream").body()
            }.getOrNull()
            if (!nexadevResult?.pages.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamTitle = nexadevResult?.chapterTitle
                        ?: nexadevResult?.mangaTitle
                        ?: _uiState.value.detail?.title,
                    pages = nexadevResult?.pages.orEmpty(),
                    error = null
                )
                return@launch
            }

            val sankaku = runCatching {
                sankavollerei.comicChapter(segment = slug).body()?.data
            }.getOrNull()
            val pages = sankaku?.allPages.orEmpty().mapIndexed { i, url ->
                KomikPage(page = i + 1, url = url)
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                streamTitle = sankaku?.title
                    ?: sankaku?.chapter
                    ?: _uiState.value.detail?.title,
                pages = pages,
                error = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
            )
        }
    }

    fun retryStream() {
        lastSlug?.let { loadStream(it) }
    }

    private fun com.jayra.app.data.SankavollereiComicItem.toItem(): KomikItem = KomikItem(
        title = title,
        link = link ?: href,
        slug = slug,
        cover = bestCover,
        type = type,
        latestChapter = latestChapter,
        rating = rating
    )
}