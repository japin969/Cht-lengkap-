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
 * AnimeKuindoApi — SATU-SATUNYA sumber anime: https://www.sankavollerei.web.id
 * Endpoint /anime/animekuindo/*. SSL valid (Cloudflare), tanpa bypass.
 * Semua pemanggilan di ViewModel dijalankan PARELAL (async), bukan fallback.
 */
interface AnimeKuindoApi {

    @GET("anime/animekuindo/home")
    suspend fun home(): Response<KuindoHomeResponse>

    @GET("anime/animekuindo/schedule")
    suspend fun schedule(): Response<KuindoScheduleResponse>

    @GET("anime/animekuindo/latest")
    suspend fun latest(): Response<KuindoListResponse>

    @GET("anime/animekuindo/popular")
    suspend fun popular(): Response<KuindoListResponse>

    @GET("anime/animekuindo/movie")
    suspend fun movie(): Response<KuindoListResponse>

    @GET("anime/animekuindo/genres")
    suspend fun genres(): Response<KuindoGenreListResponse>

    @GET("anime/animekuindo/genres/{slug}")
    suspend fun genre(@Path("slug") slug: String): Response<KuindoListResponse>

    @GET("anime/animekuindo/seasons")
    suspend fun seasons(): Response<KuindoGenreListResponse>

    @GET("anime/animekuindo/seasons/{slug}")
    suspend fun season(@Path("slug") slug: String): Response<KuindoListResponse>

    @GET("anime/animekuindo/search/{query}")
    suspend fun search(@Path("query") query: String): Response<KuindoListResponse>

    @GET("anime/animekuindo/detail/{slug}")
    suspend fun detail(@Path("slug") slug: String): Response<KuindoDetailResponse>

    @GET("anime/animekuindo/episode/{slug}")
    suspend fun episode(@Path("slug") slug: String): Response<KuindoEpisodeResponse>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        fun create(): AnimeKuindoApi {
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
                .create(AnimeKuindoApi::class.java)
        }
    }
}

// ── list item (latest/popular/movie/genre/season/search) ──
@JsonClass(generateAdapter = true)
data class KuindoListResponse(
    val status: String? = null, val creator: String? = null,
    val source: String? = null, val data: List<KuindoItem>? = null
)
@JsonClass(generateAdapter = true)
data class KuindoItem(
    val title: String? = null, val slug: String? = null, val poster: String? = null,
    val type: String? = null, val status: String? = null, val episode: String? = null,
    val url: String? = null
)

// ── home ──
@JsonClass(generateAdapter = true)
data class KuindoHomeResponse(
    val status: String? = null, val creator: String? = null, val data: KuindoHomeData? = null
)
@JsonClass(generateAdapter = true)
data class KuindoHomeData(
    val hero: KuindoItem? = null, val latest: List<KuindoItem>? = null
)

// ── schedule ──
@JsonClass(generateAdapter = true)
data class KuindoScheduleResponse(
    val status: String? = null, val creator: String? = null, val data: List<KuindoDay>? = null
)
@JsonClass(generateAdapter = true)
data class KuindoDay(val day: String? = null, val anime_list: List<KuindoItem>? = null)

// ── genres / seasons ──
@JsonClass(generateAdapter = true)
data class KuindoGenreListResponse(
    val status: String? = null, val creator: String? = null, val data: List<KuindoGenre>? = null
)
@JsonClass(generateAdapter = true)
data class KuindoGenre(
    val name: String? = null, val slug: String? = null,
    val count: String? = null, val url: String? = null
)

// ── detail ──
@JsonClass(generateAdapter = true)
data class KuindoDetailResponse(
    val status: String? = null, val creator: String? = null, val data: KuindoDetail? = null
)
@JsonClass(generateAdapter = true)
data class KuindoDetail(
    val title: String? = null, val poster: String? = null, val rating: String? = null,
    val synopsis: String? = null, val info: Map<String, String>? = null,
    val genres: List<KuindoGenre>? = null,
    val episode_list: List<KuindoEpisodeLink>? = null,
    val recommendations: List<KuindoItem>? = null
)
@JsonClass(generateAdapter = true)
data class KuindoEpisodeLink(
    val episode: String? = null, val title: String? = null,
    val release_date: String? = null, val slug: String? = null, val url: String? = null
)

// ── episode (player) ──
@JsonClass(generateAdapter = true)
data class KuindoEpisodeResponse(
    val status: String? = null, val creator: String? = null, val data: KuindoEpisodeData? = null
)
@JsonClass(generateAdapter = true)
data class KuindoEpisodeData(
    val title: String? = null, val anime_slug: String? = null,
    val streams: List<KuindoStream>? = null, val downloads: List<KuindoStream>? = null
)
@JsonClass(generateAdapter = true)
data class KuindoStream(val server: String? = null, val url: String? = null)