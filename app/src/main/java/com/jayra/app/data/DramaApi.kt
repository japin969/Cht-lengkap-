package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

interface DramaApi {
    // Donghub
    @GET("anime/donghub/home")
    suspend fun donghubHome(): Response<DonghubHomeResponse>

    @GET("anime/donghub/latest")
    suspend fun donghubLatest(): Response<DonghubListResponse>

    @GET("anime/donghub/popular")
    suspend fun donghubPopular(): Response<DonghubListResponse>

    @GET("anime/donghub/movie")
    suspend fun donghubMovie(): Response<DonghubListResponse>

    @GET("anime/donghub/schedule")
    suspend fun donghubSchedule(): Response<DonghubListResponse>

    @GET("anime/donghub/search/{query}")
    suspend fun donghubSearch(@Path("query") query: String): Response<DonghubListResponse>

    @GET("anime/donghub/genre/{slug}")
    suspend fun donghubGenre(@Path("slug") slug: String): Response<DonghubListResponse>

    @GET("anime/donghub/list")
    suspend fun donghubList(): Response<DonghubListResponse>

    @GET("anime/donghub/detail/{slug}")
    suspend fun donghubDetail(@Path("slug") slug: String): Response<DonghubDetailResponse>

    @GET("anime/donghub/episode/{slug}")
    suspend fun donghubEpisode(@Path("slug") slug: String): Response<DonghubEpisodeResponse>

    // Donghua
    @GET("anime/donghua/home")
    suspend fun donghuaHome(): Response<DonghuaHomeResponse>

    @GET("anime/donghua/ongoing")
    suspend fun donghuaOngoing(): Response<DonghuaListResponse>

    @GET("anime/donghua/completed")
    suspend fun donghuaCompleted(): Response<DonghuaListResponse>

    @GET("anime/donghua/latest")
    suspend fun donghuaLatest(): Response<DonghuaListResponse>

    @GET("anime/donghua/schedule")
    suspend fun donghuaSchedule(): Response<DonghuaListResponse>

    @GET("anime/donghua/genres")
    suspend fun donghuaGenres(): Response<DonghuaGenresResponse>

    @GET("anime/donghua/search/{keyword}")
    suspend fun donghuaSearch(@Path("keyword") keyword: String): Response<DonghuaListResponse>

    @GET("anime/donghua/detail/{slug}")
    suspend fun donghuaDetail(@Path("slug") slug: String): Response<DonghuaDetailResponse>

    @GET("anime/donghua/episode/{slug}")
    suspend fun donghuaEpisode(@Path("slug") slug: String): Response<DonghuaEpisodeResponse>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"

        fun create(): DramaApi {
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
                .create(DramaApi::class.java)
        }
    }
}

// Generic container for lists of items
@JsonClass(generateAdapter = true)
data class DramaGenericItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghubHomeResponse(
    @Json(name = "results") val results: List<DramaGenericItem>? = null,
    @Json(name = "data") val data: List<DramaGenericItem>? = null
)

@JsonClass(generateAdapter = true)
data class DonghubListResponse(
    @Json(name = "results") val results: List<DramaGenericItem>? = null,
    @Json(name = "data") val data: List<DramaGenericItem>? = null
)

@JsonClass(generateAdapter = true)
data class DonghubDetailResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "episodes") val episodes: List<DramaGenericItem>? = null
)

@JsonClass(generateAdapter = true)
data class DonghubEpisodeResponse(
    @Json(name = "videoUrl") val videoUrl: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "stream") val stream: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaHomeResponse(
    @Json(name = "results") val results: List<DramaGenericItem>? = null,
    @Json(name = "data") val data: List<DramaGenericItem>? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaListResponse(
    @Json(name = "results") val results: List<DramaGenericItem>? = null,
    @Json(name = "data") val data: List<DramaGenericItem>? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaDetailResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "episodes") val episodes: List<DramaGenericItem>? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaEpisodeResponse(
    @Json(name = "videoUrl") val videoUrl: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "stream") val stream: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaGenresResponse(
    @Json(name = "results") val results: List<DramaGenericItem>? = null,
    @Json(name = "data") val data: List<DramaGenericItem>? = null
)