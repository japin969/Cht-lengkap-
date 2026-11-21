package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KomikListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "status") val status: Int? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "totalPages") val totalPages: Int? = null,
    @Json(name = "hasNext") val hasNext: Boolean? = null,
    @Json(name = "nextPage") val nextPage: String? = null,
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
    val cleanTitle: String
        get() = title?.replace("Komik ", "")?.trim() ?: ""

    val resolvedSlug: String
        get() = slug ?: link?.let { extractSlugFromLink(it) } ?: ""

    private fun extractSlugFromLink(link: String): String {
        return link.split("/").filter { it.isNotBlank() }.lastOrNull() ?: ""
    }
}

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