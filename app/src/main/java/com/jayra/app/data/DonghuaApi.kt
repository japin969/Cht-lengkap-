package com.jayra.app.data

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * SATU API untuk fitur Donghua (dua katalog: donghua + donghub).
 * Base URL & path persis seperti endpoint yang sudah dipakai repo ini.
 */
interface DonghuaApi {

    // ---------- katalog Donghua ----------
    @GET("anime/donghua/home")
    suspend fun donghuaHome(): Response<DtoDonghuaHome>

    @GET("anime/donghua/ongoing/{page}")
    suspend fun donghuaOngoing(@Path("page") page: Int = 1): Response<DtoDonghuaList>

    @GET("anime/donghua/completed/{page}")
    suspend fun donghuaCompleted(@Path("page") page: Int = 1): Response<DtoDonghuaList>

    @GET("anime/donghua/latest/{page}")
    suspend fun donghuaLatest(@Path("page") page: Int = 1): Response<DtoDonghuaList>

    @GET("anime/donghua/schedule")
    suspend fun donghuaSchedule(): Response<DtoDonghuaHome>

    @GET("anime/donghua/az-list/{letter}/{page}")
    suspend fun donghuaAzList(
        @Path("letter") letter: String,
        @Path("page") page: Int = 1
    ): Response<DtoDonghuaAz>

    @GET("anime/donghua/genres")
    suspend fun donghuaGenres(): Response<DtoDonghuaGenreData>

    @GET("anime/donghua/genres/{slug}/{page}")
    suspend fun donghuaGenre(
        @Path("slug") slug: String,
        @Path("page") page: Int = 1
    ): Response<DtoDonghuaData>

    @GET("anime/donghua/seasons/{year}")
    suspend fun donghuaSeason(@Path("year") year: String): Response<DtoDonghuaData>

    @GET("anime/donghua/search/{keyword}/{page}")
    suspend fun donghuaSearch(
        @Path("keyword") keyword: String,
        @Path("page") page: Int = 1
    ): Response<DtoDonghuaData>

    @GET("anime/donghua/detail/{slug}")
    suspend fun donghuaDetail(@Path("slug") slug: String): Response<DtoDonghuaDetail>

    @GET("anime/donghua/episode/{slug}")
    suspend fun donghuaEpisode(@Path("slug") slug: String): Response<DtoDonghuaEpisodeData>

    // ---------- katalog Donghub ----------
    @GET("anime/donghub/home")
    suspend fun donghubHome(): Response<DtoDonghubHome>

    @GET("anime/donghub/latest")
    suspend fun donghubLatest(): Response<DtoDonghubList>

    @GET("anime/donghub/popular")
    suspend fun donghubPopular(): Response<DtoDonghubList>

    @GET("anime/donghub/movie")
    suspend fun donghubMovie(): Response<DtoDonghubList>

    @GET("anime/donghub/list")
    suspend fun donghubList(): Response<DtoDonghubList>

    @GET("anime/donghub/search/{query}")
    suspend fun donghubSearch(@Path("query") query: String): Response<DtoDonghubList>

    @GET("anime/donghub/genre/{slug}")
    suspend fun donghubGenre(@Path("slug") slug: String): Response<DtoDonghubList>

    @GET("anime/donghub/detail/{slug}")
    suspend fun donghubDetail(@Path("slug") slug: String): Response<DtoDonghubDetail>

    @GET("anime/donghub/episode/{slug}")
    suspend fun donghubEpisode(@Path("slug") slug: String): Response<DtoDonghubEpisode>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"

        fun create(): DonghuaApi {
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
                .create(DonghuaApi::class.java)
        }
    }
}
