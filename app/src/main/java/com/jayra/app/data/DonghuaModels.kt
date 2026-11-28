package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ============================================================
// Sumber data: SATU fitur "Donghua" dengan dua katalog
// (anichin/donghua + donghive/donghub). Model di bawah adalah
// SATU-SATUNYA model untuk fitur ini supaya tidak ada duplikat.
// ============================================================

enum class DonghuaSource(val label: String) {
    DONGHUA("Donghua"),
    DONGHUB("Donghub")
}

/** Item katalog yang sudah dinormalisasi dari dua skema JSON. */
data class DonghuaItem(
    val title: String,
    val slug: String,
    val poster: String?,
    val status: String?,
    val type: String?,
    val latestEpisode: String?,
    val source: DonghuaSource
)

/** Hasil halaman list (pagination diseragamkan). */
data class DonghuaPage(
    val items: List<DonghuaItem>,
    val page: Int,
    val hasNext: Boolean
)

data class DonghuaGenre(val name: String, val slug: String)

data class DonghuaEpisodeItem(
    val title: String,
    val slug: String,
    val number: String?,
    val releaseDate: String?,
    val source: DonghuaSource
)

data class DonghuaDetail(
    val title: String,
    val slug: String,
    val poster: String?,
    val synopsis: String?,
    val rating: String?,
    val status: String?,
    val type: String?,
    val studio: String?,
    val season: String?,
    val released: String?,
    val totalEpisodes: String?,
    val genres: List<DonghuaGenre>,
    val episodes: List<DonghuaEpisodeItem>,
    val recommendations: List<DonghuaItem>,
    val source: DonghuaSource
)

data class DonghuaServer(val name: String, val url: String)

data class DonghuaEpisode(
    val title: String,
    val slug: String,
    val source: DonghuaSource,
    val servers: List<DonghuaServer>,
    val prevSlug: String?,
    val nextSlug: String?
) {
    /**
     * Hanya URL yang benar-benar file media (m3u8/mp4) yang bisa
     * diputar player native. Endpoint ini juga mengirim halaman
     * embed (ok.ru, dailymotion, dst) yang TIDAK memakai WebView.
     */
    val playableUrl: String?
        get() = servers.firstOrNull { isDirectMedia(it.url) }?.url

    companion object {
        fun isDirectMedia(url: String?): Boolean {
            val u = url?.lowercase() ?: return false
            if (!(u.startsWith("http://") || u.startsWith("https://"))) return false
            return u.contains(".m3u8") || u.contains(".mp4") || u.contains(".mkv") || u.contains(".webm")
        }
    }
}

/** Home gabungan: section dipakai bersama oleh kedua katalog. */
data class DonghuaHome(
    val source: DonghuaSource,
    val hero: List<DonghuaItem>,
    val latest: List<DonghuaItem>,
    val ongoing: List<DonghuaItem>,
    val completed: List<DonghuaItem>
)

// ============================================================
// DTO mentah (persis mengikuti JSON endpoint, tanpa field karangan)
// ============================================================

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "sub") val sub: String? = null,
    @Json(name = "current_episode") val currentEpisode: String? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "href") val href: String? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaGenre(
    @Json(name = "name") val name: String? = null,
    @Json(name = "slug") val slug: String? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaEpisode(
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "episode_number") val episodeNumber: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "href") val href: String? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoPagination(
    @Json(name = "current_page") val currentPage: Int? = null,
    @Json(name = "total_pages") val totalPages: Int? = null,
    @Json(name = "has_next_page") val hasNextPage: Boolean? = null,
    @Json(name = "has_next") val hasNext: Boolean? = null,
    @Json(name = "has_prev") val hasPrev: Boolean? = null
)

// ---------- Donghua (anichin) ----------

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaHome(
    @Json(name = "latest_release") val latestRelease: List<DtoDonghuaItem>? = null,
    @Json(name = "completed_donghua") val completedDonghua: List<DtoDonghuaItem>? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaList(
    @Json(name = "ongoing_donghua") val ongoingDonghua: List<DtoDonghuaItem>? = null,
    @Json(name = "latest_donghua") val latestDonghua: List<DtoDonghuaItem>? = null,
    @Json(name = "completed_donghua") val completedDonghua: List<DtoDonghuaItem>? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaAz(
    @Json(name = "pagination") val pagination: DtoPagination? = null,
    @Json(name = "donghua_list") val donghuaList: List<DtoDonghuaItem>? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaData(
    @Json(name = "data") val data: List<DtoDonghuaItem>? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaGenreData(
    @Json(name = "data") val data: List<DtoDonghuaGenre>? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaDetail(
    @Json(name = "title") val title: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "studio") val studio: String? = null,
    @Json(name = "season") val season: String? = null,
    @Json(name = "released") val released: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "episodes_count") val episodesCount: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "genres") val genres: List<DtoDonghuaGenre>? = null,
    @Json(name = "episodes_list") val episodesList: List<DtoDonghuaEpisode>? = null,
    @Json(name = "recommendations") val recommendations: List<DtoDonghuaItem>? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaServer(
    @Json(name = "name") val name: String? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaStreaming(
    @Json(name = "servers") val servers: List<DtoDonghuaServer>? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaNav(
    @Json(name = "previous_episode") val previous: DtoDonghuaEpisode? = null,
    @Json(name = "next_episode") val next: DtoDonghuaEpisode? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghuaEpisodeData(
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "streaming") val streaming: DtoDonghuaStreaming? = null,
    @Json(name = "navigation") val navigation: DtoDonghuaNav? = null
)

// ---------- Donghub (donghive) ----------

@JsonClass(generateAdapter = true)
internal data class DtoDonghubHomeData(
    @Json(name = "slider") val slider: List<DtoDonghuaItem>? = null,
    @Json(name = "popular") val popular: List<DtoDonghuaItem>? = null,
    @Json(name = "latest") val latest: List<DtoDonghuaItem>? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghubHome(
    @Json(name = "data") val data: DtoDonghubHomeData? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghubList(
    @Json(name = "data") val data: List<DtoDonghuaItem>? = null,
    @Json(name = "pagination") val pagination: DtoPagination? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghubInfo(
    @Json(name = "status") val status: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "studio") val studio: String? = null,
    @Json(name = "released") val released: String? = null,
    @Json(name = "duration") val duration: String? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghubDetailData(
    @Json(name = "title") val title: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "info") val info: DtoDonghubInfo? = null,
    @Json(name = "genres") val genres: List<DtoDonghuaGenre>? = null,
    @Json(name = "episodes") val episodes: List<DtoDonghuaEpisode>? = null,
    @Json(name = "recommendations") val recommendations: List<DtoDonghuaItem>? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghubDetail(
    @Json(name = "data") val data: DtoDonghubDetailData? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghubStream(
    @Json(name = "server") val server: String? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghubNav(
    @Json(name = "prev_slug") val prevSlug: String? = null,
    @Json(name = "next_slug") val nextSlug: String? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghubEpisodeData(
    @Json(name = "title") val title: String? = null,
    @Json(name = "streams") val streams: List<DtoDonghubStream>? = null,
    @Json(name = "navigation") val navigation: DtoDonghubNav? = null
)

@JsonClass(generateAdapter = true)
internal data class DtoDonghubEpisode(
    @Json(name = "data") val data: DtoDonghubEpisodeData? = null
)

// ---------- Mapper (JSON -> model) ----------

internal fun DtoDonghuaItem.toModel(source: DonghuaSource = DonghuaSource.DONGHUA): DonghuaItem? {
    val t = cleanDonghuaTitle(title) ?: cleanDonghuaTitle(episode) ?: return null
    val s = slug?.trim().orEmpty()
    return DonghuaItem(
        title = t,
        slug = s,
        poster = poster?.trim()?.takeIf { it.isNotEmpty() },
        status = status?.trim()?.takeIf { it.isNotEmpty() },
        type = type?.trim()?.takeIf { it.isNotEmpty() },
        latestEpisode = currentEpisode?.trim()?.takeIf { it.isNotEmpty() }
            ?: episode?.trim()?.takeIf { it.isNotEmpty() },
        source = source
    )
}

/** Judul donghub sering mengandung tab + judul episode duplikat. */
internal fun cleanDonghuaTitle(raw: String?): String? {
    val text = raw?.replace('\u00A0', ' ')?.trim() ?: return null
    if (text.isEmpty()) return null
    val parts = text.split(Regex("\\s*\\t+\\s*")).map { it.trim() }.filter { it.isNotEmpty() }
    return (parts.lastOrNull() ?: text).trim().ifEmpty { null }
}

internal fun DtoDonghuaGenre.toModel(): DonghuaGenre? {
    val n = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return DonghuaGenre(name = n, slug = slug?.trim().orEmpty())
}

internal fun DtoDonghuaEpisode.toModel(source: DonghuaSource): DonghuaEpisodeItem? {
    val s = slug?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val t = cleanDonghuaTitle(title) ?: cleanDonghuaTitle(episode) ?: "Episode ${number ?: ""}"
    return DonghuaEpisodeItem(
        title = t,
        slug = s,
        number = episodeNumber?.trim()?.takeIf { it.isNotEmpty() },
        releaseDate = (releaseDate ?: date)?.trim()?.takeIf { it.isNotEmpty() },
        source = source
    )
}

/** Buang item dengan slug sama / judul sama supaya tidak ada duplikat. */
internal fun List<DonghuaItem>.distinctBySlug(): List<DonghuaItem> {
    val seen = HashSet<String>()
    val out = ArrayList<DonghuaItem>(size)
    for (item in this) {
        val key = item.slug.ifBlank { item.title.lowercase() }
        if (item.slug.isBlank() && item.poster == null) continue
        if (seen.add(key)) out.add(item)
    }
    return out
}
