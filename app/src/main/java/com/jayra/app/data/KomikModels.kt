package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KomikListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "results") val results: List<KomikItem>? = null
)

@JsonClass(generateAdapter = true)
data class KomikItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "rating") val rating: String? = null
)

@JsonClass(generateAdapter = true)
data class KomikDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "chapters") val chapters: List<KomikChapter>? = null
)

@JsonClass(generateAdapter = true)
data class KomikChapter(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "slug") val slug: String? = null
)

@JsonClass(generateAdapter = true)
data class KomikStreamResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "chapterTitle") val chapterTitle: String? = null,
    @Json(name = "pages") val pages: List<KomikPage>? = null
)

@JsonClass(generateAdapter = true)
data class KomikPage(
    @Json(name = "page") val page: Int? = null,
    @Json(name = "url") val url: String? = null
)
