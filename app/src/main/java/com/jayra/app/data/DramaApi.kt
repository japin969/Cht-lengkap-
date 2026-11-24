package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

/**
 * Drama (Strem) API — https://www.sankavollerei.web.id
 *
 * Sumber: donghua + donghub (menggantikan anichin/dramabox lama).
 * Endpoint (semua butuh trailing? TIDAK — path /anime/donghua/...):
 *  - anime/donghua/home/{page}       -> latest_release[]
 *  - anime/donghua/ongoing/{page}    -> ongoing_donghua[]
 *  - anime/donghua/completed/{page}  -> completed_donghua[]
 *  - anime/donghua/latest/{page}     -> latest_donghua[]
 *  - anime/donghua/detail/{slug}     -> detail lengkap
 *  - anime/donghua/episode/{slug}    -> streaming
 *  - anime/donghub/home              -> data.slider/popular/latest
 *  - anime/donghub/latest, /popular, /detail/{slug}, /episode/{slug}
 *
 * CATATAN: TIDAK pakai X-API-Key. Cukup User-Agent browser (Cloudflare).
 */
interface DramaApi {

    @GET("anime/donghua/home/{page}")
    suspend fun donghuaHome(@Path("page") page: Int = 1): Response<DramaListResponse>

    @GET("anime/donghua/ongoing/{page}")
    suspend fun donghuaOngoing(@Path("page") page: Int = 1): Response<DramaListResponse>

    @GET("anime/donghua/completed/{page}")
    suspend fun donghuaCompleted(@Path("page") page: Int = 1): Response<DramaListResponse>

    @GET("anime/donghua/latest/{page}")
    suspend fun donghuaLatest(@Path("page") page: Int = 1): Response<DramaListResponse>

    @GET("anime/donghua/search/{keyword}/{page}")
    suspend fun donghuaSearch(
        @Path("keyword") keyword: String,
        @Path("page") page: Int = 1
    ): Response<DramaListResponse>

    @GET("anime/donghua/detail/{slug}")
    suspend fun donghuaDetail(@Path("slug") slug: String): Response<DramaDetailResponse>

    @GET("anime/donghua/episode/{slug}")
    suspend fun donghuaEpisode(@Path("slug") slug: String): Response<DramaEpisodeResponse>

    @GET("anime/donghub/home")
    suspend fun donghubHome(): Response<DramaListResponse>

    @GET("anime/donghub/latest")
    suspend fun donghubLatest(): Response<DramaListResponse>

    @GET("anime/donghub/popular")
    suspend fun donghubPopular(): Response<DramaListResponse>

    @GET("anime/donghub/detail/{slug}")
    suspend fun donghubDetail(@Path("slug") slug: String): Response<DramaDetailResponse>

    @GET("anime/donghub/episode/{slug}")
    suspend fun donghubEpisode(@Path("slug") slug: String): Response<DramaEpisodeResponse>

    // ── NEKOPOI (dipisah, card sendiri) ─────────────────────────
    @GET("anime/nekopoi/home")
    suspend fun nekopoiHome(): Response<DramaListResponse>

    @GET("anime/nekopoi/detail/{slug}")
    suspend fun nekopoiDetail(@Path("slug") slug: String): Response<DramaDetailResponse>

    @GET("anime/nekopoi/episode/{slug}")
    suspend fun nekopoiEpisode(@Path("slug") slug: String): Response<DramaEpisodeResponse>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"
        private const val UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        @Volatile private var instance: DramaApi? = null
        fun create(): DramaApi = instance ?: synchronized(this) {
            instance ?: build().also { instance = it }
        }

        private fun build(): DramaApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", UA)
                            .header("Accept", "application/json")
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
                .create(DramaApi::class.java)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MODEL — mengikuti JSON asli sankavollerei donghua/donghub
// ═══════════════════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class DramaListResponse(
    val status: String? = null,
    val creator: String? = null,
    val message: String? = null,
    val latest_release: List<DramaItem>? = null,
    val ongoing_donghua: List<DramaItem>? = null,
    val completed_donghua: List<DramaItem>? = null,
    val latest_donghua: List<DramaItem>? = null,
    // donghub / nekopoi (keduanya nested di data)
    val data: DramaDonghubData? = null,
    val pagination: DramaPagination? = null
) {
    /** Gabung semua bentuk payload jadi satu list + dedup. */
    val allItems: List<DramaItem>
        get() = buildList {
            addAll(latest_release.orEmpty())
            addAll(ongoing_donghua.orEmpty())
            addAll(completed_donghua.orEmpty())
            addAll(latest_donghua.orEmpty())
            data?.let { d ->
                addAll(d.slider.orEmpty())
                addAll(d.popular.orEmpty())
                addAll(d.latest.orEmpty())
                addAll(d.recommended.orEmpty())
                addAll(d.latest_hentai.orEmpty())
                addAll(d.latest_episodes.orEmpty())
                addAll(d.latest_jav.orEmpty())
            }
        }.distinctBy { it.slug ?: it.href ?: it.title }
}

@JsonClass(generateAdapter = true)
data class DramaDonghubData(
    val slider: List<DramaItem>? = null,
    val popular: List<DramaItem>? = null,
    val latest: List<DramaItem>? = null,
    // nekopoi
    val recommended: List<DramaItem>? = null,
    val latest_hentai: List<DramaItem>? = null,
    val latest_episodes: List<DramaItem>? = null,
    val latest_jav: List<DramaItem>? = null
)


@JsonClass(generateAdapter = true)
data class DramaPagination(
    val page: Int? = null,
    val totalPages: Int? = null,
    val hasNext: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class DramaItem(
    val title: String? = null,
    val slug: String? = null,
    val poster: String? = null,
    val cover: String? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    val status: String? = null,
    val type: String? = null,
    val sub: String? = null,
    val current_episode: String? = null,
    val rating: String? = null,
    val synopsis: String? = null,
    val href: String? = null,
    // detail
    val alter_title: String? = null,
    val studio: String? = null,
    val released: String? = null,
    val duration: String? = null,
    val genres: List<DramaGenreRef>? = null,
    val episodes: List<DramaEpisode>? = null
) {
    val genreNames: List<String> get() = genres?.mapNotNull { it.name }.orEmpty()
    val stableId: String? get() = slug ?: href?.substringAfterLast("/")?.trim('/')
    val bestCover: String? get() = poster ?: cover ?: thumbnail
    val bestSynopsis: String? get() = synopsis
    val epsCount: Int? get() = episodes?.size
    val completed: Boolean get() = status?.contains("completed", true) == true || status?.contains("tamat", true) == true
}

@JsonClass(generateAdapter = true)
data class DramaDetailResponse(
    val status: String? = null,
    val creator: String? = null,
    val title: String? = null,
    val alter_title: String? = null,
    val poster: String? = null,
    val cover: String? = null,
    val rating: String? = null,
    val studio: String? = null,
    val released: String? = null,
    val duration: String? = null,
    val synopsis: String? = null,
    val genres: List<DramaGenreRef>? = null,
    val episodes: List<DramaEpisode>? = null,
    val episodes_list: List<DramaEpisode>? = null,
    val data: DramaItem? = null,
    // donghub detail membungkus di data
    val episodeList: List<DramaEpisode>? = null
) {
    val resolvedTitle: String? get() = title ?: data?.title
    val resolvedCover: String? get() = poster ?: cover ?: data?.poster ?: data?.cover
    val resolvedSynopsis: String? get() = synopsis ?: data?.synopsis
    val resolvedEpisodes: List<DramaEpisode>
        get() = episodes_list ?: episodes ?: episodeList ?: data?.episodes ?: emptyList()
    val resolvedGenres: List<String>
        get() = genres?.mapNotNull { it.name }?.ifEmpty { null }
            ?: data?.genres?.mapNotNull { it.name }
            ?: emptyList()
    val resolvedRating: String? get() = rating ?: data?.rating
}

@JsonClass(generateAdapter = true)
data class DramaGenreRef(
    val name: String? = null,
    val slug: String? = null
)

@JsonClass(generateAdapter = true)
data class DramaEpisode(
    val title: String? = null,
    val slug: String? = null,
    val href: String? = null,
    val url: String? = null,
    val episode: String? = null,
    val number: String? = null,
    val episode_number: String? = null
) {
    val label: String? get() = title ?: episode ?: number
    val epSlug: String? get() = slug ?: href?.substringAfterLast("/")?.trim('/') ?: url?.substringAfterLast("/")?.trim('/')
    /** Nomor episode (Int) untuk UI lama. */
    val num: Int? get() = (number ?: episode_number ?: episode)?.filter { it.isDigit() }?.toIntOrNull()
    val locked: Boolean get() = false
}

@JsonClass(generateAdapter = true)
data class DramaEpisodeResponse(
    val status: String? = null,
    val creator: String? = null,
    val episode: String? = null,
    val title: String? = null,
    val data: DramaEpisodeData? = null,
    // donghua episode streaming
    val streaming: DramaStreaming? = null
) {
    val bestStreamUrl: String?
        get() = streaming?.main_url?.url
            ?: streaming?.servers?.firstOrNull { !it.url.isNullOrBlank() }?.url
            ?: data?.streams?.firstOrNull { !it.url.isNullOrBlank() }?.url
}

@JsonClass(generateAdapter = true)
data class DramaEpisodeData(
    val title: String? = null,
    val streams: List<DramaStreamServer>? = null,
    val downloads: List<DramaStreamServer>? = null
)

@JsonClass(generateAdapter = true)
data class DramaStreaming(
    val main_url: DramaStreamServer? = null,
    val servers: List<DramaStreamServer>? = null
)

@JsonClass(generateAdapter = true)
data class DramaStreamServer(
    val name: String? = null,
    val server: String? = null,
    val url: String? = null
)