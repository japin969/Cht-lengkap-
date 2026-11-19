package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NexadevAnimeListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: List<NexadevAnime>? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class NexadevAnime(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "latestEpisode") val latestEpisode: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "streamUrl") val streamUrl: String? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "episodes") val episodes: List<NexadevEpisode>? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "startDate") val startDate: String? = null,
    @Json(name = "totalEpisodes") val totalEpisodes: String? = null,
    @Json(name = "genres") val genres: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class NexadevEpisode(
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "episodeUrl") val episodeUrl: String? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class NexadevAnimeDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: NexadevAnime? = null,
    @Json(name = "message") val message: String? = null
)