package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ═══════════════════════════════════════════
// DONGHUA MODELS
// ═══════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class DonghuaHomeResponse(
    @Json(name = "data") val data: DonghuaHomeData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaHomeData(
    @Json(name = "ongoing") val ongoing: List<DonghuaItem>? = null,
    @Json(name = "completed") val completed: List<DonghuaItem>? = null,
    @Json(name = "latest") val latest: List<DonghuaItem>? = null,
    @Json(name = "popular") val popular: List<DonghuaItem>? = null,
    @Json(name = "schedule") val schedule: Map<String, List<DonghuaItem>>? = null,
    @Json(name = "recommend") val recommend: List<DonghuaItem>? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaListResponse(
    @Json(name = "data") val data: DonghuaListData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaListData(
    @Json(name = "list") val list: List<DonghuaItem>? = null,
    @Json(name = "pagination") val pagination: DonghuaPagination? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaPagination(
    @Json(name = "current_page") val currentPage: Int? = null,
    @Json(name = "last_page") val lastPage: Int? = null,
    @Json(name = "per_page") val perPage: Int? = null,
    @Json(name = "total") val total: Int? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaScheduleResponse(
    @Json(name = "data") val data: Map<String, List<DonghuaItem>>? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaGenresResponse(
    @Json(name = "data") val data: List<DonghuaGenre>? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaGenre(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "count") val count: Int? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaSearchResponse(
    @Json(name = "data") val data: DonghuaSearchData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaSearchData(
    @Json(name = "list") val list: List<DonghuaItem>? = null,
    @Json(name = "pagination") val pagination: DonghuaPagination? = null,
    @Json(name = "keyword") val keyword: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaItem(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "score") val score: Double? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "total_episode") val totalEpisode: Int? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "season") val season: String? = null,
    @Json(name = "year") val year: Int? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaDetailResponse(
    @Json(name = "data") val data: DonghuaDetailData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaDetailData(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "score") val score: Double? = null,
    @Json(name = "total_episode") val totalEpisode: Int? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "genres") val genres: List<DonghuaGenre>? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "season") val season: String? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "episodes") val episodes: List<DonghuaEpisode>? = null,
    @Json(name = "recommend") val recommend: List<DonghuaItem>? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaEpisode(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "duration") val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaEpisodeResponse(
    @Json(name = "data") val data: DonghuaStreamData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaStreamData(
    @Json(name = "stream_url") val streamUrl: String? = null,
    @Json(name = "backup_urls") val backupUrls: List<String>? = null,
    @Json(name = "headers") val headers: Map<String, String>? = null,
    @Json(name = "referer") val referer: String? = null
)

// ═══════════════════════════════════════════
// DONGHUB MODELS
// ═══════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class DonghubHomeResponse(
    @Json(name = "data") val data: DonghubHomeData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubHomeData(
    @Json(name = "featured") val featured: List<DonghubItem>? = null,
    @Json(name = "latest") val latest: List<DonghubItem>? = null,
    @Json(name = "popular") val popular: List<DonghubItem>? = null,
    @Json(name = "movies") val movies: List<DonghubItem>? = null,
    @Json(name = "schedule") val schedule: Map<String, List<DonghubItem>>? = null
)

@JsonClass(generateAdapter = true)
data class DonghubListResponse(
    @Json(name = "data") val data: DonghubListData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubListData(
    @Json(name = "list") val list: List<DonghubItem>? = null,
    @Json(name = "pagination") val pagination: DonghuaPagination? = null
)

@JsonClass(generateAdapter = true)
data class DonghubScheduleResponse(
    @Json(name = "data") val data: Map<String, List<DonghubItem>>? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubSearchResponse(
    @Json(name = "data") val data: DonghubSearchData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubSearchData(
    @Json(name = "list") val list: List<DonghubItem>? = null,
    @Json(name = "pagination") val pagination: DonghuaPagination? = null,
    @Json(name = "query") val query: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubItem(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "score") val score: Double? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "total_episode") val totalEpisode: Int? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "duration") val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubDetailResponse(
    @Json(name = "data") val data: DonghubDetailData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubDetailData(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "score") val score: Double? = null,
    @Json(name = "total_episode") val totalEpisode: Int? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "genres") val genres: List<DonghuaGenre>? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "episodes") val episodes: List<DonghubEpisode>? = null
)

@JsonClass(generateAdapter = true)
data class DonghubEpisode(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubEpisodeResponse(
    @Json(name = "data") val data: DonghubStreamData? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubStreamData(
    @Json(name = "stream_url") val streamUrl: String? = null,
    @Json(name = "backup_urls") val backupUrls: List<String>? = null,
    @Json(name = "headers") val headers: Map<String, String>? = null,
    @Json(name = "referer") val referer: String? = null
)

// ═══════════════════════════════════════════
// DOMAIN MODELS (for UI)
// ═══════════════════════════════════════════

data class DonghuaCardItem(
    val id: String,
    val title: String,
    val poster: String?,
    val episode: String?,
    val score: Double?,
    val type: String?,
    val status: String?,
    val source: DonghuaSource
) {
    companion object {
        fun fromDonghua(item: DonghuaItem, source: DonghuaSource): DonghuaCardItem {
            return DonghuaCardItem(
                id = item.id?.toString() ?: item.slug ?: "",
                title = item.title ?: "",
                poster = item.poster ?: item.cover ?: item.thumbnail,
                episode = item.episode ?: item.totalEpisode?.toString(),
                score = item.score,
                type = item.type,
                status = item.status,
                source = source
            )
        }

        fun fromDonghub(item: DonghubItem, source: DonghuaSource): DonghuaCardItem {
            return DonghuaCardItem(
                id = item.id?.toString() ?: item.slug ?: "",
                title = item.title ?: "",
                poster = item.poster ?: item.cover ?: item.thumbnail,
                episode = item.episode ?: item.totalEpisode?.toString(),
                score = item.score,
                type = item.type,
                status = item.status,
                source = source
            )
        }
    }
}

enum class DonghuaSource {
    DONGHUA,
    DONGHUB
}

data class DonghuaEpisodeItem(
    val id: String,
    val title: String,
    val slug: String,
    val episode: String,
    val releaseDate: String?
)

data class DonghuaStreamUrl(
    val streamUrl: String?,
    val backupUrls: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val referer: String?
)