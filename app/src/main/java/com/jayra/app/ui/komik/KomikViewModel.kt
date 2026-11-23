package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikDetailResponse
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikPage
import com.jayra.app.data.NexadevApi
import com.jayra.app.data.SankavollereiApi
import com.jayra.app.data.SankavollereiComicItem
import com.jayra.app.data.SankavollereiComicListResponse
import com.jayra.app.ui.media.mergeDedupe
import com.jayra.app.ui.media.normalizeKey
import com.jayra.app.data.SankavollereiComicDetailResponse2
import com.jayra.app.data.SankavollereiComicChapterResponse2
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
    /** Cache detail & chapter agar buka lagi langsung muncul (tidak loading ulang). */
    private val detailCache = LinkedHashMap<String, KomikDetailResponse>()
    private val chapterCache = LinkedHashMap<String, List<KomikChapter>>()
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

    /** Cache kategori terakhir (dipakai mergeHome). */
    private var _nexadevLatest: List<KomikItem> = emptyList()
    private var _sankaLatest: List<KomikItem> = emptyList()
    private var _sankaPopular: List<KomikItem> = emptyList()

    private suspend fun fetchNexadevList(): KomikSourceResult {
        return try {
            // Beberapa halaman pertama (paralel) supaya daftar terisi.
            val items: List<KomikItem> = coroutineScope {
                (1..3).map { p ->
                    async {
                        runCatching { nexadev.getKomikList(page = p).body()?.results }
                            .getOrDefault(emptyList()).orEmpty()
                    }
                }.flatMap { it.await() }.distinctBy { it.slug ?: it.title }
            }
            _nexadevLatest = items
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
        // Ambil SEMUA source komik Sankavollerei secara PARALEL, dipisah
        // latest vs popular supaya Home punya section berbeda & terisi.
        val latestLists: List<List<KomikItem>>
        val popularLists: List<List<KomikItem>>
        coroutineScope {
            val latest = listOf(
                // komikindo punya 228 halaman -> ambil 3 halaman pertama (paralel).
                async { safeKomikList { sankavollerei.comicLatest(page = 1, limit = 20) } },
                async { safeKomikList { sankavollerei.komikindoLatest(1) } },
                async { safeKomikList { sankavollerei.komikindoLatest(2) } },
                async { safeKomikList { sankavollerei.komikindoLatest(3) } },
                async { safeKomikList { sankavollerei.bacakomikLatest() } },
                async { safeKomikList { sankavollerei.shinigamiLatest() } },
                async { safeKomikList { sankavollerei.mangasusukuLatest() } },
                async { safeKomikList { sankavollerei.kiryuuLatest() } },
                async { safeKomikList { sankavollerei.komikstationHome() } },
                async { safeKomikList { sankavollerei.westmangaLatest() } },
                async { safeKomikList { sankavollerei.softkomikList() } },
                async { safeKomikList { sankavollerei.mangakitaList() } },
                async { safeKomikList { sankavollerei.maidHome() } }
            )
            val popular = listOf(
                async { safeKomikList { sankavollerei.comicPopular(page = 1, limit = 20) } },
                async { safeKomikList { sankavollerei.komikindoPopular(1) } },
                async { safeKomikList { sankavollerei.bacakomikPopular() } },
                async { safeKomikList { sankavollerei.kiryuuPopular() } },
                async { safeKomikList { sankavollerei.komikstationPopular() } }
            )
            latestLists = latest.map { it.await() }
            popularLists = popular.map { it.await() }
        }
        _sankaLatest = mergeDedupe(
            lists = latestLists,
            keyOf = { normalizeKey(it.title) },
            scoreOf = { if (it.cover.isNullOrBlank()) 0 else 1 }
        )
        _sankaPopular = mergeDedupe(
            lists = popularLists,
            keyOf = { normalizeKey(it.title) },
            scoreOf = { if (it.cover.isNullOrBlank()) 0 else 1 }
        )
        val items = mergeDedupe(
            lists = listOf(_sankaLatest, _sankaPopular),
            keyOf = { normalizeKey(it.title) },
            scoreOf = { if (it.cover.isNullOrBlank()) 0 else 1 }
        )
        return KomikSourceResult(
            source = "sankavollerei",
            items = items,
            error = if (items.isEmpty()) "kosong" else null
        )
    }

    private suspend fun safeKomikList(
        block: suspend () -> retrofit2.Response<*>
    ): List<KomikItem> {
        return try {
            val body = block().body()
            when (body) {
                is SankavollereiComicListResponse -> body.allItems.map { it.toItem() }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mergeHome(results: List<KomikSourceResult>) {
        fun merge(vararg lists: List<KomikItem>): List<KomikItem> =
            mergeDedupe(
                lists = lists.toList(),
                keyOf = { normalizeKey(it.title) },
                scoreOf = { if (it.cover.isNullOrBlank()) 0 else 1 }
            )

        val all = merge(_nexadevLatest, _sankaLatest, _sankaPopular)
        val latest = merge(_nexadevLatest, _sankaLatest).ifEmpty { all }
        val popular = merge(_sankaPopular, _sankaLatest, _nexadevLatest).ifEmpty { all }

        val anyOk = results.any { it.ok }
        val msgs = results.filter { !it.ok }.mapNotNull { r ->
            r.error?.takeIf { it != "kosong" }?.let { "${r.source}: $it" }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            latest = latest,
            popular = popular,
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

        val q = query.trim()
        // Semua sumber pencarian dipanggil PARALEL.
        val results: List<List<KomikItem>> = coroutineScope {
            val a = async {
                safeNexadevSearch { nexadev.searchKomik(q).body()?.results }
            }
            val b = async {
                safeSankaSearch { sankavollerei.comicSearch(q, page = 1, limit = 20).body()?.allItems }
            }
            val c = async {
                safeSankaSearch { sankavollerei.komikindoSearch(q, 1).body()?.allItems }
            }
            val d = async {
                safeSankaSearch { sankavollerei.bacakomikSearch(q).body()?.allItems }
            }
            val e = async {
                safeSankaSearch { sankavollerei.kiryuuSearch(q).body()?.allItems }
            }
            val f = async {
                safeSankaSearch { sankavollerei.comicAdvancedSearch(q).body()?.allItems }
            }
            listOf(a.await(), b.await(), c.await(), d.await(), e.await(), f.await())
        }
        val merged = mergeDedupe(
            lists = results,
            keyOf = { normalizeKey(it.title) },
            scoreOf = { if (it.cover.isNullOrBlank()) 0 else 1 }
        )
        searchCache[key] = merged
        return merged
    }

    private suspend fun safeNexadevSearch(
        block: suspend () -> List<KomikItem>?
    ): List<KomikItem> {
        return try {
            block().orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun safeSankaSearch(
        block: suspend () -> List<SankavollereiComicItem>?
    ): List<KomikItem> {
        return try {
            block().orEmpty().map { it.toItem() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun List<SankavollereiComicItem>.toKomikItems(): List<KomikItem> =
        map { it.toItem() }

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
        // Buka lagi -> langsung dari cache.
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
            // Semua sumber detail dipanggil PARALEL.
            val results: List<KomikDetailResponse> = coroutineScope {
                val a = async { safeNexadevDetail { nexadev.getKomikDetail(slug, "detail").body() } }
                val b = async { safeSankaDetail { sankavollerei.comicDetail(slug).body() } }
                val c = async { safeSankaDetail { sankavollerei.komikindoDetail(slug).body() } }
                val d = async { safeSankaDetail { sankavollerei.bacakomikDetail(slug).body() } }
                val e = async { safeSankaDetail { sankavollerei.kiryuuDetail(slug).body() } }
                val f = async { safeSankaDetail { sankavollerei.komikstationDetail(slug).body() } }
                listOfNotNull(a.await(), b.await(), c.await(), d.await(), e.await(), f.await())
            }
            if (results.isNotEmpty()) {
                // Pilih kandidat dengan chapter terbanyak (detail paling lengkap).
                val first = results.maxByOrNull { it.chapters?.size ?: 0 } ?: results.first()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = first,
                    chapters = first.chapters.orEmpty(),
                    error = null
                )
                detailCache[slug] = _uiState.value.detail ?: first
                chapterCache[slug] = first.chapters.orEmpty()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Detail komik tidak ditemukan di semua sumber. Coba lagi."
                )
            }
        }
    }

    private suspend fun safeNexadevDetail(
        block: suspend () -> KomikDetailResponse?
    ): KomikDetailResponse? {
        return try { block() } catch (e: Exception) { null }
    }

    /**
     * Konversi response Sankavollerei (bentuk flat ATAU nested `data`)
     * ke model yang dipakai UI.
     */
    private suspend fun safeSankaDetail(
        block: suspend () -> SankavollereiComicDetailResponse2?
    ): KomikDetailResponse? {
        return try {
            val body = block() ?: return null
            val d = body.data
            val chapters = body.allChapters.map { ch ->
                KomikChapter(
                    title = ch.label,
                    link = ch.link ?: ch.href,
                    slug = ch.slug
                )
            }
            KomikDetailResponse(
                success = true,
                title = body.plainTitle,
                cover = body.bestCover,
                synopsis = body.plainSynopsis,
                chapters = chapters
            )
        } catch (e: Exception) {
            null
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
        // Cache chapter -> buka lagi langsung tampil.
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
            // Semua sumber chapter dipanggil PARALEL; sumber pertama yang
            // punya halaman gambar yang dipakai. Mangakita/maid/softkomik/
            // westmanga/shinigami/mangasusuku juga dicoba.
            val pages: List<KomikPage> = coroutineScope {
                val a = async { safeNexadevPages(slug) }
                val b = async { safeSankaPages { sankavollerei.comicChapter(slug).body() } }
                val c = async { safeSankaPages { sankavollerei.komikindoChapter(slug).body() } }
                val d = async { safeSankaPages { sankavollerei.bacakomikChapter(slug).body() } }
                val e = async { safeSankaPages { sankavollerei.kiryuuChapter(slug).body() } }
                val f = async { safeSankaPages { sankavollerei.komikstationChapter(slug).body() } }
                val g = async { safeSankaPages { sankavollerei.mangasusukuChapter(slug).body() } }
                val h = async { safeSankaPages { sankavollerei.maidChapter(slug).body() } }
                val i = async { safeSankaPages { sankavollerei.softkomikChapter(slug, "1").body() } }
                val j = async { safeSankaPages { sankavollerei.westmangaChapter(slug).body() } }
                val k = async { safeSankaPages { sankavollerei.shinigamiChapter(slug).body() } }
                val l = async { safeSankaPages { sankavollerei.mangakitaChapter(slug).body() } }
                // Fallback: beberapa source (mangakita/kiryuu) butuh NOMOR
                // chapter, bukan slug. Coba dengan angka yang diekstrak.
                val num = extractChapterNumber(slug)
                val m = async {
                    if (num != null && num > 0) safeSankaPages { sankavollerei.mangakitaChapter(num.toString()).body() }
                    else emptyList()
                }
                listOf(a.await(), b.await(), c.await(), d.await(), e.await(), f.await(),
                    g.await(), h.await(), i.await(), j.await(), k.await(), l.await(), m.await())
                    .firstOrNull { it.isNotEmpty() } ?: emptyList()
            }
            if (pages.isNotEmpty()) chapterPagesCache[slug] = pages
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                streamTitle = _uiState.value.detail?.title,
                pages = pages,
                error = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
            )
        }
    }

    /** Cache halaman chapter (biar buka lagi instan). */
    private val chapterPagesCache = LinkedHashMap<String, List<KomikPage>>()

    /** Ekstrak nomor chapter dari slug (mis. chapter-1.438774 -> 1). */
    private fun extractChapterNumber(slug: String): Int? {
        // Ambil segmen setelah 'chapter' terakhir, lalu angka pertama.
        val after = slug.substringAfterLast("chapter").trim('-', '.', ' ')
            .ifBlank { slug.substringAfterLast('-') }
        val digits = after.filter { it.isDigit() }
        return digits.toIntOrNull()
    }

    private suspend fun safeNexadevPages(slug: String): List<KomikPage> {
        return try {
            val body = nexadev.getKomikPages(slug = slug, type = "stream").body()
            body?.pages.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Konversi response chapter Sankavollerei (flat atau nested) ke halaman. */
    private suspend fun safeSankaPages(
        block: suspend () -> SankavollereiComicChapterResponse2?
    ): List<KomikPage> {
        return try {
            val body = block() ?: return emptyList()
            body.allImages.mapIndexed { i, url ->
                KomikPage(page = i + 1, url = url)
            }
        } catch (e: Exception) {
            emptyList()
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
        latestChapter = latestChapterText,
        rating = rating
    )
}