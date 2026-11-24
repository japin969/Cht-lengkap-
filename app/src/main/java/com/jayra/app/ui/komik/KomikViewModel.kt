package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikDetailResponse
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikPage
import com.jayra.app.data.KomikHomeStore
import com.jayra.app.data.NexadevApi
import com.jayra.app.ui.media.mergeDedupe
import com.jayra.app.ui.media.normalizeKey
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
    val sources: List<KomikSourceResult> = emptyList()
)

/**
 * ViewModel Komik — SEMENTARA pakai NexaDev saja (stabil).
 * SankavollereiApi di-nonaktifkan; akan ditambah balik satu-satu setelah
 * disetujui. Kontrak UI (state + fungsi) dipertahankan.
 */
class KomikViewModel : ViewModel() {
    private val nexadev = NexadevApi.create()

    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    private val searchCache = LinkedHashMap<String, List<KomikItem>>()
    private val detailCache = LinkedHashMap<String, KomikDetailResponse>()
    private val chapterCache = LinkedHashMap<String, List<KomikChapter>>()
    private val chapterPagesCache = LinkedHashMap<String, List<KomikPage>>()
    private var lastSlug: String? = null
    private var homeLoaded = false

    init {
        val mem = HomeCache.state
        val disk = if (mem == null) komikDiskToState(KomikHomeStore.load()) else null
        (mem ?: disk)?.let { c -> _uiState.value = c.copy(isLoading = true) }
        loadHome()
    }

    private fun komikDiskToState(items: List<KomikHomeStore.Item>?): KomikUiState? {
        if (items == null) return null
        val mapped = items.map {
            KomikItem(
                title = it.title, link = it.link, slug = it.slug, cover = it.cover,
                type = it.type, latestChapter = it.latestChapter, rating = it.rating
            )
        }
        return KomikUiState(latest = mapped, popular = mapped)
    }

    fun loadHome(force: Boolean = false) {
        if (homeLoaded && !force) return
        homeLoaded = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            val result = fetchNexadevList()
            mergeHome(result)
        }
    }

    private suspend fun fetchNexadevList(): KomikSourceResult {
        return try {
            val items: List<KomikItem> = coroutineScope {
                (1..2).map { p ->
                    async { runCatching { nexadev.getKomikList(page = p).body()?.results }.getOrDefault(emptyList()).orEmpty() }
                }.flatMap { it.await() }.distinctBy { it.slug ?: it.title }
            }
            KomikSourceResult(
                source = "nexadev",
                items = items,
                error = if (items.isEmpty()) "kosong" else null
            )
        } catch (e: Exception) {
            KomikSourceResult("nexadev", emptyList(), e.localizedMessage ?: "gagal")
        }
    }

    private fun mergeHome(result: KomikSourceResult) {
        val all = mergeDedupe(
            lists = listOf(result.items),
            keyOf = { normalizeKey(it.title) },
            scoreOf = { if (it.cover.isNullOrBlank()) 0 else 1 }
        )
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            latest = all,
            popular = all,
            sources = listOf(result),
            homeError = if (all.isNotEmpty()) null
                else result.error?.takeIf { it != "kosong" } ?: "Gagal memuat daftar komik"
        )
        HomeCache.state = _uiState.value.copy(isLoading = false)
        KomikHomeStore.save(_uiState.value.latest.map {
            KomikHomeStore.Item(
                title = it.title, link = it.link, slug = it.slug, cover = it.cover,
                type = it.type, latestChapter = it.latestChapter, rating = it.rating
            )
        })
    }

    object HomeCache {
        @Volatile var state: KomikUiState? = null
    }

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

    suspend fun search(query: String, force: Boolean = false): List<KomikItem> {
        if (query.isBlank()) return emptyList()
        val key = query.trim().lowercase()
        if (!force) searchCache[key]?.let { return it }
        val q = query.trim()
        val results = runCatching {
            nexadev.searchKomik(q).body()?.results
        }.getOrElse { emptyList() }.orEmpty()
        val merged = mergeDedupe(
            lists = listOf(results),
            keyOf = { normalizeKey(it.title) },
            scoreOf = { if (it.cover.isNullOrBlank()) 0 else 1 }
        )
        searchCache[key] = merged
        return merged
    }

    suspend fun searchFresh(query: String): List<KomikItem> {
        searchCache.remove(query.trim().lowercase())
        return search(query, force = true)
    }

    fun loadDetail(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Komik tidak ditemukan")
            return
        }
        lastSlug = slug
        detailCache[slug]?.let { cached ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                detail = cached,
                chapters = chapterCache[slug] ?: cached.chapters.orEmpty(),
                error = null
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val results = runCatching { nexadev.getKomikDetail(slug, "detail").body() }.getOrNull()
            if (results != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = results,
                    chapters = results.chapters.orEmpty(),
                    error = null
                )
                detailCache[slug] = results
                chapterCache[slug] = results.chapters.orEmpty()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Gagal memuat detail komik"
                )
            }
        }
    }

    fun retryDetail() { lastSlug?.let { loadDetail(it) } }

    fun loadStream(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Chapter tidak ditemukan")
            return
        }
        lastSlug = slug
        chapterPagesCache[slug]?.let { cached ->
            if (cached.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamTitle = _uiState.value.detail?.title,
                    pages = cached,
                    error = null
                )
                return
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val pages = runCatching {
                nexadev.getKomikPages(slug = slug, type = "stream").body()?.pages
            }.getOrNull().orEmpty()
            if (pages.isNotEmpty()) chapterPagesCache[slug] = pages
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                streamTitle = _uiState.value.detail?.title,
                pages = pages,
                error = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
            )
        }
    }

    fun retryStream() { lastSlug?.let { loadStream(it) } }
}