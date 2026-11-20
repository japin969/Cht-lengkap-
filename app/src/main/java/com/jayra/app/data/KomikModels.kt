package com.jayra.app.data

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KomikHomeResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "sections") val sections: KomikHomeSections? = null
)

@JsonClass(generateAdapter = true)
data class KomikHomeSections(
    @Json(name = "popular") val popular: List<KomikItem>? = null,
    @Json(name = "latest") val latest: List<KomikItem>? = null
)

@JsonClass(generateAdapter = true)
data class KomikListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "totalPages") val totalPages: Int? = null,
    @Json(name = "hasNext") val hasNext: Boolean? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "results") val results: List<KomikItem>? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class KomikItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "latestChapter") val latestChapter: String? = null,
    @Json(name = "chapterLink") val chapterLink: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "rating") val rating: String? = null
) {
    val cleanTitle: String
        get() = title?.removePrefix("Komik ")?.trim().orEmpty().ifBlank { "Tanpa Judul" }

    val resolvedSlug: String
        get() = slug?.takeIf { it.isNotBlank() }
            ?: link?.trimEnd('/')?.substringAfterLast('/').orEmpty()
}

@Immutable
@JsonClass(generateAdapter = true)
data class KomikDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "alternativeTitle") val alternativeTitle: String? = null,
    @Json(name = "statusText") val statusText: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "authorName") val authorName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "totalChapters") val totalChapters: Int? = null,
    @Json(name = "chapters") val chapters: List<KomikChapter>? = null
) {
    val cleanTitle: String
        get() = title?.removePrefix("Komik ")?.trim().orEmpty().ifBlank { "Detail Komik" }
}

@Immutable
@JsonClass(generateAdapter = true)
data class KomikChapter(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "date") val date: String? = null
) {
    val resolvedSlug: String
        get() = slug?.takeIf { it.isNotBlank() }
            ?: link?.trimEnd('/')?.substringAfterLast('/').orEmpty()
}

@JsonClass(generateAdapter = true)
data class KomikStreamResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "chapterTitle") val chapterTitle: String? = null,
    @Json(name = "comicTitle") val comicTitle: String? = null,
    @Json(name = "comicSlug") val comicSlug: String? = null,
    @Json(name = "totalImages") val totalImages: Int? = null,
    @Json(name = "images") val images: List<String>? = null,
    @Json(name = "navigation") val navigation: KomikNavigation? = null
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() }
            ?: chapterTitle?.takeIf { it.isNotBlank() }
            ?: "Baca Komik"
}

@JsonClass(generateAdapter = true)
data class KomikNavigation(
    @Json(name = "prev") val prev: KomikNavTarget? = null,
    @Json(name = "next") val next: KomikNavTarget? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class KomikNavTarget(
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "link") val link: String? = null
) {
    val resolvedSlug: String
        get() = slug?.takeIf { it.isNotBlank() }
            ?: link?.trimEnd('/')?.substringAfterLast('/').orEmpty()
}
