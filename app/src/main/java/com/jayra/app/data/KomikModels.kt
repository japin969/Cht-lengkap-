package com.jayra.app.data

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Unified response for all komik endpoints (search / list / detail / stream).
 * Detail & stream return the same daftar-manga list shape — caller matches by slug.
 */
@JsonClass(generateAdapter = true)
data class KomikResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "status") val status: Int? = null,
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "query") val query: String? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "totalPages") val totalPages: Int? = null,
    @Json(name = "hasNext") val hasNext: Boolean? = null,
    @Json(name = "nextPage") val nextPage: String? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "results") val results: List<KomikItem>? = null
) {
    val items: List<KomikItem> = results.orEmpty()
}

@Immutable
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
        get() = title?.removePrefix("Komik ")?.trim().orEmpty().ifBlank { "Tanpa Judul" }

    val resolvedSlug: String
        get() = slug?.takeIf { it.isNotBlank() }
            ?: link?.trimEnd('/')?.substringAfterLast('/').orEmpty()
}
