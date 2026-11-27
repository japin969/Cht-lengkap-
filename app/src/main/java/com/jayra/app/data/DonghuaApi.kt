package com.jayra.app.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

/**
 * Donghua API - https://www.sankavollerei.web.id
 * HANYA endpoint yang diizinkan sesuai spesifikasi.
 */
interface DonghuaApi {

    // ═══════════════════════════════════════════
    // DONGHUA ENDPOINTS
    // ═══════════════════════════════════════════

    @GET("anime/donghua/home/{page}")
    suspend fun home(@Path("page") page: Int = 1): Response<DonghuaHomeResponse>

    @GET("anime/donghua/ongoing/{page}")
    suspend fun ongoing(@Path("page") page: Int = 1): Response<DonghuaListResponse>

    @GET("anime/donghua/completed/{page}")
    suspend fun completed(@Path("page") page: Int = 1): Response<DonghuaListResponse>

    @GET("anime/donghua/latest/{page}")
    suspend fun latest(@Path("page") page: Int = 1): Response<DonghuaListResponse>

    @GET("anime/donghua/schedule")
    suspend fun schedule(): Response<DonghuaScheduleResponse>

    @GET("anime/donghua/az-list/{slug}/{page}")
    suspend fun azList(@Path("slug") slug: String, @Path("page") page: Int = 1): Response<DonghuaListResponse>

    @GET("anime/donghua/genres")
    suspend fun genres(): Response<DonghuaGenresResponse>

    @GET("anime/donghua/genres/{slug}/{page}")
    suspend fun genreList(@Path("slug") slug: String, @Path("page") page: Int = 1): Response<DonghuaListResponse>

    @GET("anime/donghua/seasons/{year}")
    suspend fun seasons(@Path("year") year: Int): Response<DonghuaListResponse>

    @GET("anime/donghua/search/{keyword}/{page}")
    suspend fun search(@Path("keyword") keyword: String, @Path("page") page: Int = 1): Response<DonghuaSearchResponse>

    @GET("anime/donghua/detail/{slug}")
    suspend fun detail(@Path("slug") slug: String): Response<DonghuaDetailResponse>

    @GET("anime/donghua/episode/{slug}")
    suspend fun episode(@Path("slug") slug: String): Response<DonghuaEpisodeResponse>

    // ═══════════════════════════════════════════
    // DONGHUB ENDPOINTS
    // ═══════════════════════════════════════════

    @GET("anime/donghub/home")
    suspend fun donghubHome(): Response<DonghubHomeResponse>

    @GET("anime/donghub/latest")
    suspend fun donghubLatest(): Response<DonghubListResponse>

    @GET("anime/donghub/popular")
    suspend fun donghubPopular(): Response<DonghubListResponse>

    @GET("anime/donghub/movie")
    suspend fun donghubMovie(): Response<DonghubListResponse>

    @GET("anime/donghub/schedule")
    suspend fun donghubSchedule(): Response<DonghubScheduleResponse>

    @GET("anime/donghub/search/{query}")
    suspend fun donghubSearch(@Path("query") query: String): Response<DonghubSearchResponse>

    @GET("anime/donghub/genre/{slug}")
    suspend fun donghubGenre(@Path("slug") slug: String): Response<DonghubListResponse>

    @GET("anime/donghub/list")
    suspend fun donghubList(): Response<DonghubListResponse>

    @GET("anime/donghub/detail/{slug}")
    suspend fun donghubDetail(@Path("slug") slug: String): Response<DonghubDetailResponse>

    @GET("anime/donghub/episode/{slug}")
    suspend fun donghubEpisode(@Path("slug") slug: String): Response<DonghubEpisodeResponse>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"
        private const val REFERRER = "https://www.sankavollerei.web.id/"

        fun create(): DonghuaApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(ImageReferrerInterceptor(REFERRER))
                .build()

            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(DonghuaApi::class.java)
        }
    }
}