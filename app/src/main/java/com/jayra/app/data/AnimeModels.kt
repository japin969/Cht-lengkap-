package com.jayra.app.data

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnimeSearchResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimeSearchData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeSearchData(
    @Json(name = "query") val query: String? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "results") val results: List<AnimeSearchItem>? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class AnimeSearchItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "latestEpisode") val latestEpisode: String? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "time") val time: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "rating") val rating: Double? = null
) {
    val displayBadge: String?
        get() = latestEpisode?.takeIf { it.isNotBlank() }
            ?: episode?.takeIf { it.isNotBlank() }
}

@JsonClass(generateAdapter = true)
data class AnimeDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimeDetailData? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class AnimeDetailData(
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "sinopsis") val sinopsis: String? = null,
    @Json(name = "synopsis") val synopsisAlt: String? = null,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "totalEpisodes") val totalEpisodes: Int? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "info") val info: Map<String, String>? = null,
    @Json(name = "episodes") val episodes: List<AnimeEpisode>? = null,
    @Json(name = "url") val url: String? = null
) {
    val displaySynopsis: String
        get() = sinopsis?.takeIf { it.isNotBlank() }
            ?: synopsisAlt?.takeIf { it.isNotBlank() }
            ?: "Sinopsis belum tersedia."

    val displayEpisodeCount: Int
        get() = totalEpisodes?.takeIf { it > 0 }
            ?: episodes?.size?.takeIf { it > 0 }
            ?: info?.get("Episode")?.toIntOrNull()
            ?: 0
}

@Immutable
@JsonClass(generateAdapter = true)
data class AnimeEpisode(
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "episodeUrl") val episodeUrl: String? = null,
    @Json(name = "date") val date: String? = null
)

@Keep
data class AnimeStreamResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimeStreamData? = null
)

@Keep
data class AnimeStreamData(
    @Json(name = "streamUrl") val streamUrl: String? = null,
    @Json(name = "alternativeLinks") val alternativeLinks: List<Any>? = null,
    @Json(name = "episodeInfo") val episodeInfo: AnimeEpisodeInfo? = null
) {
    fun parsedServers(): List<AnimeStreamServer> {
        val list = mutableListOf<AnimeStreamServer>()
        streamUrl?.takeIf { it.isNotBlank() }?.let { url ->
            list.add(AnimeStreamServer(name = "Server Utama", quality = "HD", url = url))
        }
        alternativeLinks?.forEachIndexed { idx, raw ->
            when (raw) {
                is String -> {
                    if (raw.isNotBlank() && raw != streamUrl) {
                        list.add(AnimeStreamServer(name = "Mirror ${idx + 1}", url = raw))
                    }
                }
                is Map<*, *> -> {
                    val u = (raw["url"] ?: raw["link"] ?: raw["streamUrl"])?.toString()?.trim()
                    if (!u.isNullOrBlank() && u != streamUrl) {
                        val n = (raw["name"] ?: raw["server"] ?: raw["label"])?.toString()?.trim()
                            ?: "Mirror ${idx + 1}"
                        val q = raw["quality"]?.toString()?.trim()
                        list.add(AnimeStreamServer(name = n, quality = q, url = u))
                    }
                }
            }
        }
        return list.distinctBy { it.url }
    }
}

@Immutable
data class AnimeStreamServer(
    val name: String,
    val quality: String? = null,
    val url: String
)

@Keep
@JsonClass(generateAdapter = true)
data class AnimeEpisodeInfo(
    @Json(name = "title") val title: String? = null,
    @Json(name = "heading") val heading: String? = null
)

@JsonClass(generateAdapter = true)
data class AnimeListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimeListData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeListData(
    @Json(name = "page") val page: Int? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "animeList") val animeList: List<AnimeSearchItem>? = null,
    @Json(name = "schedule") val schedule: Map<String, List<AnimeSearchItem>>? = null
)
