package com.jayra.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * SATU repository untuk fitur Donghua (donghua + donghub).
 * Semua request section dijalankan PARALEL, hasil digabung lalu
 * dibersihkan dari duplikat.
 */
class DonghuaRepository(private val api: DonghuaApi = DonghuaApi.create()) {

    suspend fun home(source: DonghuaSource): Result<DonghuaHome> = coroutineScope {
        when (source) {
            DonghuaSource.DONGHUA -> {
                val homeReq = async { runCatching { api.donghuaHome() } }
                val ongoingReq = async { runCatching { api.donghuaOngoing(1) } }
                val completedReq = async { runCatching { api.donghuaCompleted(1) } }

                val home = homeReq.await().getOrNull()?.takeIf { it.isSuccessful }?.body()
                val ongoing = ongoingReq.await().getOrNull()?.takeIf { it.isSuccessful }?.body()
                val completed = completedReq.await().getOrNull()?.takeIf { it.isSuccessful }?.body()

                val latest = home?.latestRelease.orEmpty()
                    .mapNotNull { it.toModel(DonghuaSource.DONGHUA) }
                    .distinctBySlug()
                val ongoingItems = ongoing?.ongoingDonghua.orEmpty()
                    .mapNotNull { it.toModel(DonghuaSource.DONGHUA) }
                    .distinctBySlug()
                val completedItems = (completed?.completedDonghua.orEmpty() + home?.completedDonghua.orEmpty())
                    .mapNotNull { it.toModel(DonghuaSource.DONGHUA) }
                    .distinctBySlug()

                if (latest.isEmpty() && ongoingItems.isEmpty() && completedItems.isEmpty()) {
                    Result.failure(IllegalStateException("Gagal memuat katalog Donghua"))
                } else {
                    Result.success(
                        DonghuaHome(
                            source = DonghuaSource.DONGHUA,
                            hero = ongoingItems.take(3).ifEmpty { latest.take(3) },
                            latest = latest,
                            ongoing = ongoingItems,
                            completed = completedItems
                        )
                    )
                }
            }

            DonghuaSource.DONGHUB -> {
                val homeReq = async { runCatching { api.donghubHome() } }
                val popularReq = async { runCatching { api.donghubPopular() } }
                val latestReq = async { runCatching { api.donghubLatest() } }

                val home = homeReq.await().getOrNull()?.takeIf { it.isSuccessful }?.body()?.data
                val popular = popularReq.await().getOrNull()?.takeIf { it.isSuccessful }?.body()
                    ?.data.orEmpty()
                val latest = latestReq.await().getOrNull()?.takeIf { it.isSuccessful }?.body()
                    ?.data.orEmpty()

                val latestItems = (latest + home?.latest.orEmpty())
                    .mapNotNull { it.toModel(DonghuaSource.DONGHUB) }
                    .distinctBySlug()
                val popularItems = (popular + home?.popular.orEmpty())
                    .mapNotNull { it.toModel(DonghuaSource.DONGHUB) }
                    .distinctBySlug()
                val heroItems = home?.slider.orEmpty()
                    .mapNotNull { it.toModel(DonghuaSource.DONGHUB) }
                    .distinctBySlug()

                if (latestItems.isEmpty() && popularItems.isEmpty() && heroItems.isEmpty()) {
                    Result.failure(IllegalStateException("Gagal memuat katalog Donghub"))
                } else {
                    Result.success(
                        DonghuaHome(
                            source = DonghuaSource.DONGHUB,
                            hero = heroItems.take(3).ifEmpty { popularItems.take(3) },
                            latest = latestItems,
                            ongoing = popularItems,
                            completed = emptyList()
                        )
                    )
                }
            }
        }
    }

    suspend fun search(
        source: DonghuaSource,
        keyword: String
    ): Result<List<DonghuaItem>> {
        if (keyword.isBlank()) return Result.success(emptyList())
        return runCatching {
            val items = when (source) {
                DonghuaSource.DONGHUA ->
                    api.donghuaSearch(keyword = keyword.trim(), page = 1).body()?.data.orEmpty()

                DonghuaSource.DONGHUB ->
                    api.donghubSearch(query = keyword.trim()).body()?.data.orEmpty()
            }
            items.mapNotNull { it.toModel(source) }.distinctBySlug()
        }
    }

    suspend fun detail(
        source: DonghuaSource,
        slug: String
    ): Result<DonghuaDetail> = runCatching {
        when (source) {
            DonghuaSource.DONGHUA -> {
                val res = api.donghuaDetail(slug)
                val dto = res.body()
                if (!res.isSuccessful || dto == null) {
                    throw IllegalStateException("Detail Donghua tidak ditemukan")
                }
                DonghuaDetail(
                    title = cleanDonghuaTitle(dto.title) ?: slug,
                    slug = slug,
                    poster = normalizeUrl(dto.poster) ?: normalizeUrl(dto.cover),
                    synopsis = dto.synopsis,
                    rating = dto.rating,
                    status = dto.status,
                    type = dto.type,
                    studio = dto.studio,
                    season = dto.season,
                    released = dto.released,
                    totalEpisodes = dto.episodesCount,
                    genres = dto.genres.orEmpty().mapNotNull { it.toModel() },
                    episodes = dto.episodesList.orEmpty()
                        .mapNotNull { it.toModel(DonghuaSource.DONGHUA) },
                    recommendations = dto.recommendations.orEmpty()
                        .mapNotNull { it.toModel(DonghuaSource.DONGHUA) }
                        .distinctBySlug(),
                    source = DonghuaSource.DONGHUA
                )
            }

            DonghuaSource.DONGHUB -> {
                val res = api.donghubDetail(slug)
                val dto = res.body()?.data
                if (!res.isSuccessful || dto == null) {
                    throw IllegalStateException("Detail Donghub tidak ditemukan")
                }
                DonghuaDetail(
                    title = cleanDonghuaTitle(dto.title) ?: slug,
                    slug = slug,
                    poster = normalizeUrl(dto.poster),
                    synopsis = dto.synopsis,
                    rating = dto.rating,
                    status = dto.info?.status,
                    type = dto.info?.type,
                    studio = dto.info?.studio,
                    season = null,
                    released = dto.info?.released,
                    totalEpisodes = null,
                    genres = dto.genres.orEmpty().mapNotNull { it.toModel() },
                    episodes = dto.episodes.orEmpty()
                        .mapNotNull { it.toModel(DonghuaSource.DONGHUB) },
                    recommendations = dto.recommendations.orEmpty()
                        .mapNotNull { it.toModel(DonghuaSource.DONGHUB) }
                        .distinctBySlug(),
                    source = DonghuaSource.DONGHUB
                )
            }
        }
    }

    suspend fun episode(
        source: DonghuaSource,
        slug: String
    ): Result<DonghuaEpisode> = runCatching {
        when (source) {
            DonghuaSource.DONGHUA -> {
                val res = api.donghuaEpisode(slug)
                val dto = res.body()
                if (!res.isSuccessful || dto == null) {
                    throw IllegalStateException("Data episode tidak ditemukan")
                }
                val servers = dto.streaming?.servers.orEmpty().mapNotNull { s ->
                    val url = normalizeUrl(s.url)
                    if (url == null) null else DonghuaServer(s.name?.trim().orEmpty().ifEmpty { "Server" }, url)
                }
                DonghuaEpisode(
                    title = cleanDonghuaTitle(dto.episode) ?: slug,
                    slug = slug,
                    source = DonghuaSource.DONGHUA,
                    servers = servers,
                    prevSlug = dto.navigation?.previous?.slug?.trim()?.takeIf { it.isNotEmpty() },
                    nextSlug = dto.navigation?.next?.slug?.trim()?.takeIf { it.isNotEmpty() }
                )
            }

            DonghuaSource.DONGHUB -> {
                val res = api.donghubEpisode(slug)
                val dto = res.body()?.data
                if (!res.isSuccessful || dto == null) {
                    throw IllegalStateException("Data episode tidak ditemukan")
                }
                val servers = dto.streams.orEmpty().mapNotNull { s ->
                    val url = normalizeUrl(s.url)
                    if (url == null) null else DonghuaServer(s.server?.trim().orEmpty().ifEmpty { "Server" }, url)
                }
                DonghuaEpisode(
                    title = cleanDonghuaTitle(dto.title) ?: slug,
                    slug = slug,
                    source = DonghuaSource.DONGHUB,
                    servers = servers,
                    prevSlug = dto.navigation?.prevSlug?.trim()?.takeIf { it.isNotEmpty() },
                    nextSlug = dto.navigation?.nextSlug?.trim()?.takeIf { it.isNotEmpty() }
                )
            }
        }
    }

    private fun normalizeUrl(raw: String?): String? {
        val url = raw?.replace('\u00A0', ' ')?.trim() ?: return null
        if (url.isEmpty()) return null
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") -> "https://" + url.removePrefix("http://")
            url.startsWith("https://") -> url
            else -> null
        }
    }
}
