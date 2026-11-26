package com.jayra.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * AnimeRepository - Parallel fetch from multiple anime sources.
 * Sources: Kitsu (primary) + AnimeApi (legacy, if alive) + future sources
 */
class AnimeRepository : BaseRepository<AnimeItem>() {

    private val kitsuApi = KitsuApi.create()
    private val legacyApi = AnimeApi.create()

    override val cacheTtlMs = 10 * 60 * 1000 // 10 minutes

    // ═══════════════════════════════════════════
    // PUBLIC API - HOME SECTIONS
    // ═══════════════════════════════════════════

    /** Fetch all home sections in parallel: popular, ongoing, completed */
    suspend fun fetchHomeSections(): HomeSectionsResult {
        val popular = fetchPopular()
        val ongoing = fetchOngoing()
        val completed = fetchCompleted()
        return HomeSectionsResult(popular, ongoing, completed)
    }

    /** Fetch popular anime from all sources in parallel */
    suspend fun fetchPopular(): RepositoryResult<AnimeItem> {
        return fetchParallel(
            "anime_popular",
            FetchSource("kitsu") { fetchKitsuPopular() },
            FetchSource("legacy") { fetchLegacyPopular() }
        )
    }

    /** Fetch ongoing anime from all sources in parallel */
    suspend fun fetchOngoing(): RepositoryResult<AnimeItem> {
        return fetchParallel(
            "anime_ongoing",
            FetchSource("kitsu") { fetchKitsuOngoing() },
            FetchSource("legacy") { fetchLegacyOngoing() }
        )
    }

    /** Fetch completed anime from all sources in parallel */
    suspend fun fetchCompleted(): RepositoryResult<AnimeItem> {
        return fetchParallel(
            "anime_completed",
            FetchSource("kitsu") { fetchKitsuCompleted() },
            FetchSource("legacy") { fetchLegacyCompleted() }
        )
    }

    /** Search anime from all sources in parallel */
    suspend fun search(query: String): RepositoryResult<AnimeItem> {
        return fetchParallel(
            "anime_search_$query",
            FetchSource("kitsu") { fetchKitsuSearch(query) },
            FetchSource("legacy") { fetchLegacySearch(query) }
        )
    }

    /** Fetch anime detail from all sources in parallel, merge results */
    suspend fun fetchDetail(animeId: String): RepositoryResult<AnimeDetail> {
        return fetchParallel(
            "anime_detail_$animeId",
            FetchSource("kitsu") { fetchKitsuDetail(animeId) },
            FetchSource("legacy") { fetchLegacyDetail(animeId) }
        )
    }

    /** Fetch stream URLs for an episode */
    suspend fun fetchStream(episodeUrl: String): RepositoryResult<StreamUrl> {
        return fetchParallel(
            "anime_stream_$episodeUrl",
            FetchSource("kitsu") { fetchKitsuStream(episodeUrl) },
            FetchSource("legacy") { fetchLegacyStream(episodeUrl) }
        )
    }

    // ═══════════════════════════════════════════
    // KITSU API IMPLEMENTATIONS
    // ═══════════════════════════════════════════

    private suspend fun fetchKitsuPopular(): List<AnimeItem> {
        val response = kitsuApi.list(limit = 20, sort = "-userCount")
        val data = response.body()?.data.orEmpty()
        return data.map { it.toAnimeItem("popular") }
    }

    private suspend fun fetchKitsuOngoing(): List<AnimeItem> {
        val response = kitsuApi.list(limit = 20, sort = "-userCount", status = "current")
        val data = response.body()?.data.orEmpty()
        return data.map { it.toAnimeItem("ongoing") }
    }

    private suspend fun fetchKitsuCompleted(): List<AnimeItem> {
        val response = kitsuApi.list(limit = 20, sort = "-averageRating", status = "finished")
        val data = response.body()?.data.orEmpty()
        return data.map { it.toAnimeItem("completed") }
    }

    private suspend fun fetchKitsuSearch(query: String): List<AnimeItem> {
        val response = kitsuApi.search(query, limit = 20)
        val data = response.body()?.data.orEmpty()
        return data.map { it.toAnimeItem("search") }
    }

    private suspend fun fetchKitsuDetail(id: String): List<AnimeDetail> {
        val response = kitsuApi.detail(id)
        val data = response.body()?.data
        return if (data != null) listOf(data.toAnimeDetail()) else emptyList()
    }

    private suspend fun fetchKitsuStream(episodeUrl: String): List<StreamUrl> {
        // Kitsu doesn't provide streaming - return empty
        emptyList()
    }

    // ═══════════════════════════════════════════
    // LEGACY API IMPLEMENTATIONS (apii.nexadev.my.id)
    // ═══════════════════════════════════════════

    private suspend fun fetchLegacyPopular(): List<AnimeItem> {
        val response = legacyApi.popular(on = 1, page = 1)
        val data = response.body()?.data?.animeList.orEmpty()
        return data.map { it.toAnimeItem("popular") }
    }

    private suspend fun fetchLegacyOngoing(): List<AnimeItem> {
        val response = legacyApi.ongoing(on = 1, page = 1)
        val data = response.body()?.data?.animeList.orEmpty()
        return data.map { it.toAnimeItem("ongoing") }
    }

    private suspend fun fetchLegacyCompleted(): List<AnimeItem> {
        val response = legacyApi.completed(on = 1, page = 1)
        val data = response.body()?.data?.animeList.orEmpty()
        return data.map { it.toAnimeItem("completed") }
    }

    private suspend fun fetchLegacySearch(query: String): List<AnimeItem> {
        val response = legacyApi.search(query)
        val data = response.body()?.data?.results.orEmpty()
        return data.map { it.toAnimeItem("search") }
    }

    private suspend fun fetchLegacyDetail(url: String): List<AnimeDetail> {
        val response = legacyApi.detail(url)
        val data = response.body()?.data
        return if (data != null) listOf(data.toAnimeDetail()) else emptyList()
    }

    private suspend fun fetchLegacyStream(episodeUrl: String): List<StreamUrl> {
        val response = legacyApi.stream(episodeUrl)
        val data = response.body()?.data
        return if (data != null) listOf(data.toStreamUrl()) else emptyList()
    }

    // ═══════════════════════════════════════════
    // DEDUPLICATION & MERGE
    // ═══════════════════════════════════════════

    override fun getId(item: AnimeItem): String = item.id ?: item.link ?: item.title ?: ""

    override fun mergeDuplicates(items: List<AnimeItem>): List<AnimeItem> {
        val byId = mutableMapOf<String, AnimeItem>()
        items.forEach { item ->
            val id = getId(item)
            if (id.isBlank()) {
                byId[item.title ?: "unknown_${System.currentTimeMillis()}"] = item
            } else {
                val existing = byId[id]
                byId[id] = if (existing == null) item else mergeAnimeItem(existing, item)
            }
        }
        return byId.values.toList()
    }

    private fun mergeAnimeItem(a: AnimeItem, b: AnimeItem): AnimeItem {
        // Prefer non-null fields, prefer higher quality image
        val image = chooseBestImage(a.image, b.image)
        return a.copy(
            image = image,
            latestEpisode = a.latestEpisode?.takeIf { it.isNotBlank() } ?: b.latestEpisode,
            type = a.type?.takeIf { it.isNotBlank() } ?: b.type
        )
    }

    private fun chooseBestImage(img1: String?, img2: String?): String? {
        val valid1 = img1?.takeIf { it.isNotBlank() && it.startsWith("http") }
        val valid2 = img2?.takeIf { it.isNotBlank() && it.startsWith("http") }
        return valid1 ?: valid2
    }

    override fun getId(item: AnimeDetail): String = item.id ?: item.title ?: ""

    override fun mergeDuplicates(items: List<AnimeDetail>): List<AnimeDetail> {
        val byId = mutableMapOf<String, AnimeDetail>()
        items.forEach { item ->
            val id = getId(item)
            val existing = byId[id]
            byId[id] = if (existing == null) item else mergeAnimeDetail(existing, item)
        }
        return byId.values.toList()
    }

    private fun mergeAnimeDetail(a: AnimeDetail, b: AnimeDetail): AnimeDetail {
        return a.copy(
            image = chooseBestImage(a.image, b.image),
            synopsis = a.synopsis?.takeIf { it.isNotBlank() } ?: b.synopsis,
            rating = a.rating ?: b.rating,
            totalEpisodes = a.totalEpisodes ?: b.totalEpisodes,
            genres = (a.genres.orEmpty() + b.genres.orEmpty()).distinct(),
            info = (a.info.orEmpty() + b.info.orEmpty()).toMap(),
            episodes = mergeEpisodes(a.episodes, b.episodes)
        )
    }

    private fun mergeEpisodes(a: List<AnimeEpisode>, b: List<AnimeEpisode>): List<AnimeEpisode> {
        val byEp = mutableMapOf<String, AnimeEpisode>()
        (a + b).forEach { ep ->
            val key = ep.episode ?: ep.title ?: ep.episodeUrl ?: ""
            if (key.isNotBlank()) {
                val existing = byId[key]
                byId[key] = if (existing == null) ep else mergeEpisode(existing, ep)
            }
        }
        return byId.values.toList().sortedBy { it.episode?.toIntOrNull() ?: 0 }
    }

    private fun mergeEpisode(a: AnimeEpisode, b: AnimeEpisode): AnimeEpisode {
        return a.copy(
            title = a.title?.takeIf { it.isNotBlank() } ?: b.title,
            episodeUrl = a.episodeUrl?.takeIf { it.isNotBlank() } ?: b.episodeUrl
        )
    }

    // ═══════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════

    data class HomeSectionsResult(
        val popular: RepositoryResult<AnimeItem>,
        val ongoing: RepositoryResult<AnimeItem>,
        val completed: RepositoryResult<AnimeItem>
    )

    data class AnimeItem(
        val id: String? = null,        // Kitsu ID or Legacy link
        val title: String? = null,
        val link: String? = null,      // Detail URL
        val image: String? = null,     // Poster URL
        val latestEpisode: String? = null,
        val type: String? = null,
        val source: String? = null     // Which source this came from
    ) {
        fun copy(
            id: String? = this.id,
            title: String? = this.title,
            link: String? = this.link,
            image: String? = this.image,
            latestEpisode: String? = this.latestEpisode,
            type: String? = this.type,
            source: String? = this.source
        ): AnimeItem = AnimeItem(id, title, link, image, latestEpisode, type, source)
    }

    data class AnimeDetail(
        val id: String? = null,
        val title: String? = null,
        val image: String? = null,
        val synopsis: String? = null,
        val rating: Double? = null,
        val totalEpisodes: Int? = null,
        val genres: List<String>? = null,
        val info: Map<String, String>? = null,
        val episodes: List<AnimeEpisode>? = null
    )

    data class AnimeEpisode(
        val episode: String? = null,
        val title: String? = null,
        val episodeUrl: String? = null
    )

    data class StreamUrl(
        val streamUrl: String? = null,
        val alternativeLinks: List<String>? = null,
        val referer: String? = null,
        val headers: Map<String, String>? = null
    )
}

// ═══════════════════════════════════════════
// KITSU EXTENSIONS
// ═══════════════════════════════════════════

private fun KitsuAnime.toAnimeItem(section: String): AnimeRepository.AnimeItem {
    val attrs = attributes
    return AnimeRepository.AnimeItem(
        id = id,
        title = attrs?.displayTitle(),
        link = id,
        image = attrs?.posterImage?.best()?.takeIf { it.isNotBlank() }
            ?: attrs?.coverImage?.best(),
        latestEpisode = attrs?.episodeCount?.let { "$it eps" } ?: attrs?.status,
        type = attrs?.subtype,
        source = "kitsu"
    )
}

private fun KitsuAnime.toAnimeDetail(): AnimeRepository.AnimeDetail {
    val attrs = attributes
    val info = buildMap {
        attrs?.status?.let { put("Status", it) }
        attrs?.startDate?.let { put("Mulai", it) }
        attrs?.episodeCount?.let { put("Episode", "$it") }
    }
    return AnimeRepository.AnimeDetail(
        id = id,
        title = attrs?.displayTitle(),
        image = attrs?.posterImage?.best()?.takeIf { it.isNotBlank() }
            ?: attrs?.coverImage?.best(),
        synopsis = attrs?.synopsis,
        rating = attrs?.averageRating?.toDoubleOrNull(),
        totalEpisodes = attrs?.episodeCount,
        genres = null, // Kitsu genres need separate fetch
        info = info,
        episodes = emptyList()
    )
}

// ═══════════════════════════════════════════
// LEGACY EXTENSIONS
// ═══════════════════════════════════════════

private fun AnimeSearchItem.toAnimeItem(section: String): AnimeRepository.AnimeItem {
    return AnimeRepository.AnimeItem(
        id = link,
        title = title,
        link = link,
        image = image,
        latestEpisode = latestEpisode,
        type = type,
        source = "legacy"
    )
}

private fun AnimeDetailData.toAnimeDetail(): AnimeRepository.AnimeDetail {
    return AnimeRepository.AnimeDetail(
        id = link ?: title,
        title = title,
        image = image,
        synopsis = synopsis,
        rating = rating,
        totalEpisodes = totalEpisodes,
        genres = genres,
        info = info,
        episodes = episodes?.map { it.toAnimeEpisode() } ?: emptyList()
    )
}

private fun AnimeEpisode.toAnimeEpisode(): AnimeRepository.AnimeEpisode {
    return AnimeRepository.AnimeEpisode(
        episode = episode,
        title = title,
        episodeUrl = episodeUrl
    )
}

private fun AnimeStreamData.toStreamUrl(): AnimeRepository.StreamUrl {
    return AnimeRepository.StreamUrl(
        streamUrl = streamUrl,
        alternativeLinks = alternativeLinks,
        referer = "https://apii.nexadev.my.id/",
        headers = mapOf(
            "Referer" to "https://apii.nexadev.my.id/",
            "User-Agent" to NetworkModule.BROWSER_USER_AGENT
        )
    )
}