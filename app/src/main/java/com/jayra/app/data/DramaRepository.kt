package com.jayra.app.data

/**
 * DramaRepository - Parallel fetch from multiple drama/stream sources.
 * Sources: TVMaze (metadata) + VideoSource (video URLs)
 * 
 * Note: TVMaze doesn't provide video URLs. We need a separate video source.
 * For now, we'll use TVMaze for metadata and mark episodes as requiring external video source.
 */
class DramaRepository : BaseRepository<DramaItem>() {

    private val tvmazeApi = TvmazeApi.create()
    // private val videoApi = VideoSourceApi.create() // TODO: Add video source API

    override val cacheTtlMs = 30 * 60 * 1000 // 30 minutes

    // ═══════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════

    /** Fetch featured dramas from TVMaze */
    suspend fun fetchFeatured(): RepositoryResult<DramaItem> {
        return fetchParallel(
            "drama_featured",
            FetchSource("tvmaze") { fetchTvmazeFeatured() }
            // FetchSource("video_source") { fetchVideoSourceFeatured() }
        )
    }

    /** Search dramas from TVMaze */
    suspend fun search(query: String): RepositoryResult<DramaItem> {
        return fetchParallel(
            "drama_search_$query",
            FetchSource("tvmaze") { fetchTvmazeSearch(query) }
        )
    }

    /** Fetch drama detail with episodes from TVMaze */
    suspend fun fetchDetail(dramaId: String): RepositoryResult<DramaDetail> {
        return fetchParallel(
            "drama_detail_$dramaId",
            FetchSource("tvmaze") { fetchTvmazeDetail(dramaId) }
        )
    }

    /** Fetch video URLs for episodes (placeholder - needs real video source) */
    suspend fun fetchEpisodeVideos(dramaId: String): RepositoryResult<EpisodeVideo> {
        return fetchParallel(
            "drama_videos_$dramaId",
            // FetchSource("video_source") { fetchVideoSourceEpisodes(dramaId) }
            FetchSource("placeholder") { emptyList() }
        )
    }

    // ═══════════════════════════════════════════
    // TVMAZE API IMPLEMENTATIONS
    // ═══════════════════════════════════════════

    private suspend fun fetchTvmazeFeatured(): List<DramaItem> {
        val response = tvmazeApi.shows(page = 0)
        val shows = response.body().orEmpty()
        return shows
            .filter { it.image?.original?.isNotBlank() == true || it.image?.medium?.isNotBlank() == true }
            .take(20)
            .map { it.toDramaItem() }
    }

    private suspend fun fetchTvmazeSearch(query: String): List<DramaItem> {
        val response = tvmazeApi.search(query)
        val items = response.body().orEmpty()
        return items
            .map { it.show }
            .filterNotNull()
            .filter { it.image?.original?.isNotBlank() == true || it.image?.medium?.isNotBlank() == true }
            .take(20)
            .map { it.toDramaItem() }
    }

    private suspend fun fetchTvmazeDetail(id: String): List<DramaDetail> {
        val dramaId = id.toLongOrNull() ?: return emptyList()
        val response = tvmazeApi.detail(dramaId, embed = "episodes")
        val show = response.body()
        return if (show != null) listOf(show.toDramaDetail()) else emptyList()
    }

    // ═══════════════════════════════════════════
    // DEDUPLICATION & MERGE
    // ═══════════════════════════════════════════

    override fun getId(item: DramaItem): String = item.id ?: item.url ?: item.title ?: ""

    override fun mergeDuplicates(items: List<DramaItem>): List<DramaItem> {
        val byId = mutableMapOf<String, DramaItem>()
        items.forEach { item ->
            val id = getId(item)
            if (id.isBlank()) {
                byId[item.title ?: "unknown_${System.currentTimeMillis()}"] = item
            } else {
                val existing = byId[id]
                byId[id] = if (existing == null) item else mergeDramaItem(existing, item)
            }
        }
        return byId.values.toList()
    }

    private fun mergeDramaItem(a: DramaItem, b: DramaItem): DramaItem {
        return a.copy(
            cover = chooseBestCover(a.cover, b.cover),
            intro = a.intro?.takeIf { it.isNotBlank() } ?: b.intro,
            chapterCount = a.chapterCount ?: b.chapterCount
        )
    }

    private fun chooseBestCover(c1: String?, c2: String?): String? {
        val valid1 = c1?.takeIf { it.isNotBlank() && it.startsWith("http") }
        val valid2 = c2?.takeIf { it.isNotBlank() && it.startsWith("http") }
        return valid1 ?: valid2
    }

    override fun getId(item: DramaDetail): String = item.id ?: item.title ?: ""

    override fun mergeDuplicates(items: List<DramaDetail>): List<DramaDetail> {
        val byId = mutableMapOf<String, DramaDetail>()
        items.forEach { item ->
            val id = getId(item)
            val existing = byId[id]
            byId[id] = if (existing == null) item else mergeDramaDetail(existing, item)
        }
        return byId.values.toList()
    }

    private fun mergeDramaDetail(a: DramaDetail, b: DramaDetail): DramaDetail {
        return a.copy(
            cover = chooseBestCover(a.cover, b.cover),
            intro = a.intro?.takeIf { it.isNotBlank() } ?: b.intro,
            chapterCount = a.chapterCount ?: b.chapterCount,
            episodes = mergeEpisodes(a.episodes, b.episodes)
        )
    }

    private fun mergeEpisodes(a: List<DramaEpisode>, b: List<DramaEpisode>): List<DramaEpisode> {
        val byEp = mutableMapOf<String, DramaEpisode>()
        (a + b).forEach { ep ->
            val key = ep.index?.toString() ?: ep.name ?: ""
            if (key.isNotBlank()) {
                val existing = byEp[key]
                byEp[key] = if (existing == null) ep else mergeEpisode(existing, ep)
            }
        }
        return byEp.values.toList().sortedBy { it.index ?: 0 }
    }

    private fun mergeEpisode(a: DramaEpisode, b: DramaEpisode): DramaEpisode {
        return a.copy(
            name = a.name?.takeIf { it.isNotBlank() } ?: b.name,
            unlock = a.unlock ?: b.unlock,
            mp4 = a.mp4?.takeIf { it.isNotBlank() } ?: b.mp4,
            videoUrl = a.videoUrl?.takeIf { it.isNotBlank() } ?: b.videoUrl
        )
    }

    // ═══════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════

    data class DramaItem(
        val id: String? = null,
        val title: String? = null,
        val cover: String? = null,
        val intro: String? = null,
        val chapterCount: Int? = null,
        val url: String? = null,
        val source: String? = null
    ) {
        fun copy(
            id: String? = this.id,
            title: String? = this.title,
            cover: String? = this.cover,
            intro: String? = this.intro,
            chapterCount: Int? = this.chapterCount,
            url: String? = this.url,
            source: String? = this.source
        ): DramaItem = DramaItem(id, title, cover, intro, chapterCount, url, source)
    }

    data class DramaDetail(
        val id: String? = null,
        val title: String? = null,
        val cover: String? = null,
        val intro: String? = null,
        val chapterCount: Int? = null,
        val url: String? = null,
        val episodes: List<DramaEpisode>? = null
    )

    data class DramaEpisode(
        val index: Int? = null,
        val name: String? = null,
        val unlock: Boolean? = null,
        val mp4: String? = null,
        val videoUrl: String? = null
    )

    data class EpisodeVideo(
        val episodeIndex: Int? = null,
        val videoUrl: String? = null,
        val headers: Map<String, String>? = null
    )
}

// ═══════════════════════════════════════════
// TVMAZE EXTENSIONS
// ═══════════════════════════════════════════

private fun TvmazeShow.toDramaItem(): DramaRepository.DramaItem {
    return DramaRepository.DramaItem(
        id = id?.toString(),
        title = name,
        cover = image?.original ?: image?.medium,
        intro = summary?.let { stripHtml(it) },
        chapterCount = embedded?.episodes?.size,
        url = id?.toString(),
        source = "tvmaze"
    )
}

private fun TvmazeShow.toDramaDetail(): DramaRepository.DramaDetail {
    val episodes = embedded?.episodes?.map { ep ->
        DramaRepository.DramaEpisode(
            index = ep.number,
            name = ep.name,
            unlock = false, // No video URL from TVMaze
            mp4 = null,
            videoUrl = null
        )
    } ?: emptyList()

    return DramaRepository.DramaDetail(
        id = id?.toString(),
        title = name,
        cover = image?.original ?: image?.medium,
        intro = summary?.let { stripHtml(it) },
        chapterCount = episodes.size,
        url = id?.toString(),
        episodes = episodes
    )
}

private fun stripHtml(raw: String): String =
    raw.replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&", "&")
        .replace(""", "\"")
        .trim()