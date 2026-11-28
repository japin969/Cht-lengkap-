package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ============================================================================
// BASE RESPONSE WRAPPERS
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiBaseResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: Any? = null
)

// Generic list response with pagination
@JsonClass(generateAdapter = true)
data class AnimeKompiListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiListData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiListData(
    @Json(name = "anime") val anime: List<AnimeKompiItem>? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "totalPage") val totalPage: Int? = null,
    @Json(name = "totalItems") val totalItems: Int? = null
)

// ============================================================================
// CORE ANIME ITEM (used in lists)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiItem(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "backdrop") val backdrop: String? = null,
    @Json(name = "type") val type: String? = null,           // TV, Movie, OVA, ONA, Special, Batch, Live-Action, Donghua, Tokusatsu
    @Json(name = "status") val status: String? = null,       // Ongoing, Completed, Upcoming
    @Json(name = "score") val score: String? = null,         // Rating like "8.5"
    @Json(name = "episode") val episode: String? = null,     // Current episode "Ep 24" or "24"
    @Json(name = "totalEpisode") val totalEpisode: String? = null,
    @Json(name = "duration") val duration: String? = null,   // "24 min" or "1h 45m"
    @Json(name = "releaseDate") val releaseDate: String? = null,
    @Json(name = "season") val season: String? = null,       // "Winter 2024"
    @Json(name = "year") val year: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "studios") val studios: List<String>? = null,
    @Json(name = "isNew") val isNew: Boolean? = null,        // For NEW badge
    @Json(name = "rank") val rank: Int? = null,              // For Top 10
    @Json(name = "schedule") val schedule: AnimeKompiScheduleItem? = null, // For schedule items
)

// ============================================================================
// HOME RESPONSE (Endpoint 1)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiHomeResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiHomeData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiHomeData(
    @Json(name = "hero") val hero: List<AnimeKompiItem>? = null,           // 3-5 items for hero banner
    @Json(name = "trending") val trending: List<AnimeKompiItem>? = null,   // Trending Now row
    @Json(name = "terbaru") val terbaru: List<AnimeKompiItem>? = null,     // New Releases
    @Json(name = "popular") val popular: List<AnimeKompiItem>? = null,     // Popular row
    @Json(name = "ongoing") val ongoing: List<AnimeKompiItem>? = null,     // Ongoing row
    @Json(name = "completed") val completed: List<AnimeKompiItem>? = null, // Completed row
    @Json(name = "batch") val batch: List<AnimeKompiItem>? = null,         // Batch row
    @Json(name = "movie") val movie: List<AnimeKompiItem>? = null,         // Movie row
    @Json(name = "liveAction") val liveAction: List<AnimeKompiItem>? = null, // Live-Action row
    @Json(name = "donghua") val donghua: List<AnimeKompiItem>? = null,     // Donghua row
    @Json(name = "tokusatsu") val tokusatsu: List<AnimeKompiItem>? = null, // Tokusatsu row
    @Json(name = "upcoming") val upcoming: List<AnimeKompiItem>? = null,   // Upcoming row
)

// ============================================================================
// SCHEDULE RESPONSE (Endpoint 9)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiScheduleResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiScheduleData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiScheduleData(
    @Json(name = "schedule") val schedule: Map<String, List<AnimeKompiScheduleItem>>? = null
    // Key: "Senin", "Selasa", etc. Value: list of anime for that day
)

@JsonClass(generateAdapter = true)
data class AnimeKompiScheduleItem(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "time") val time: String? = null,           // "17:00"
    @Json(name = "episode") val episode: String? = null,     // "Ep 5"
    @Json(name = "type") val type: String? = null,
)

// ============================================================================
// A-Z RESPONSE (Endpoint 11)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiAzResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiAzData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiAzData(
    @Json(name = "az") val az: Map<String, List<AnimeKompiItem>>? = null
    // Key: "A", "B", "C"... Value: list of anime starting with that letter
)

// ============================================================================
// SEARCH SUGGEST RESPONSE (Endpoint 13)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiSuggestResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: List<AnimeKompiSuggestItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiSuggestItem(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "score") val score: String? = null,
)

// ============================================================================
// FILTER LIST RESPONSE (Endpoint 15) - For filter bottom sheet options
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiFilterListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiFilterListData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiFilterListData(
    @Json(name = "genres") val genres: List<AnimeKompiFilterOption>? = null,
    @Json(name = "seasons") val seasons: List<AnimeKompiFilterOption>? = null,
    @Json(name = "studios") val studios: List<AnimeKompiFilterOption>? = null,
    @Json(name = "status") val status: List<AnimeKompiFilterOption>? = null,
    @Json(name = "types") val types: List<AnimeKompiFilterOption>? = null,
    @Json(name = "orders") val orders: List<AnimeKompiFilterOption>? = null,
)

@JsonClass(generateAdapter = true)
data class AnimeKompiFilterOption(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "count") val count: Int? = null,
)

// ============================================================================
// GENRES LIST (Endpoint 16)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiGenresResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: List<AnimeKompiGenreItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiGenreItem(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "count") val count: Int? = null,
    @Json(name = "poster") val poster: String? = null,
)

// ============================================================================
// SEASONS LIST (Endpoint 17)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiSeasonsResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: List<AnimeKompiSeasonItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiSeasonItem(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "name") val name: String? = null,           // "Winter 2024"
    @Json(name = "year") val year: String? = null,
    @Json(name = "season") val season: String? = null,       // "winter", "spring", "summer", "fall"
    @Json(name = "count") val count: Int? = null,
    @Json(name = "poster") val poster: String? = null,
)

// ============================================================================
// STUDIOS LIST (Endpoint 18)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiStudiosResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: List<AnimeKompiStudioItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiStudioItem(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "count") val count: Int? = null,
    @Json(name = "poster") val poster: String? = null,
)

// ============================================================================
// STATUS LIST (Endpoint 19)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiStatusResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: List<AnimeKompiStatusItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiStatusItem(
    @Json(name = "slug") val slug: String? = null,           // "ongoing", "completed", "upcoming"
    @Json(name = "name") val name: String? = null,           // "Ongoing", "Completed", "Upcoming"
    @Json(name = "count") val count: Int? = null,
)

// ============================================================================
// TYPES LIST (Endpoint 20)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiTypesResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: List<AnimeKompiTypeItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiTypeItem(
    @Json(name = "slug") val slug: String? = null,           // "tv", "movie", "ova", "ona", "special", "batch", "live-action", "donghua", "tokusatsu"
    @Json(name = "name") val name: String? = null,           // "TV Series", "Movie", "OVA", etc.
    @Json(name = "count") val count: Int? = null,
)

// ============================================================================
// ORDERS LIST (Endpoint 21)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiOrdersResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: List<AnimeKompiOrderItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiOrderItem(
    @Json(name = "slug") val slug: String? = null,           // "popular", "newest", "top-rated", "az", "za"
    @Json(name = "name") val name: String? = null,           // "Popular", "Newest", "Top Rated", "A-Z", "Z-A"
)

// ============================================================================
// GENRE DETAIL (Endpoint 22 & 23)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiGenreDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiGenreDetailData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiGenreDetailData(
    @Json(name = "genre") val genre: AnimeKompiGenreItem? = null,
    @Json(name = "anime") val anime: List<AnimeKompiItem>? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "totalPage") val totalPage: Int? = null,
)

// ============================================================================
// ANIME DETAIL (Endpoint 29)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiDetailData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiDetailData(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "alternativeTitle") val alternativeTitle: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "backdrop") val backdrop: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "score") val score: String? = null,
    @Json(name = "totalEpisode") val totalEpisode: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null,
    @Json(name = "endDate") val endDate: String? = null,
    @Json(name = "season") val season: String? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "studios") val studios: List<String>? = null,
    @Json(name = "producers") val producers: List<String>? = null,
    @Json(name = "licensors") val licensors: List<String>? = null,
    @Json(name = "rating") val rating: String? = null,       // Age rating like "TV-14", "PG-13"
    @Json(name = "sub") val sub: String? = null,             // "Indonesia", "English"
    @Json(name = "dub") val dub: String? = null,
    @Json(name = "episodes") val episodes: List<AnimeKompiEpisodeItem>? = null,
    @Json(name = "recommendations") val recommendations: List<AnimeKompiItem>? = null, // More Like This
)

// Episode item in detail
@JsonClass(generateAdapter = true)
data class AnimeKompiEpisodeItem(
    @Json(name = "slug") val slug: String? = null,           // Episode slug for /episode/:slug
    @Json(name = "title") val title: String? = null,
    @Json(name = "episode") val episode: String? = null,     // "1", "2", "Episode 1"
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "isFiller") val isFiller: Boolean? = null,
    @Json(name = "isUpcoming") val isUpcoming: Boolean? = null,
)

// ============================================================================
// TOOLTIP (Endpoint 30)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiTooltipResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiTooltipData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiTooltipData(
    @Json(name = "title") val title: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "score") val score: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "episode") val episode: String? = null,
)

// ============================================================================
// EPISODE DETAIL (Endpoint 31) - For Player
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiEpisodeResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiEpisodeData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiEpisodeData(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "animeSlug") val animeSlug: String? = null,
    @Json(name = "animeTitle") val animeTitle: String? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null,        // Main video URL (m3u8/mp4)
    @Json(name = "videoType") val videoType: String? = null,     // "m3u8", "mp4"
    @Json(name = "subtitles") val subtitles: List<AnimeKompiSubtitle>? = null,
    @Json(name = "audio") val audio: List<AnimeKompiAudio>? = null,
    @Json(name = "intro") val intro: AnimeKompiSkipSegment? = null,  // Skip intro timestamps
    @Json(name = "outro") val outro: AnimeKompiSkipSegment? = null,  // Skip outro timestamps
    @Json(name = "nextEpisode") val nextEpisode: AnimeKompiNextEpisode? = null,
    @Json(name = "prevEpisode") val prevEpisode: AnimeKompiNextEpisode? = null,
)

@JsonClass(generateAdapter = true)
data class AnimeKompiSubtitle(
    @Json(name = "lang") val lang: String? = null,         // "id", "en"
    @Json(name = "label") val label: String? = null,       // "Indonesia", "English"
    @Json(name = "url") val url: String? = null,
    @Json(name = "default") val isDefault: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class AnimeKompiAudio(
    @Json(name = "lang") val lang: String? = null,
    @Json(name = "label") val label: String? = null,       // "Sub", "Dub"
    @Json(name = "url") val url: String? = null,
    @Json(name = "default") val isDefault: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class AnimeKompiSkipSegment(
    @Json(name = "start") val start: Int? = null,          // Start time in seconds
    @Json(name = "end") val end: Int? = null,              // End time in seconds
)

@JsonClass(generateAdapter = true)
data class AnimeKompiNextEpisode(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "episode") val episode: String? = null,
)

// ============================================================================
// EPISODE MIRRORS (Endpoint 32)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiMirrorsResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: List<AnimeKompiMirror>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiMirror(
    @Json(name = "name") val name: String? = null,         // "Mirror 1", "StreamSB", "Voe"
    @Json(name = "url") val url: String? = null,
    @Json(name = "type") val type: String? = null,         // "m3u8", "mp4"
    @Json(name = "quality") val quality: String? = null,   // "1080p", "720p", "480p", "360p"
)

// ============================================================================
// DANMAKU (Endpoint 33)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiDanmakuResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: List<AnimeKompiDanmaku>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiDanmaku(
    @Json(name = "time") val time: Float? = null,          // Time in seconds
    @Json(name = "text") val text: String? = null,
    @Json(name = "color") val color: String? = null,
    @Json(name = "type") val type: Int? = null,            // 1=scroll, 2=top, 3=bottom
    @Json(name = "author") val author: String? = null,
)

// ============================================================================
// COMMENTS (Endpoint 34)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiCommentsResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiCommentsData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiCommentsData(
    @Json(name = "comments") val comments: List<AnimeKompiComment>? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "totalPage") val totalPage: Int? = null,
)

@JsonClass(generateAdapter = true)
data class AnimeKompiComment(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user") val user: AnimeKompiCommentUser? = null,
    @Json(name = "comment") val comment: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "likes") val likes: Int? = null,
    @Json(name = "replies") val replies: List<AnimeKompiComment>? = null,
)

@JsonClass(generateAdapter = true)
data class AnimeKompiCommentUser(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
)

// ============================================================================
// REACTIONS (Endpoint 35)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiReactionsResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiReactionsData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiReactionsData(
    @Json(name = "reactions") val reactions: List<AnimeKompiReaction>? = null,
    @Json(name = "total") val total: Int? = null,
)

@JsonClass(generateAdapter = true)
data class AnimeKompiReaction(
    @Json(name = "emoji") val emoji: String? = null,       // "👍", "❤️", "😂", etc.
    @Json(name = "count") val count: Int? = null,
    @Json(name = "userReacted") val userReacted: Boolean? = null,
)

// ============================================================================
// SETTINGS (Endpoint 36)
// ============================================================================

@JsonClass(generateAdapter = true)
data class AnimeKompiSettingsResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: AnimeKompiSettingsData? = null
)

@JsonClass(generateAdapter = true)
data class AnimeKompiSettingsData(
    @Json(name = "defaultQuality") val defaultQuality: String? = null,       // "auto", "1080p", "720p", etc.
    @Json(name = "defaultSubtitle") val defaultSubtitle: String? = null,     // "id", "en", "off"
    @Json(name = "defaultAudio") val defaultAudio: String? = null,           // "sub", "dub"
    @Json(name = "autoNext") val autoNext: Boolean? = null,
    @Json(name = "autoPlay") val autoPlay: Boolean? = null,
    @Json(name = "danmakuEnabled") val danmakuEnabled: Boolean? = null,
    @Json(name = "skipIntroEnabled") val skipIntroEnabled: Boolean? = null,
    @Json(name = "skipOutroEnabled") val skipOutroEnabled: Boolean? = null,
    @Json(name = "downloadQuality") val downloadQuality: String? = null,
    @Json(name = "availableQualities") val availableQualities: List<String>? = null, // ["360p", "480p", "720p", "1080p"]
)