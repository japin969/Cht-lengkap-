package com.jayra.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * DonghuaRepository - Parallel fetching for Donghua and Donghub
 * All API calls run in parallel, errors handled per-source.
 */
class DonghuaRepository {

    private val api = DonghuaApi.create()

    // In-memory cache with TTL
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val CACHE_TTL_MS = 5 * 60 * 1000 // 5 minutes

    // ═══════════════════════════════════════════
    // HOME - Parallel fetch Donghua + Donghub
    // ═══════════════════════════════════════════

    suspend fun getHome(): DonghuaHomeResult {
        val cacheKey = "donghua_home"
        val cached = getCached<DonghuaHomeResult>(cacheKey)
        if (cached != null) return cached

        return coroutineScope {
            val donghuaHome = async(Dispatchers.IO) {
                try {
                    val response = api.home(1)
                    response.body()
                } catch (e: Exception) {
                    null
                }
            }

            val donghubHome = async(Dispatchers.IO) {
                try {
                    val response = api.donghubHome()
                    response.body()
                } catch (e: Exception) {
                    null
                }
            }

            val (donghuaResult, donghubResult) = awaitAll(donghuaHome, donghubHome)

            val result = DonghuaHomeResult(
                donghuaData = donghuaResult,
                donghubData = donghubResult
            )

            putCache(cacheKey, result)
            result
        }
    }

    // ═══════════════════════════════════════════
    // SECTION LISTS - Parallel fetch
    // ════════════════════════════════════════════

    suspend fun getOngoing(page: Int = 1): DonghuaSectionResult {
        return fetchSection(
            cacheKey = "donghua_ongoing_$page",
            donghuaCall = { api.ongoing(page) },
            donghubCall = { api.donghubLatest() }
        )
    }

    suspend fun getCompleted(page: Int = 1): DonghuaSectionResult {
        return fetchSection(
            cacheKey = "donghua_completed_$page",
            donghuaCall = { api.completed(page) },
            donghubCall = { null }
        )
    }

    suspend fun getLatest(page: Int = 1): DonghuaSectionResult {
        return fetchSection(
            cacheKey = "donghua_latest_$page",
            donghuaCall = { api.latest(page) },
            donghubCall = { api.donghubLatest() }
        )
    }

    suspend fun getPopular(page: Int = 1): DonghuaSectionResult {
        return fetchSection(
            cacheKey = "donghua_popular_$page",
            donghuaCall = { null },
            donghubCall = { api.donghubPopular() }
        )
    }

    suspend fun getMovies(page: Int = 1): DonghuaSectionResult {
        return fetchSection(
            cacheKey = "donghua_movies_$page",
            donghuaCall = { null },
            donghubCall = { api.donghubMovie() }
        )
    }

    suspend fun getSchedule(): DonghuaScheduleResult {
        val cacheKey = "donghua_schedule"
        val cached = getCached<DonghuaScheduleResult>(cacheKey)
        if (cached != null) return cached

        return coroutineScope {
            val donghuaSchedule = async(Dispatchers.IO) {
                try {
                    api.schedule().body()
                } catch (e: Exception) {
                    null
                }
            }

            val donghubSchedule = async(Dispatchers.IO) {
                try {
                    api.donghubSchedule().body()
                } catch (e: Exception) {
                    null
                }
            }

            val (dResult, hResult) = awaitAll(donghuaSchedule, donghubSchedule)
            val result = DonghuaScheduleResult(
                donghuaData = dResult,
                donghubData = hResult
            )
            putCache(cacheKey, result)
            result
        }
    }

    suspend fun getGenres(): DonghuaGenresResult {
        val cacheKey = "donghua_genres"
        val cached = getCached<DonghuaGenresResult>(cacheKey)
        if (cached != null) return cached

        return try {
            val response = api.genres()
            if (response.isSuccessful) {
                val result = DonghuaGenresResult(genres = response.body()?.data ?: emptyList())
                putCache(cacheKey, result)
                result
            } else {
                DonghuaGenresResult(genres = emptyList(), error = "HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            DonghuaGenresResult(genres = emptyList(), error = e.message)
        }
    }

    // ═══════════════════════════════════════════
    // SEARCH
    // ═══════════════════════════════════════════

    suspend fun searchDonghua(keyword: String, page: Int = 1): DonghuaSearchResult {
        return try {
            val response = api.search(keyword, page)
            if (response.isSuccessful) {
                DonghuaSearchResult(
                    items = response.body()?.data?.list?.map { DonghuaCardItem.fromDonghua(it, DonghuaSource.DONGHUA) } ?: emptyList(),
                    pagination = response.body()?.data?.pagination
                )
            } else {
                DonghuaSearchResult(items = emptyList(), error = "HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            DonghuaSearchResult(items = emptyList(), error = e.message)
        }
    }

    suspend fun searchDonghub(query: String): DonghuaSearchResult {
        return try {
            val response = api.donghubSearch(query)
            if (response.isSuccessful) {
                DonghuaSearchResult(
                    items = response.body()?.data?.list?.map { DonghuaCardItem.fromDonghub(it, DonghuaSource.DONGHUB) } ?: emptyList(),
                    pagination = response.body()?.data?.pagination
                )
            } else {
                DonghuaSearchResult(items = emptyList(), error = "HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            DonghuaSearchResult(items = emptyList(), error = e.message)
        }
    }

    // ════════════════════════════════════════════
    // DETAIL & EPISODE
    // ════════════════════════════════════════════

    suspend fun getDonghuaDetail(slug: String): DonghuaDetailResult {
        return try {
            val response = api.detail(slug)
            if (response.isSuccessful) {
                val data = response.body()?.data
                DonghuaDetailResult(
                    title = data?.title,
                    poster = data?.poster ?: data?.cover,
                    synopsis = data?.synopsis,
                    score = data?.score,
                    totalEpisode = data?.totalEpisode,
                    genres = data?.genres?.map { it.name }.orEmpty(),
                    episodes = data?.episodes?.map { ep ->
                        DonghuaEpisodeItem(
                            id = ep.id?.toString() ?: ep.slug ?: "",
                            title = ep.title ?: "Episode ${ep.episode}",
                            slug = ep.slug ?: "",
                            episode = ep.episode ?: "",
                            releaseDate = ep.releaseDate
                        )
                    } ?: emptyList(),
                    recommend = data?.recommend?.map { DonghuaCardItem.fromDonghua(it, DonghuaSource.DONGHUA) } ?: emptyList()
                )
            } else {
                DonghuaDetailResult(error = "HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            DonghuaDetailResult(error = e.message)
        }
    }

    suspend fun getDonghubDetail(slug: String): DonghuaDetailResult {
        return try {
            val response = api.donghubDetail(slug)
            if (response.isSuccessful) {
                val data = response.body()?.data
                DonghuaDetailResult(
                    title = data?.title,
                    poster = data?.poster ?: data?.cover,
                    synopsis = data?.synopsis,
                    score = data?.score,
                    totalEpisode = data?.totalEpisode,
                    genres = data?.genres?.map { it.name }.orEmpty(),
                    episodes = data?.episodes?.map { ep ->
                        DonghuaEpisodeItem(
                            id = ep.id?.toString() ?: ep.slug ?: "",
                            title = ep.title ?: "Episode ${ep.episode}",
                            slug = ep.slug ?: "",
                            episode = ep.episode ?: "",
                            releaseDate = ep.releaseDate
                        )
                    } ?: emptyList()
                )
            } else {
                DonghuaDetailResult(error = "HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            DonghuaDetailResult(error = e.message)
        }
    }

    suspend fun getDonghuaEpisode(slug: String): DonghuaStreamUrl? {
        return try {
            val response = api.episode(slug)
            if (response.isSuccessful) {
                val data = response.body()?.data
                DonghuaStreamUrl(
                    streamUrl = data?.streamUrl,
                    backupUrls = data?.backupUrls ?: emptyList(),
                    headers = data?.headers ?: emptyMap(),
                    referer = data?.referer
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getDonghubEpisode(slug: String): DonghuaStreamUrl? {
        return try {
            val response = api.donghubEpisode(slug)
            if (response.isSuccessful) {
                val data = response.body()?.data
                DonghuaStreamUrl(
                    streamUrl = data?.streamUrl,
                    backupUrls = data?.backupUrls ?: emptyList(),
                    headers = data?.headers ?: emptyMap(),
                    referer = data?.referer
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════

    private suspend fun fetchSection(
        cacheKey: String,
        donghuaCall: suspend () -> retrofit2.Response<DonghuaListResponse?>,
        donghubCall: suspend () -> retrofit2.Response<DonghubListResponse?>
    ): DonghuaSectionResult {
        val cached = getCached<DonghuaSectionResult>(cacheKey)
        if (cached != null) return cached

        return coroutineScope {
            val donghua = async(Dispatchers.IO) { donghuaCall() }
            val donghub = async(Dispatchers.IO) { donghubCall() }

            val (dResult, hResult) = awaitAll(donghua, donghub)

            val donghuaItems = dResult?.body()?.data?.list?.map { DonghuaCardItem.fromDonghua(it, DonghuaSource.DONGHUA) } ?: emptyList()
            val donghubItems = hResult?.body()?.data?.list?.map { DonghuaCardItem.fromDonghub(it, DonghuaSource.DONGHUB) } ?: emptyList()

            val result = DonghuaSectionResult(
                donghuaItems = donghuaItems,
                donghubItems = donghubItems
            )

            putCache(cacheKey, result)
            result
        }
    }

    private fun getCached<T>(key: String): T? {
        val entry = cache[key]
        if (entry != null && (System.currentTimeMillis() - entry.timestamp < CACHE_TTL_MS)) {
            return entry.data as T
        }
        return null
    }

    private fun <T> putCache(key: String, data: T) {
        cache[key] = CacheEntry(data, System.currentTimeMillis())
    }

    private data class CacheEntry<T>(val data: T, val timestamp: Long)
}

// ═══════════════════════════════════════════
// RESULT CLASSES
// ════════════════════════════════════════════

data class DonghuaHomeResult(
    val donghuaData: DonghuaHomeResponse?,
    val donghubData: DonghubHomeResponse?
) {
    val hasAnyData: Boolean
        get() = (donghuaData?.data?.ongoing?.isNotEmpty() == true) ||
                (donghubData?.data?.featured?.isNotEmpty() == true) ||
                (donghubData?.data?.latest?.isNotEmpty() == true)
}

data class DonghuaSectionResult(
    val donghuaItems: List<DonghuaCardItem> = emptyList(),
    val donghubItems: List<DonghuaCardItem> = emptyList()
) {
    val allItems: List<DonghuaCardItem>
        get() = donghuaItems + donghubItems

    val hasAnyData: Boolean
        get() = allItems.isNotEmpty()
}

data class DonghuaScheduleResult(
    val donghuaData: DonghuaScheduleResponse?,
    val donghubData: DonghubScheduleResponse?
)

data class DonghuaGenresResult(
    val genres: List<DonghuaGenre> = emptyList(),
    val error: String? = null
)

data class DonghuaSearchResult(
    val items: List<DonghuaCardItem> = emptyList(),
    val pagination: DonghuaPagination? = null,
    val error: String? = null
)

data class DonghuaDetailResult(
    val title: String? = null,
    val poster: String? = null,
    val synopsis: String? = null,
    val score: Double? = null,
    val totalEpisode: Int? = null,
    val genres: List<String> = emptyList(),
    val episodes: List<DonghuaEpisodeItem> = emptyList(),
    val recommend: List<DonghuaCardItem> = emptyList(),
    val error: String? = null
) {
    val hasData: Boolean
        get() = title?.isNotBlank() == true
}