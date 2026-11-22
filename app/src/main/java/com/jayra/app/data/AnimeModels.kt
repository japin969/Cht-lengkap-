package com.jayra.app.data

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

@JsonClass(generateAdapter = true)
data class AnimeSearchItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "latestEpisode") val latestEpisode: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class AnimeDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimeDetailData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeDetailData(
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "sinopsis") val sinopsis: String? = null,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "totalEpisodes") val totalEpisodes: Int? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "info") val info: Map<String, String>? = null,
    @Json(name = "episodes") val episodes: List<AnimeEpisode>? = null
) {
    /** Alias agar UI lama yang memakai `synopsis` tetap valid. */
    val synopsis: String? get() = sinopsis
}

@JsonClass(generateAdapter = true)
data class AnimeEpisode(
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "episodeUrl") val episodeUrl: String? = null,
    @Json(name = "date") val date: String? = null
)

@JsonClass(generateAdapter = true)
data class AnimeStreamResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimeStreamData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeStreamData(
    @Json(name = "streamUrl") val streamUrl: String? = null,
    @Json(name = "alternativeLinks") val alternativeLinks: List<String>? = null,
    @Json(name = "episodeInfo") val episodeInfo: AnimeEpisodeInfo? = null
)

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
