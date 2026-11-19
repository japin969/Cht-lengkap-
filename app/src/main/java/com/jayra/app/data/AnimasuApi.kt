package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Animasu API (https://api.nexadev.my.id/animasu) — Anime streaming API.
 * Endpoints: search, detail, stream, ongoing, completed, popular, schedule.
 */
interface AnimasuApi {

    @GET("animasu")
    suspend fun search(
        @Query("search") query: String,
        @Query("page") page: Int = 1
    ): Response<AnimasuSearchResponse>

    @GET("animasu")
    suspend fun detail(
        @Query("details") url: String
    ): Response<AnimasuDetailResponse>

    @GET("animasu")
    suspend fun stream(
        @Query("stream") url: String
    ): Response<AnimasuStreamResponse>

    @GET("animasu")
    suspend fun ongoing(
        @Query("ongoing") ongoing: Int = 1,
        @Query("page") page: Int = 1
    ): Response<AnimasuListResponse>

    @GET("animasu")
    suspend fun completed(
        @Query("completed") completed: Int = 1,
        @Query("page") page: Int = 1
    ): Response<AnimasuListResponse>

    @GET("animasu")
    suspend fun popular(
        @Query("popular") popular: Int = 1,
        @Query("page") page: Int = 1
    ): Response<AnimasuListResponse>

    @GET("animasu")
    suspend fun schedule(
        @Query("schedule") schedule: Int = 1
    ): Response<AnimasuScheduleResponse>

    companion object {
        private const val BASE_URL = "https://api.nexadev.my.id/"

        fun create(): AnimasuApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Accept", "application/json")
                            .header("User-Agent", "JayraApp/1.0 (Android)")
                            .build()
                    )
                }
                .build()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AnimasuApi::class.java)
        }
    }
}

// ==================== SEARCH ====================
@JsonClass(generateAdapter = true)
data class AnimasuSearchResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimasuSearchData? = null
)

@JsonClass(generateAdapter = true)
data class AnimasuSearchData(
    @Json(name = "results") val results: List<AnimasuSearchItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimasuSearchItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "score") val score: String? = null
) {
    fun toAnimeSearchItem(): AnimeSearchItem = AnimeSearchItem(
        title = title ?: "Unknown",
        link = url ?: "",
        image = image ?: "",
        latestEpisode = episode ?: type ?: "",
        type = type
    )
}

// ==================== DETAIL ====================
@JsonClass(generateAdapter = true)
data class AnimasuDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimasuDetailData? = null
)

@JsonClass(generateAdapter = true)
data class AnimasuDetailData(
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "totalEpisodes") val totalEpisodes: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "info") val info: Map<String, String>? = null,
    @Json(name = "episodes") val episodes: List<AnimasuEpisode>? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null
) {
    fun toAnimeDetailData(): AnimeDetailData {
        val infoMap = buildMap<String, String> {
            status?.let { put("Status", it) }
            type?.let { put("Tipe", it) }
            releaseDate?.let { put("Rilis", it) }
            totalEpisodes?.let { put("Total Episode", it) }
            rating?.let { put("Rating", it) }
            info?.forEach { (k, v) -> put(k, v) }
        }
        return AnimeDetailData(
            title = title ?: "Unknown",
            image = image ?: "",
            synopsis = synopsis ?: "",
            rating = rating?.toDoubleOrNull(),
            totalEpisodes = totalEpisodes?.toIntOrNull(),
            genres = genres,
            info = infoMap,
            episodes = episodes?.map { it.toAnimeEpisode() } ?: emptyList()
        )
    }
}

@JsonClass(generateAdapter = true)
data class AnimasuEpisode(
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "episodeUrl") val episodeUrl: String? = null
) {
    fun toAnimeEpisode(): AnimeEpisode = AnimeEpisode(
        episode = episode ?: "1",
        title = title ?: "Episode $episode",
        episodeUrl = episodeUrl ?: url ?: ""
    )
}

// ==================== STREAM ====================
@JsonClass(generateAdapter = true)
data class AnimasuStreamResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimasuStreamData? = null
)

@JsonClass(generateAdapter = true)
data class AnimasuStreamData(
    @Json(name = "streamUrl") val streamUrl: String? = null,
    @Json(name = "iframeUrl") val iframeUrl: String? = null,
    @Json(name = "m3u8Url") val m3u8Url: String? = null,
    @Json(name = "alternativeLinks") val alternativeLinks: List<String>? = null,
    @Json(name = "servers") val servers: List<AnimasuServer>? = null
) {
    fun getBestStreamUrl(): String? {
        // Priority: m3u8 > streamUrl > iframeUrl > first alternative > first server
        return m3u8Url?.takeIf { it.isNotBlank() }
            ?: streamUrl?.takeIf { it.isNotBlank() }
            ?: iframeUrl?.takeIf { it.isNotBlank() }
            ?: alternativeLinks?.firstOrNull { it.isNotBlank() }
            ?: servers?.firstOrNull()?.embedUrl?.takeIf { it.isNotBlank() }
    }
}

@JsonClass(generateAdapter = true)
data class AnimasuServer(
    @Json(name = "name") val name: String? = null,
    @Json(name = "embedUrl") val embedUrl: String? = null,
    @Json(name = "m3u8Url") val m3u8Url: String? = null
)

// ==================== LIST (ongoing, completed, popular) ====================
@JsonClass(generateAdapter = true)
data class AnimasuListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimasuListData? = null
)

@JsonClass(generateAdapter = true)
data class AnimasuListData(
    @Json(name = "page") val page: Int? = null,
    @Json(name = "totalPages") val totalPages: Int? = null,
    @Json(name = "animeList") val animeList: List<AnimasuListItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimasuListItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "score") val score: String? = null
) {
    fun toAnimeSearchItem(): AnimeSearchItem = AnimeSearchItem(
        title = title ?: "Unknown",
        link = url ?: "",
        image = image ?: "",
        latestEpisode = episode ?: type ?: "",
        type = type
    )
}

// ==================== SCHEDULE ====================
@JsonClass(generateAdapter = true)
data class AnimasuScheduleResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: AnimasuScheduleData? = null
)

@JsonClass(generateAdapter = true)
data class AnimasuScheduleData(
    @Json(name = "schedule") val schedule: Map<String, List<AnimasuListItem>>? = null
)