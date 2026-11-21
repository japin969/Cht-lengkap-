package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KomikListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "hasNext") val hasNext: Boolean? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "results") val results: List<KomikItem>? = null
)

@JsonClass(generateAdapter = true)
data class KomikItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "latestChapter") val latestChapter: String? = null,
    @Json(name = "rating") val rating: String? = null
) {
    /** Dipakai UI lama yang menampilkan info terbaru. */
    val updatedAt: String?
        get() = latestChapter?.takeIf { it.isNotBlank() }
            ?: type?.takeIf { it.isNotBlank() }
}

@JsonClass(generateAdapter = true)
data class KomikDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "totalChapters") val totalChapters: Int? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "chapters") val chapters: List<KomikChapter>? = null
)

@JsonClass(generateAdapter = true)
data class KomikChapter(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "date") val date: String? = null
)

@JsonClass(generateAdapter = true)
data class KomikStreamResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "chapterSlug") val chapterSlug: String? = null,
    @Json(name = "chapterTitle") val chapterTitle: String? = null,
    @Json(name = "mangaTitle") val mangaTitle: String? = null,
    @Json(name = "totalPages") val totalPages: Int? = null,
    @Json(name = "pages") val pages: List<KomikPage>? = null
)

@JsonClass(generateAdapter = true)
data class KomikPage(
    @Json(name = "page") val page: Int? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "alt") val alt: String? = null
)
