package com.jayra.app.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * API Drama — HANYA https://www.sankavollerei.web.id
 * Endpoint yang diizinkan (TIDAK boleh ditambah):
 *
 * Donghub : /anime/donghub/home, /latest, /popular, /movie, /schedule,
 *           /search/:query, /genre/:slug, /list, /detail/:slug, /episode/:slug
 * Donghua : /anime/donghua/home/:page?, /ongoing/:page?, /completed/:page?,
 *           /latest/:page?, /schedule, /az-list/:slug/:page?, /genres,
 *           /genres/:slug/:page?, /seasons/:year?, /search/:keyword/:page?,
 *           /detail/:slug, /episode/:slug
 *
 * Respons kedua sumber berbeda-beda strukturnya, jadi dikembalikan sebagai
 * JsonObject (Map) lalu diparse manual di DramaRepository/ViewModel.
 */
interface SankavollereiApi {

    // ===== Donghua =====
    @GET("anime/donghua/home")
    suspend fun donghuaHome(): Response<Map<String, Any?>>

    @GET("anime/donghua/home/{page}")
    suspend fun donghuaHomePage(@Path("page") page: Int): Response<Map<String, Any?>>

    @GET("anime/donghua/ongoing")
    suspend fun donghuaOngoing(): Response<Map<String, Any?>>

    @GET("anime/donghua/ongoing/{page}")
    suspend fun donghuaOngoingPage(@Path("page") page: Int): Response<Map<String, Any?>>

    @GET("anime/donghua/completed")
    suspend fun donghuaCompleted(): Response<Map<String, Any?>>

    @GET("anime/donghua/latest")
    suspend fun donghuaLatest(): Response<Map<String, Any?>>

    @GET("anime/donghua/schedule")
    suspend fun donghuaSchedule(): Response<Map<String, Any?>>

    @GET("anime/donghua/genres")
    suspend fun donghuaGenres(): Response<Map<String, Any?>>

    @GET("anime/donghua/detail/{slug}")
    suspend fun donghuaDetail(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/donghua/episode/{slug}")
    suspend fun donghuaEpisode(@Path("slug") slug: String): Response<Map<String, Any?>>

    // ===== Donghub =====
    @GET("anime/donghub/home")
    suspend fun donghubHome(): Response<Map<String, Any?>>

    @GET("anime/donghub/latest")
    suspend fun donghubLatest(): Response<Map<String, Any?>>

    @GET("anime/donghub/popular")
    suspend fun donghubPopular(): Response<Map<String, Any?>>

    @GET("anime/donghub/movie")
    suspend fun donghubMovie(): Response<Map<String, Any?>>

    @GET("anime/donghub/schedule")
    suspend fun donghubSchedule(): Response<Map<String, Any?>>

    @GET("anime/donghub/search/{query}")
    suspend fun donghubSearch(@Path("query") query: String): Response<Map<String, Any?>>

    @GET("anime/donghub/genre/{slug}")
    suspend fun donghubGenre(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/donghub/list")
    suspend fun donghubList(): Response<Map<String, Any?>>

    @GET("anime/donghub/detail/{slug}")
    suspend fun donghubDetail(@Path("slug") slug: String): Response<Map<String, Any?>>

    @GET("anime/donghub/episode/{slug}")
    suspend fun donghubEpisode(@Path("slug") slug: String): Response<Map<String, Any?>>

    companion object {
        const val BASE_URL = "https://www.sankavollerei.web.id/"

        fun create(): SankavollereiApi {
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
                .create(SankavollereiApi::class.java)
        }
    }
}