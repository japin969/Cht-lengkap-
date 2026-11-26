package com.jayra.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * KomikRepository - Parallel fetch from multiple manga sources.
 * Sources: MangaDex (primary) + KomikApi (legacy) + future sources
 */
class KomikRepository : BaseRepository<KomikItem>() {

    private val mangadexApi = MangaDexApi.create()
    private val legacyApi = KomikApi.create()

    override val cacheTtlMs = 10 * 60 * 1000 // 10 minutes

    // ═══════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════

    /** Fetch latest manga from all sources in parallel */
    suspend fun fetchLatest(): RepositoryResult<KomikItem> {
        return fetchParallel(
            "komik_latest",
            FetchSource("mangadex") { fetchMangaDexLatest() },
            FetchSource("legacy") { fetchLegacyLatest() }
        )
    }

    /** Search manga from all sources in parallel */
    suspend fun search(query: String): RepositoryResult<KomikItem> {
        return fetchParallel(
            "komik_search_$query",
            FetchSource("mangadex") { fetchMangaDexSearch(query) },
            FetchSource("legacy") { fetchLegacySearch(query) }
        )
    }

    /** Fetch manga detail from all sources in parallel, merge results */
    suspend fun fetchDetail(slug: String): RepositoryResult<KomikDetail> {
        return fetchParallel(
            "komik_detail_$slug",
            FetchSource("mangadex") { fetchMangaDexDetail(slug) },
            FetchSource("legacy") { fetchLegacyDetail(slug) }
        )
    }

    /** Fetch chapter list from all sources in parallel */
    suspend fun fetchChapters(slug: String): RepositoryResult<KomikChapter> {
        return fetchParallel(
            "komik_chapters_$slug",
            FetchSource("mangadex") { fetchMangaDexChapters(slug) },
            FetchSource("legacy") { fetchLegacyChapters(slug) }
        )
    }

    /** Fetch chapter images from all sources in parallel */
    suspend fun fetchChapterImages(chapterSlug: String): RepositoryResult<KomikPage> {
        return fetchParallel(
            "komik_images_$chapterSlug",
            FetchSource("mangadex") { fetchMangaDexChapterImages(chapterSlug) },
            FetchSource("legacy") { fetchLegacyStream(chapterSlug) }
        )
    }

    // ═══════════════════════════════════════════
    // MANGADEX API IMPLEMENTATIONS
    // ═══════════════════════════════════════════

    private suspend fun fetchMangaDexLatest(): List<KomikItem> {
        val response = mangadexApi.list(
            limit = 30,
            includes = listOf("cover_art"),
            order = "desc",
            ratings = listOf("safe", "suggestive", "erotica", "pornographic")
        )
        val data = response.body()?.data.orEmpty()
        return data.map { it.toKomikItem() }
    }

    private suspend fun fetchMangaDexSearch(query: String): List<KomikItem> {
        val response = mangadexApi.search(
            title = query,
            limit = 30,
            includes = listOf("cover_art")
        )
        val data = response.body()?.data.orEmpty()
        return data.map { it.toKomikItem() }
    }

    private suspend fun fetchMangaDexDetail(slug: String): List<KomikDetail> {
        val detailRes = mangadexApi.detail(slug, includes = listOf("cover_art"))
        val manga = detailRes.body()?.data
        val chapterRes = mangadexApi.chapters(
            id = slug,
            limit = 500,
            languages = listOf("id", "en"),
            order = "asc"
        )
        val chapters = chapterRes.body()?.data.orEmpty()

        return if (manga != null) {
            listOf(manga.toKomikDetail(chapters.map { it.toKomikChapter() }))
        } else emptyList()
    }

    private suspend fun fetchMangaDexChapters(slug: String): List<KomikChapter> {
        val response = mangadexApi.chapters(
            id = slug,
            limit = 500,
            languages = listOf("id", "en"),
            order = "asc"
        )
        val data = response.body()?.data.orEmpty()
        return data.map { it.toKomikChapter() }
    }

    private suspend fun fetchMangaDexChapterImages(chapterSlug: String): List<KomikPage> {
        val response = mangadexApi.chapterImages(chapterSlug)
        val body = response.body()
        val base = body?.baseUrl
        val hash = body?.chapter?.hash
        val files = body?.chapter?.data.orEmpty()

        return if (base != null && hash != null) {
            files.mapIndexed { index, file ->
                KomikPage(page = index + 1, url = "$base/data/$hash/$file")
            }
        } else emptyList()
    }

    // ═══════════════════════════════════════════
    // LEGACY API IMPLEMENTATIONS
    // ═══════════════════════════════════════════

    private suspend fun fetchLegacyLatest(): List<KomikItem> {
        val response = legacyApi.update(page = 1)
        val data = response.body()?.results.orEmpty()
        return data.map { it.toKomikItem() }
    }

    private suspend fun fetchLegacySearch(query: String): List<KomikItem> {
        val response = legacyApi.search(q = query)
        val data = response.body()?.results.orEmpty()
        return data.map { it.toKomikItem() }
    }

    private suspend fun fetchLegacyDetail(slug: String): List<KomikDetail> {
        val response = legacyApi.detail(slug = slug)
        val data = response.body()
        return if (data != null) listOf(data.toKomikDetail()) else emptyList()
    }

    private suspend fun fetchLegacyChapters(slug: String): List<KomikChapter> {
        val response = legacyApi.detail(slug = slug)
        val chapters = response.body()?.chapters.orEmpty()
        return chapters.map { it.toKomikChapter() }
    }

    private suspend fun fetchLegacyStream(slug: String): List<KomikPage> {
        val response = legacyApi.stream(slug = slug)
        val pages = response.body()?.pages.orEmpty()
        return pages.map { it.toKomikPage() }
    }

    // ═══════════════════════════════════════════
    // DEDUPLICATION & MERGE
    // ═══════════════════════════════════════════

    override fun getId(item: KomikItem): String = item.slug ?: item.link ?: item.title ?: ""

    override fun mergeDuplicates(items: List<KomikItem>): List<KomikItem> {
        val byId = mutableMapOf<String, KomikItem>()
        items.forEach { item ->
            val id = getId(item)
            if (id.isBlank()) {
                byId[item.title ?: "unknown_${System.currentTimeMillis()}"] = item
            } else {
                val existing = byId[id]
                byId[id] = if (existing == null) item else mergeKomikItem(existing, item)
            }
        }
        return byId.values.toList()
    }

    private fun mergeKomikItem(a: KomikItem, b: KomikItem): KomikItem {
        // Prefer non-null fields, prefer MangaDex cover (higher quality)
        val cover = chooseBestCover(a.cover, b.cover)
        return a.copy(
            cover = cover,
            updatedAt = a.updatedAt?.takeIf { it.isNotBlank() } ?: b.updatedAt,
            rating = a.rating?.takeIf { it.isNotBlank() } ?: b.rating
        )
    }

    private fun chooseBestCover(c1: String?, c2: String?): String? {
        val valid1 = c1?.takeIf { it.isNotBlank() && it.startsWith("http") }
        val valid2 = c2?.takeIf { it.isNotBlank() && it.startsWith("http") }
        // Prefer MangaDex (uploads.mangadex.org) over others
        return when {
            valid1?.contains("mangadex") == true -> valid1
            valid2?.contains("mangadex") == true -> valid2
            else -> valid1 ?: valid2
        }
    }

    override fun getId(item: KomikDetail): String = item.slug ?: item.title ?: ""

    override fun mergeDuplicates(items: List<KomikDetail>): List<KomikDetail> {
        val byId = mutableMapOf<String, KomikDetail>()
        items.forEach { item ->
            val id = getId(item)
            val existing = byId[id]
            byId[id] = if (existing == null) item else mergeKomikDetail(existing, item)
        }
        return byId.values.toList()
    }

    private fun mergeKomikDetail(a: KomikDetail, b: KomikDetail): KomikDetail {
        return a.copy(
            cover = chooseBestCover(a.cover, b.cover),
            synopsis = a.synopsis?.takeIf { it.isNotBlank() } ?: b.synopsis,
            chapters = mergeChapters(a.chapters, b.chapters)
        )
    }

    private fun mergeChapters(a: List<KomikChapter>, b: List<KomikChapter>): List<KomikChapter> {
        val bySlug = mutableMapOf<String, KomikChapter>()
        (a + b).forEach { ch ->
            val key = ch.slug ?: ch.link ?: ch.title ?: ""
            if (key.isNotBlank()) {
                val existing = bySlug[key]
                bySlug[key] = if (existing == null) ch else mergeChapter(existing, ch)
            }
        }
        return bySlug.values.toList().sortedBy { it.title ?: "" }
    }

    private fun mergeChapter(a: KomikChapter, b: KomikChapter): KomikChapter {
        return a.copy(
            title = a.title?.takeIf { it.isNotBlank() } ?: b.title,
            link = a.link?.takeIf { it.isNotBlank() } ?: b.link,
            slug = a.slug?.takeIf { it.isNotBlank() } ?: b.slug
        )
    }

    override fun getId(item: KomikPage): String = item.url ?: item.page.toString()

    override fun mergeDuplicates(items: List<KomikPage>): List<KomikPage> {
        val byUrl = mutableMapOf<String, KomikPage>()
        items.forEach { page ->
            val key = page.url ?: page.page.toString()
            byUrl[key] = page
        }
        return byUrl.values.toList().sortedBy { it.page }
    }

    // ═══════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════

    data class KomikItem(
        val title: String? = null,
        val link: String? = null,
        val slug: String? = null,
        val cover: String? = null,
        val updatedAt: String? = null,
        val rating: String? = null,
        val source: String? = null
    ) {
        fun copy(
            title: String? = this.title,
            link: String? = this.link,
            slug: String? = this.slug,
            cover: String? = this.cover,
            updatedAt: String? = this.updatedAt,
            rating: String? = this.rating,
            source: String? = this.source
        ): KomikItem = KomikItem(title, link, slug, cover, updatedAt, rating, source)
    }

    data class KomikDetail(
        val title: String? = null,
        val cover: String? = null,
        val synopsis: String? = null,
        val chapters: List<KomikChapter>? = null,
        val slug: String? = null
    )

    data class KomikChapter(
        val title: String? = null,
        val link: String? = null,
        val slug: String? = null
    )

    data class KomikPage(
        val page: Int? = null,
        val url: String? = null
    )
}

// ═══════════════════════════════════════════
// MANGADEX EXTENSIONS
// ═══════════════════════════════════════════

private fun MdxManga.toKomikItem(): KomikRepository.KomikItem {
    val attrs = attributes
    return KomikRepository.KomikItem(
        title = attrs?.displayTitle(),
        slug = id,
        cover = coverUrl(),
        updatedAt = attrs?.lastChapter?.takeIf { it.isNotBlank() }?.let { "Ch. $it" }
            ?: attrs?.status,
        rating = null,
        source = "mangadex"
    )
}

private fun MdxManga.toKomikDetail(chapters: List<KomikRepository.KomikChapter>): KomikRepository.KomikDetail {
    val attrs = attributes
    return KomikRepository.KomikDetail(
        title = attrs?.displayTitle(),
        cover = coverUrl(),
        synopsis = attrs?.displayDescription(),
        chapters = chapters,
        slug = id
    )
}

private fun MdxChapter.toKomikChapter(): KomikRepository.KomikChapter {
    val attrs = attributes
    val number = attrs?.chapter?.takeIf { it.isNotBlank() }
    val title = attrs?.title?.takeIf { it.isNotBlank() }
    val label = when {
        number != null && title != null -> "Chapter $number · $title"
        number != null -> "Chapter $number"
        title != null -> title
        else -> "Chapter"
    }
    return KomikRepository.KomikChapter(
        title = label,
        slug = id,
        link = id
    )
}

// ═══════════════════════════════════════════
// LEGACY EXTENSIONS
// ═══════════════════════════════════════════

private fun KomikItem.toKomikItem(): KomikRepository.KomikItem {
    return KomikRepository.KomikItem(
        title = title,
        link = link,
        slug = slug,
        cover = cover,
        updatedAt = updatedAt,
        rating = rating,
        source = "legacy"
    )
}

private fun KomikDetailResponse.toKomikDetail(): KomikRepository.KomikDetail {
    return KomikRepository.KomikDetail(
        title = title,
        cover = cover,
        synopsis = synopsis,
        chapters = chapters?.map { it.toKomikChapter() } ?: emptyList(),
        slug = slug ?: link
    )
}

private fun KomikChapter.toKomikChapter(): KomikRepository.KomikChapter {
    return KomikRepository.KomikChapter(
        title = title,
        link = link,
        slug = slug
    )
}

private fun KomikPage.toKomikPage(): KomikRepository.KomikPage {
    return KomikRepository.KomikPage(
        page = page,
        url = url
    )
}