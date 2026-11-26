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
 * NekopoiApi — sumber nekopoi: https://www.sankavollerei.web.id (/anime/nekopoi/).
 * SSL valid (Cloudflare), tanpa bypass. Dipakai PARELAL dengan sumber anime lain.
 */
interface NekopoiApi {

    @GET("anime/nekopoi/home")
    suspend fun home(): Response<NkHomeResponse>

    @GET("anime/nekopoi/genres")
    suspend fun genres(): Response<NkGenreResponse>

    @GET("anime/nekopoi/genre/{slug}")
    suspend fun genre(@Path("slug") slug: String): Response<NkListResponse>

    @GET("anime/nekopoi/latest-hentai")
    suspend fun latestHentai(): Response<NkListResponse>

    @GET("anime/nekopoi/latest-jav")
    suspend fun latestJav(): Response<NkListResponse>

    @GET("anime/nekopoi/detail/{slug}")
    suspend fun detail(@Path("slug") slug: String): Response<NkDetailResponse>

    @GET("anime/nekopoi/episode/{slug}")
    suspend fun episode(@Path("slug") slug: String): Response<NkEpisodeResponse>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        fun create(): NekopoiApi {
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
                .create(NekopoiApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class NkHomeResponse(
    val status: String? = null, val creator: String? = null, val data: NkHomeData? = null
)
@JsonClass(generateAdapter = true)
data class NkHomeData(
    val recommended: List<NkItem>? = null,
    @com.squareup.moshi.Json(name = "latest_hentai") val latest: List<NkItem>? = null,
    @com.squareup.moshi.Json(name = "latest_episodes") val latestEpisodes: List<NkItem>? = null,
    @com.squareup.moshi.Json(name = "latest_jav") val latestJav: List<NkItem>? = null
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
    val streams: List<NkServer>? = null, val downloads: List<NkServer>? = null
)
@JsonClass(generateAdapter = true)
data class NkServer(val server: String? = null, val name: String? = null, val url: String? = null)