package com.jayra.app.data.api

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
 * API service untuk fitur Anime (animekompi).
 * Base URL & path PERSIS sesuai daftar endpoint yang disediakan.
 * Response model nullable agar tidak crash bila bentuk JSON berbeda.
 */
interface AnimekompiApi {

    @GET("anime/animekompi/home")
    suspend fun home(): Response<Map<String, Any?>>

    @GET("anime/animekompi/terbaru")
    suspend fun terbaru(@Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/batch")
    suspend fun batch(@Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/movie")
    suspend fun movie(@Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/live-action")
    suspend fun liveAction(@Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/donghua")
    suspend fun donghua(@Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/tokusatsu")
    suspend fun tokusatsu(@Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/upcoming")
    suspend fun upcoming(@Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/schedule")
    suspend fun schedule(): Response<Map<String, Any?>>

    @GET("anime/animekompi/list")
    suspend fun list(@Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/az")
    suspend fun az(@Query("letter") letter: String? = null, @Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/search")
    suspend fun search(@Query("q") q: String, @Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/search/suggest")
    suspend fun searchSuggest(@Query("q") q: String): Response<Map<String, Any?>>

    @GET("anime/animekompi/filter")
    suspend fun filter(): Response<Map<String, Any?>>

    @GET("anime/animekompi/filterlist")
    suspend fun filterList(
        @Query("genre") genre: String? = null,
        @Query("season") season: String? = null,
        @Query("studio") studio: String? = null,
        @Query("status") status: String? = null,
        @Query("type") type: String? = null,
        @Query("order") order: String? = null,
        @Query("page") page: Int? = null
    ): Response<Map<String, Any?>>

    @GET("anime/animekompi/genres")
    suspend fun genres(): Response<Map<String, Any?>>

    @GET("anime/animekompi/seasons")
    suspend fun seasons(): Response<Map<String, Any?>>

    @GET("anime/animekompi/studios")
    suspend fun studios(): Response<Map<String, Any?>>

    @GET("anime/animekompi/status")
    suspend fun status(): Response<Map<String, Any?>>

    @GET("anime/animekompi/types")
    suspend fun types(): Response<Map<String, Any?>>

    @GET("anime/animekompi/orders")
    suspend fun orders(): Response<Map<String, Any?>>

    @GET("anime/animekompi/genre/{slug}")
    suspend fun genre(@Path("slug") slug: String, @Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/genre-detail/{slug}")
    suspend fun genreDetail(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/animekompi/season/{slug}")
    suspend fun season(@Path("slug") slug: String, @Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/studio/{slug}")
    suspend fun studio(@Path("slug") slug: String, @Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/status/{slug}")
    suspend fun statusSlug(@Path("slug") slug: String, @Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/type/{slug}")
    suspend fun typeSlug(@Path("slug") slug: String, @Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/order/{slug}")
    suspend fun orderSlug(@Path("slug") slug: String, @Query("page") page: Int? = null): Response<Map<String, Any?>>

    @GET("anime/animekompi/detail/{slug}")
    suspend fun detail(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/animekompi/tooltip/{slug}")
    suspend fun tooltip(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/animekompi/episode/{slug}")
    suspend fun episode(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/animekompi/episode/{slug}/mirrors")
    suspend fun mirrors(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/animekompi/episode/{slug}/danmaku")
    suspend fun danmaku(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/animekompi/episode/{slug}/comments")
    suspend fun comments(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/animekompi/episode/{slug}/reactions")
    suspend fun reactions(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/animekompi/settings")
    suspend fun settings(): Response<Map<String, Any?>>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"

        fun create(): AnimekompiApi {
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
                .create(AnimekompiApi::class.java)
        }
    }
}