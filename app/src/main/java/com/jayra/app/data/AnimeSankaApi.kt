package com.jayra.app.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * AnimeSankaApi — sumber anime tambahan: https://www.sankavollerei.web.id.
 * Endpoint /anime/* (home, schedule, genre, search, detail, episode, server, batch)
 * + /anime/nekopoi/*. SSL valid, tanpa bypass.
 */
interface AnimeSankaApi {

    @GET("anime/home")
    suspend fun home(): Response<AnimeHomeResponse>

    @GET("anime/schedule")
    suspend fun schedule(): Response<AnimeScheduleResponse>

    @GET("anime/ongoing-anime")
    suspend fun ongoing(): Response<AnimeSectionResponse>

    @GET("anime/complete-anime")
    suspend fun complete(): Response<AnimeSectionResponse>

    @GET("anime/genre")
    suspend fun genres(): Response<AnimeGenreResponse>

    @GET("anime/genre/{slug}")
    suspend fun genre(@Path("slug") slug: String): Response<AnimeSectionResponse>

    @GET("anime/search/{keyword}")
    suspend fun search(@Path("keyword") keyword: String): Response<AnimeSectionResponse>

    @GET("anime/anime/{slug}")
    suspend fun detail(@Path("slug") slug: String): Response<AnimeDetailResponse2>

    @GET("anime/episode/{slug}")
    suspend fun episode(@Path("slug") slug: String): Response<AnimeEpisodeResponse2>

    // ── nekopoi ──
    @GET("anime/nekopoi/home")
    suspend fun nkHome(): Response<NkHomeResponse>

    @GET("anime/nekopoi/genres")
    suspend fun nkGenres(): Response<NkGenreResponse>

    @GET("anime/nekopoi/genre/{slug}")
    suspend fun nkGenre(@Path("slug") slug: String): Response<NkListResponse>

    @GET("anime/nekopoi/latest-hentai")
    suspend fun nkLatestHentai(): Response<NkListResponse>

    @GET("anime/nekopoi/detail/{slug}")
    suspend fun nkDetail(@Path("slug") slug: String): Response<NkDetailResponse>

    @GET("anime/nekopoi/episode/{slug}")
    suspend fun nkEpisode(@Path("slug") slug: String): Response<NkEpisodeResponse>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        fun create(): AnimeSankaApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", UA)
                            .header("Accept", "application/json")
                            .header("Referer", BASE_URL)
                            .build()
                    )
                }
                .build()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL).client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AnimeSankaApi::class.java)
        }
    }
}

// ── anime (otakudesu via sankavollerei) ──
@JsonClass(generateAdapter = true)
data class AnimeHomeResponse(
    val status: String? = null, val creator: String? = null, val data: AnimeHomeData? = null
)
@JsonClass(generateAdapter = true)
data class AnimeHomeData(
    val ongoing: AnimeSection? = null,
    val complete: AnimeSection? = null,
    val popular: AnimeSection? = null,
    val recommended: AnimeSection? = null
)
@JsonClass(generateAdapter = true)
data class AnimeSection(
    val href: String? = null, val animeList: List<AnimeSankaItem>? = null
)
@JsonClass(generateAdapter = true)
data class AnimeSectionResponse(
    val status: String? = null, val creator: String? = null, val data: AnimeSection? = null
)
@JsonClass(generateAdapter = true)
data class AnimeSankaItem(
    val title: String? = null, val poster: String? = null, val animeId: String? = null,
    val slug: String? = null, val episodes: Int? = null, val score: String? = null,
    val releaseDay: String? = null, val latestReleaseDate: String? = null,
    val lastReleaseDate: String? = null, val url: String? = null
)
@JsonClass(generateAdapter = true)
data class AnimeScheduleResponse(
    val status: String? = null, val creator: String? = null, val data: List<AnimeDay>? = null
)
@JsonClass(generateAdapter = true)
data class AnimeDay(val day: String? = null, val anime_list: List<AnimeSankaItem>? = null)
@JsonClass(generateAdapter = true)
data class AnimeGenreResponse(
    val status: String? = null, val creator: String? = null, val data: AnimeGenreData? = null
)
@JsonClass(generateAdapter = true)
data class AnimeGenreData(val genreList: List<AnimeGenreItem>? = null)
@JsonClass(generateAdapter = true)
data class AnimeGenreItem(
    val title: String? = null, val genreId: String? = null, val href: String? = null
)
@JsonClass(generateAdapter = true)
data class AnimeDetailResponse2(
    val status: String? = null, val creator: String? = null, val data: AnimeSankaDetail? = null
)
@JsonClass(generateAdapter = true)
data class AnimeSankaDetail(
    val title: String? = null, val poster: String? = null, val japanese: String? = null,
    val score: String? = null, val producers: String? = null, val type: String? = null,
    val status: String? = null, val episodes: Int? = null, val duration: String? = null,
    val aired: String? = null, val studios: String? = null, val synopsis: String? = null,
    val genreList: List<AnimeGenreItem>? = null,
    val episodeList: List<AnimeSankaEpisode>? = null,
    val recommendedAnimeList: List<AnimeSankaItem>? = null
)
@JsonClass(generateAdapter = true)
data class AnimeSankaEpisode(
    val title: String? = null, val slug: String? = null, val episodeId: String? = null,
    val eps: Int? = null, val date: String? = null
)
@JsonClass(generateAdapter = true)
data class AnimeEpisodeResponse2(
    val status: String? = null, val creator: String? = null, val data: AnimeEpisodeData2? = null
)
@JsonClass(generateAdapter = true)
data class AnimeEpisodeData2(
    val title: String? = null, val animeId: String? = null,
    val defaultStreamingUrl: String? = null,
    val hasPrevEpisode: Boolean? = null, val hasNextEpisode: Boolean? = null,
    val prevEpisode: AnimeSankaEpisode? = null, val nextEpisode: AnimeSankaEpisode? = null
)

// ── nekopoi ──
@JsonClass(generateAdapter = true)
data class NkHomeResponse(
    val status: String? = null, val creator: String? = null, val data: NkHomeData? = null
)
@JsonClass(generateAdapter = true)
data class NkHomeData(
    val recommended: List<NkItem>? = null,
    val latest: List<NkItem>? = null
)
@JsonClass(generateAdapter = true)
data class NkListResponse(
    val status: String? = null, val creator: String? = null, val data: List<NkItem>? = null
)
@JsonClass(generateAdapter = true)
data class NkItem(
    val title: String? = null, val slug: String? = null, val url: String? = null,
    val thumbnail: String? = null
)
@JsonClass(generateAdapter = true)
data class NkGenreResponse(
    val status: String? = null, val creator: String? = null, val data: List<NkGenre>? = null
)
@JsonClass(generateAdapter = true)
data class NkGenre(val name: String? = null, val slug: String? = null, val url: String? = null)
@JsonClass(generateAdapter = true)
data class NkDetailResponse(
    val status: String? = null, val creator: String? = null, val data: NkDetail? = null
)
@JsonClass(generateAdapter = true)
data class NkDetail(
    val title: String? = null, val slug: String? = null, val url: String? = null,
    val thumbnail: String? = null, val japan_name: String? = null,
    val synopsis: String? = null, val episodes: List<NkItem>? = null
)
@JsonClass(generateAdapter = true)
data class NkEpisodeResponse(
    val status: String? = null, val creator: String? = null, val data: NkEpisodeData? = null
)
@JsonClass(generateAdapter = true)
data class NkEpisodeData(
    val title: String? = null, val slug: String? = null,
    val streaming: List<NkServer>? = null, val download: List<NkServer>? = null
)
@JsonClass(generateAdapter = true)
data class NkServer(val name: String? = null, val url: String? = null)