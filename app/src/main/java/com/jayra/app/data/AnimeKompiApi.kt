package com.jayra.app.data

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * AnimeKompi API - 36 endpoints from https://www.sankavollerei.web.id/anime/animekompi/
 * Base URL: https://www.sankavollerei.web.id
 */
interface AnimeKompiApi {

    // 1. Home page data - Hero, Trending, etc.
    @GET("anime/animekompi/home")
    suspend fun getHome(): Response<AnimeKompiHomeResponse>

    // 2. Terbaru (New Releases)
    @GET("anime/animekompi/terbaru")
    suspend fun getTerbaru(): Response<AnimeKompiListResponse>

    // 3. Batch Anime
    @GET("anime/animekompi/batch")
    suspend fun getBatch(): Response<AnimeKompiListResponse>

    // 4. Movie Anime
    @GET("anime/animekompi/movie")
    suspend fun getMovie(): Response<AnimeKompiListResponse>

    // 5. Live-Action
    @GET("anime/animekompi/live-action")
    suspend fun getLiveAction(): Response<AnimeKompiListResponse>

    // 6. Donghua
    @GET("anime/animekompi/donghua")
    suspend fun getDonghua(): Response<AnimeKompiListResponse>

    // 7. Tokusatsu
    @GET("anime/animekompi/tokusatsu")
    suspend fun getTokusatsu(): Response<AnimeKompiListResponse>

    // 8. Upcoming
    @GET("anime/animekompi/upcoming")
    suspend fun getUpcoming(): Response<AnimeKompiListResponse>

    // 9. Schedule
    @GET("anime/animekompi/schedule")
    suspend fun getSchedule(): Response<AnimeKompiScheduleResponse>

    // 10. List Semua Anime
    @GET("anime/animekompi/list")
    suspend fun getList(@Query("page") page: Int = 1): Response<AnimeKompiListResponse>

    // 11. A-Z
    @GET("anime/animekompi/az")
    suspend fun getAz(): Response<AnimeKompiAzResponse>

    // 12. Search
    @GET("anime/animekompi/search")
    suspend fun search(@Query("q") query: String, @Query("page") page: Int = 1): Response<AnimeKompiListResponse>

    // 13. Search Suggest
    @GET("anime/animekompi/search/suggest")
    suspend fun searchSuggest(@Query("q") query: String): Response<AnimeKompiSuggestResponse>

    // 14. Filter
    @GET("anime/animekompi/filter")
    suspend fun filter(
        @Query("genre") genre: String? = null,
        @Query("season") season: String? = null,
        @Query("studio") studio: String? = null,
        @Query("status") status: String? = null,
        @Query("type") type: String? = null,
        @Query("order") order: String? = null,
        @Query("page") page: Int = 1
    ): Response<AnimeKompiListResponse>

    // 15. Filter List (for filter options)
    @GET("anime/animekompi/filterlist")
    suspend fun getFilterList(): Response<AnimeKompiFilterListResponse>

    // 16. Genres List
    @GET("anime/animekompi/genres")
    suspend fun getGenres(): Response<AnimeKompiGenresResponse>

    // 17. Seasons List
    @GET("anime/animekompi/seasons")
    suspend fun getSeasons(): Response<AnimeKompiSeasonsResponse>

    // 18. Studios List
    @GET("anime/animekompi/studios")
    suspend fun getStudios(): Response<AnimeKompiStudiosResponse>

    // 19. Status List
    @GET("anime/animekompi/status")
    suspend fun getStatus(): Response<AnimeKompiStatusResponse>

    // 20. Types List
    @GET("anime/animekompi/types")
    suspend fun getTypes(): Response<AnimeKompiTypesResponse>

    // 21. Orders List
    @GET("anime/animekompi/orders")
    suspend fun getOrders(): Response<AnimeKompiOrdersResponse>

    // 22. Genre Detail (basic)
    @GET("anime/animekompi/genre/{slug}")
    suspend fun getGenre(@Path("slug") slug: String, @Query("page") page: Int = 1): Response<AnimeKompiListResponse>

    // 23. Genre Detail Lengkap
    @GET("anime/animekompi/genre-detail/{slug}")
    suspend fun getGenreDetail(@Path("slug") slug: String): Response<AnimeKompiGenreDetailResponse>

    // 24. Season Detail
    @GET("anime/animekompi/season/{slug}")
    suspend fun getSeason(@Path("slug") slug: String, @Query("page") page: Int = 1): Response<AnimeKompiListResponse>

    // 25. Studio Detail
    @GET("anime/animekompi/studio/{slug}")
    suspend fun getStudio(@Path("slug") slug: String, @Query("page") page: Int = 1): Response<AnimeKompiListResponse>

    // 26. Status Detail
    @GET("anime/animekompi/status/{slug}")
    suspend fun getStatusDetail(@Path("slug") slug: String, @Query("page") page: Int = 1): Response<AnimeKompiListResponse>

    // 27. Type Detail
    @GET("anime/animekompi/type/{slug}")
    suspend fun getType(@Path("slug") slug: String, @Query("page") page: Int = 1): Response<AnimeKompiListResponse>

    // 28. Order Detail
    @GET("anime/animekompi/order/{slug}")
    suspend fun getOrder(@Path("slug") slug: String, @Query("page") page: Int = 1): Response<AnimeKompiListResponse>

    // 29. Anime Detail
    @GET("anime/animekompi/detail/{slug}")
    suspend fun getDetail(@Path("slug") slug: String): Response<AnimeKompiDetailResponse>

    // 30. Tooltip (quick info)
    @GET("anime/animekompi/tooltip/{slug}")
    suspend fun getTooltip(@Path("slug") slug: String): Response<AnimeKompiTooltipResponse>

    // 31. Episode Detail (for player)
    @GET("anime/animekompi/episode/{slug}")
    suspend fun getEpisode(@Path("slug") slug: String): Response<AnimeKompiEpisodeResponse>

    // 32. Episode Mirrors
    @GET("anime/animekompi/episode/{slug}/mirrors")
    suspend fun getEpisodeMirrors(@Path("slug") slug: String): Response<AnimeKompiMirrorsResponse>

    // 33. Episode Danmaku
    @GET("anime/animekompi/episode/{slug}/danmaku")
    suspend fun getEpisodeDanmaku(@Path("slug") slug: String): Response<AnimeKompiDanmakuResponse>

    // 34. Episode Comments
    @GET("anime/animekompi/episode/{slug}/comments")
    suspend fun getEpisodeComments(@Path("slug") slug: String, @Query("page") page: Int = 1): Response<AnimeKompiCommentsResponse>

    // 35. Episode Reactions
    @GET("anime/animekompi/episode/{slug}/reactions")
    suspend fun getEpisodeReactions(@Path("slug") slug: String): Response<AnimeKompiReactionsResponse>

    // 36. Settings
    @GET("anime/animekompi/settings")
    suspend fun getSettings(): Response<AnimeKompiSettingsResponse>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"

        fun create(): AnimeKompiApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AnimeKompiApi::class.java)
        }
    }
}