package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Model Drama — HANYA untuk API https://www.sankavollerei.web.id
 * Donghub + Donghua. Field JSON mengikuti respons ASLI API (poster, slug, dst).
 *
 * Catatan: kedua sumber (donghua & donghub) punya struktur berbeda, jadi
 * dipakai model "longgar" (semua field nullable) + parser manual di ViewModel
 * untuk memetakan ke DramaItem/DramaEpisode yang seragam.
 */

@JsonClass(generateAdapter = true)
data class DramaItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "intro") val intro: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "chapterCount") val chapterCount: Int? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "current_episode") val currentEpisode: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "url") val url: String? = null,
    // sumber: "donghua" atau "donghub"
    @Json(name = "source") val source: String? = null
)

@JsonClass(generateAdapter = true)
data class DramaEpisode(
    @Json(name = "index") val index: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "unlock") val unlock: Boolean? = null,
    @Json(name = "mp4") val mp4: String? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null,
    // slug episode untuk fetch stream
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "href") val href: String? = null,
    @Json(name = "episode_number") val episodeNumber: String? = null
)

/** Detail drama gabungan (donghua / donghub). */
data class DramaDetail(
    val item: DramaItem,
    val episodes: List<DramaEpisode>
)

/** Server streaming hasil resolve embed -> URL m3u8/mp4 langsung. */
data class DramaStream(
    val name: String,
    val url: String
)