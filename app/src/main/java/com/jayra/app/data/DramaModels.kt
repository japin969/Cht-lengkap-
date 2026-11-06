package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DramaHomeResponse(
    @Json(name = "ok") val ok: Boolean? = null,
    @Json(name = "featured") val featured: List<DramaItem>? = null,
    @Json(name = "sections") val sections: List<DramaSection>? = null
)

@JsonClass(generateAdapter = true)
data class DramaSection(
    @Json(name = "name") val name: String? = null,
    @Json(name = "items") val items: List<DramaItem>? = null
)

@JsonClass(generateAdapter = true)
data class DramaItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "intro") val intro: String? = null,
    @Json(name = "chapterCount") val chapterCount: Int? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class DramaDetailResponse(
    @Json(name = "ok") val ok: Boolean? = null,
    @Json(name = "book") val book: DramaItem? = null,
    @Json(name = "episodes") val episodes: List<DramaEpisode>? = null
)

@JsonClass(generateAdapter = true)
data class DramaEpisode(
    @Json(name = "index") val index: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "unlock") val unlock: Boolean? = null,
    @Json(name = "mp4") val mp4: String? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null
)
