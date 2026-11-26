package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

interface NekoApi {

    @GET("neko/home")
    suspend fun home(@Query("page") page: Int = 1): Response<NekoHomeResponse>

    @GET("neko/search")
    suspend fun search(@Query("q") q: String, @Query("page") page: Int = 1): Response<NekoSearchResponse>

    @GET("neko/detail")
    suspend fun detail(@Query("url") url: String): Response<NekoDetailResponse>

    @GET("neko/stream")
    suspend fun stream(@Query("url") url: String): Response<NekoStreamResponse>

    @GET("neko/ongoing")
    suspend fun ongoing(@Query("page") page: Int = 1): Response<NekoListResponse>

    @GET("neko/completed")
    suspend fun completed(@Query("page") page: Int = 1): Response<NekoListResponse>

    @GET("neko/popular")
    suspend fun popular(@Query("page") page: Int = 1): Response<NekoListResponse>

    @GET("neko/schedule")
    suspend fun schedule(@Query("page") page: Int = 1): Response<NekoScheduleResponse>

    companion object {
        private const val BASE_URL = "https://apii.nexadev.my.id/"
        private const val REFERRER = "https://apii.nexadev.my.id/"

        fun create(): NekoApi {
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
                .create(NekoApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class NekoHomeResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: NekoHomeData? = null
)

@JsonClass(generateAdapter = true)
data class NekoHomeData(
    @Json(name = "ongoing") val ongoing: List<NekoItem>? = null,
    @Json(name = "popular") val popular: List<NekoItem>? = null,
    @Json(name = "completed") val completed: List<NekoItem>? = null,
    @Json(name = "schedule") val schedule: Map<String, List<NekoItem>>? = null
)

@JsonClass(generateAdapter = true)
data class NekoSearchResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: NekoSearchData? = null
)

@JsonClass(generateAdapter = true)
data class NekoSearchData(
    @Json(name = "results") val results: List<NekoItem>? = null
)

@JsonClass(generateAdapter = true)
data class NekoItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "latestEpisode") val latestEpisode: String? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class NekoDetailResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: NekoDetailData? = null
)

@JsonClass(generateAdapter = true)
data class NekoDetailData(
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "totalEpisodes") val totalEpisodes: Int? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "info") val info: Map<String, String>? = null,
    @Json(name = "episodes") val episodes: List<NekoEpisode>? = null
)

@JsonClass(generateAdapter = true)
data class NekoEpisode(
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "episodeUrl") val episodeUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class NekoStreamResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: NekoStreamData? = null
)

@JsonClass(generateAdapter = true)
data class NekoStreamData(
    @Json(name = "streamUrl") val streamUrl: String? = null,
    @Json(name = "alternativeLinks") val alternativeLinks: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class NekoListResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: NekoListData? = null
)

@JsonClass(generateAdapter = true)
data class NekoListData(
    @Json(name = "page") val page: Int? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "animeList") val animeList: List<NekoItem>? = null
)

@JsonClass(generateAdapter = true)
data class NekoScheduleResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: Map<String, List<NekoItem>>? = null
)