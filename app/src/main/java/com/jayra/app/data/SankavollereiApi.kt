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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Sanka Vollerei API — sumber KEDUA untuk Komik & Anime.
 *
 * Ditambahkan DI SAMPING [NexadevApi], bukan menggantikannya. Pola client,
 * base URL, dan cara manggil mengikuti [NexadevApi] yang sudah ada di project.
 *
 * Base URL: https://www.sankavollerei.web.id/
 *
 * CATATAN PENTING: endpoint di API ini memerlukan User-Agent browser, kalau
 * tidak akan dijawab 403 oleh Cloudflare. Karena itu client-nya memasang
 * interceptor UA — sama seperti pola yang dipakai SiputzxApi di project ini.
 */
interface SankavollereiApi {

    // ── ANIME ──────────────────────────────────────────────────

    /** Home anime: ongoing + completed. */
    @GET("anime/home")
    suspend fun animeHome(): Response<SankavollereiAnimeHomeResponse>

    /** Anime sedang tayang. */
    @GET("anime/ongoing-anime")
    suspend fun animeOngoing(): Response<SankavollereiAnimeListResponse>

    /** Anime yang sudah selesai. */
    @GET("anime/complete-anime")
    suspend fun animeComplete(): Response<SankavollereiAnimeListResponse>

    /** Cari anime berdasarkan judul. */
    @GET("anime/search/anime")
    suspend fun animeSearch(@Query("search") query: String): Response<SankavollereiAnimeListResponse>

    /** Detail anime (termasuk daftar episode). */
    @GET("anime/anime/{animeId}")
    suspend fun animeDetail(
        @Path("animeId") animeId: String
    ): Response<SankavollereiAnimeDetailResponse>

    /** Detail episode: berisi defaultStreamingUrl + server per kualitas. */
    @GET("anime/episode/{episodeId}")
    suspend fun animeEpisode(
        @Path("episodeId") episodeId: String
    ): Response<SankavollereiEpisodeResponse>

    // ── KOMIK ──────────────────────────────────────────────────

    /** Komik terbaru. */
    @GET("comic/terbaru")
    suspend fun comicLatest(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<SankavollereiComicListResponse>

    /** Komik populer. */
    @GET("comic/populer")
    suspend fun comicPopular(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<SankavollereiComicListResponse>

    /** Cari komik. */
    @GET("comic/search")
    suspend fun comicSearch(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<SankavollereiComicListResponse>

    /** Detail komik (slug dari hasil search). */
    @GET("comic/comic/{slug}")
    suspend fun comicDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse>

    /** Chapter komik. */
    @GET("comic/chapter/{segment}")
    suspend fun comicChapter(
        @Path("segment") segment: String
    ): Response<SankavollereiComicChapterResponse>

    companion object {
        private const val BASE_URL = "https://www.sankavollerei.web.id/"

        /** Wajib browser UA, kalau tidak Cloudflare menolak (403). */
        private const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        fun create(): SankavollereiApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", BROWSER_UA)
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SankavollereiApi::class.java)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MODEL — ANIME
// ═══════════════════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class SankavollereiAnimeHomeResponse(
    val status: String? = null,
    val ok: Boolean? = null,
    val data: SankavollereiAnimeHomeData? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiAnimeHomeData(
    val ongoing: SankavollereiAnimeGroup? = null,
    val completed: SankavollereiAnimeGroup? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiAnimeGroup(
    val href: String? = null,
    val otakudesuUrl: String? = null,
    val animeList: List<SankavollereiAnimeItem>? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiAnimeListResponse(
    val status: String? = null,
    val ok: Boolean? = null,
    val data: SankavollereiAnimeListData? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiAnimeListData(
    val animeList: List<SankavollereiAnimeItem>? = null,
    val pagination: SankavollereiPagination? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiAnimeItem(
    val title: String? = null,
    val poster: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val score: String? = null,
    val releaseDay: String? = null,
    val latestReleaseDate: String? = null,
    val animeId: String? = null,
    val href: String? = null,
    val otakudesuUrl: String? = null,
    val genres: List<String>? = null
) {
    /** Dipakai untuk navigasi ke halaman detail. */
    val detailHref: String? get() = href ?: animeId?.let { "/anime/anime/$it" }
}

@JsonClass(generateAdapter = true)
data class SankavollereiAnimeDetailResponse(
    val status: String? = null,
    val ok: Boolean? = null,
    val data: SankavollereiAnimeDetailData? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiAnimeDetailData(
    val title: String? = null,
    val poster: String? = null,
    val status: String? = null,
    val score: String? = null,
    val studio: String? = null,
    val duration: String? = null,
    val episodes: Int? = null,
    val genres: List<String>? = null,
    val synopsis: SankavollereiSynopsis? = null,
    val episodeList: List<SankavollereiEpisodeItem>? = null
) {
    val plainSynopsis: String?
        get() = synopsis?.paragraphs
            ?.filter { it.isNotBlank() }
            ?.joinToString("\n\n")
}

@JsonClass(generateAdapter = true)
data class SankavollereiSynopsis(
    val paragraphs: List<String>? = null,
    val connections: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiEpisodeItem(
    val title: String? = null,
    val eps: Int? = null,
    val date: String? = null,
    val episodeId: String? = null,
    val href: String? = null,
    val otakudesuUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiEpisodeResponse(
    val status: String? = null,
    val ok: Boolean? = null,
    val data: SankavollereiEpisodeData? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiEpisodeData(
    val title: String? = null,
    val animeId: String? = null,
    val releaseTime: String? = null,
    val defaultStreamingUrl: String? = null,
    val hasPrevEpisode: Boolean? = null,
    val hasNextEpisode: Boolean? = null,
    val server: SankavollereiServerBlock? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiServerBlock(
    val qualities: List<SankavollereiQuality>? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiQuality(
    val title: String? = null,
    val serverList: List<SankavollereiServerItem>? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiServerItem(
    val title: String? = null,
    val serverId: String? = null,
    val href: String? = null
)

// ═══════════════════════════════════════════════════════════════
// MODEL — KOMIK
// ═══════════════════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class SankavollereiComicListResponse(
    val status: Boolean? = null,
    val creator: String? = null,
    val message: String? = null,
    val q: String? = null,
    val total: Int? = null,
    val page: Int? = null,
    val comics: List<SankavollereiComicItem>? = null,
    val data: List<SankavollereiComicItem>? = null
) {
    /** Gabungkan dua bentuk payload yang dipakai API ini. */
    val allItems: List<SankavollereiComicItem>
        get() = data ?: comics ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class SankavollereiComicItem(
    val title: String? = null,
    val altTitle: String? = null,
    val slug: String? = null,
    val link: String? = null,
    val href: String? = null,
    val thumbnail: String? = null,
    val image: String? = null,
    val img: String? = null,
    val chapter: String? = null,
    val type: String? = null,
    val rating: String? = null,
    val time_ago: String? = null,
    val url: String? = null,
    val data: SankavollereiComicItemData? = null
) {
    val bestCover: String?
        get() = thumbnail ?: image ?: img

    val latestChapter: String?
        get() = chapter ?: data?.chapter
}

@JsonClass(generateAdapter = true)
data class SankavollereiComicItemData(
    val url: String? = null,
    val chapter: String? = null,
    val time: String? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiComicDetailResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val data: SankavollereiComicDetailData? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiComicDetailData(
    val title: String? = null,
    val slug: String? = null,
    val cover: String? = null,
    val thumbnail: String? = null,
    val image: String? = null,
    val synopsis: String? = null,
    val sinopsis: String? = null,
    val genres: List<String>? = null,
    val type: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val chapters: List<SankavollereiComicChapterItem>? = null,
    val chapter: List<SankavollereiComicChapterItem>? = null
) {
    val bestCover: String? get() = cover ?: thumbnail ?: image
    val plainSynopsis: String? get() = synopsis ?: sinopsis
    val allChapters: List<SankavollereiComicChapterItem>
        get() = chapters ?: chapter ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class SankavollereiComicChapterItem(
    val title: String? = null,
    val slug: String? = null,
    val link: String? = null,
    val href: String? = null,
    val url: String? = null,
    val date: String? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiComicChapterResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val data: SankavollereiComicChapterData? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiComicChapterData(
    val title: String? = null,
    val chapter: String? = null,
    val pages: List<String>? = null,
    val images: List<String>? = null
) {
    val allPages: List<String> get() = pages ?: images ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class SankavollereiPagination(
    val page: Int? = null,
    val totalPages: Int? = null,
    val hasNext: Boolean? = null
)