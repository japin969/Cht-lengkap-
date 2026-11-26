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
import java.util.concurrent.TimeUnit

/**
 * DonghuaApi — sumber resmi strem/donghua: https://www.sankavollerei.web.id.
 * Hanya hostname ini yang dipakai (sesuai aturan). SSL valid (Cloudflare).
 */
interface DonghuaApi {

    @GET("anime/donghua/latest")
    suspend fun latest(): Response<DonghuaLatestResponse>

    @GET("anime/donghua/search/{q}")
    suspend fun search(@Path("q") q: String): Response<DonghuaSearchResponse>

    @GET("anime/donghua/detail/{slug}")
    suspend fun detail(@Path("slug") slug: String): Response<DonghuaDetailResponse>

    @GET("anime/donghua/episode/{slug}")
    suspend fun episode(@Path("slug") slug: String): Response<DonghuaEpisodeResponse>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        fun create(): DonghuaApi {
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
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(DonghuaApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class DonghuaLatestResponse(
    val status: String? = null,
    val creator: String? = null,
    val latest_donghua: List<DhItem>? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaSearchResponse(
    val creator: String? = null,
    val data: List<DhItem>? = null
)

@JsonClass(generateAdapter = true)
data class DhItem(
    val title: String? = null,
    val slug: String? = null,
    val poster: String? = null,
    val status: String? = null,
    val type: String? = null,
    val sub: String? = null,
    val href: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaDetailResponse(
    val status: String? = null,
    val title: String? = null,
    val poster: String? = null,
    val rating: String? = null,
    val type: String? = null,
    val episodes_count: String? = null,
    val status_detail: String? = null,
    val synopsis: String? = null,
    val genres: List<DhGenre>? = null,
    val episodes_list: List<DhEpisodeLink>? = null,
    val recommendations: List<DhItem>? = null
)

@JsonClass(generateAdapter = true)
data class DhGenre(
    val name: String? = null,
    val slug: String? = null
)

@JsonClass(generateAdapter = true)
data class DhEpisodeLink(
    val episode: String? = null,
    val episode_number: String? = null,
    val slug: String? = null,
    val href: String? = null
)

@JsonClass(generateAdapter = true)
data class DonghuaEpisodeResponse(
    val status: String? = null,
    val episode: String? = null,
    val streaming: DhStreaming? = null,
    val donghua_details: DhItem? = null,
    val navigation: DhNavigation? = null,
    val episodes_list: List<DhEpisodeLink>? = null
)

@JsonClass(generateAdapter = true)
data class DhStreaming(
    val main_url: DhServer? = null,
    val servers: List<DhServer>? = null
)

@JsonClass(generateAdapter = true)
data class DhServer(
    val name: String? = null,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class DhNavigation(
    val previous_episode: DhEpisodeLink? = null,
    val next_episode: DhEpisodeLink? = null
)