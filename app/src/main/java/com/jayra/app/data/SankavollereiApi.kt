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

    // ── ANIME — SAMEHADAKU ─────────────────────────────────────

    @GET("anime/samehadaku/home")
    suspend fun samehadakuHome(): Response<SankavollereiSourceHomeResponse>

    @GET("anime/samehadaku/list")
    suspend fun samehadakuList(): Response<SankavollereiSourceHomeResponse>

    @GET("anime/samehadaku/recent")
    suspend fun samehadakuRecent(): Response<SankavollereiSourceHomeResponse>

    @GET("anime/samehadaku/popular")
    suspend fun samehadakuPopular(): Response<SankavollereiSourceHomeResponse>

    @GET("anime/samehadaku/completed")
    suspend fun samehadakuCompleted(): Response<SankavollereiSourceHomeResponse>

    @GET("anime/samehadaku/ongoing")
    suspend fun samehadakuOngoing(): Response<SankavollereiSourceHomeResponse>

    @GET("anime/samehadaku/schedule")
    suspend fun samehadakuSchedule(): Response<SankavollereiSourceHomeResponse>

    @GET("anime/samehadaku/genres")
    suspend fun samehadakuGenres(): Response<SankavollereiGenreResponse>

    @GET("anime/samehadaku/genres/{genre}")
    suspend fun samehadakuGenre(
        @Path("genre") genre: String
    ): Response<SankavollereiSourceHomeResponse>

    @GET("anime/samehadaku/search")
    suspend fun samehadakuSearch(@Query("q") query: String): Response<SankavollereiSourceHomeResponse>

    // ── ANIME — GLOBAL ─────────────────────────────────────────

    @GET("anime/schedule")
    suspend fun animeSchedule(): Response<SankavollereiScheduleResponse>

    @GET("anime/genre/{genre}")
    suspend fun animeGenre(@Path("genre") genre: String): Response<SankavollereiSourceHomeResponse>

    @GET("anime/samehadaku/anime/{slug}")
    suspend fun samehadakuDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiSamehadakuDetailResponse>

    @GET("anime/samehadaku/episode/{slug}")
    suspend fun samehadakuEpisode(
        @Path("slug") slug: String
    ): Response<SankavollereiEpisodeResponse>

    // ── ANIME — OPLOVERZ ───────────────────────────────────────

    @GET("anime/oploverz/home")
    suspend fun oploverzHome(): Response<SankavollereiOploverzResponse>

    @GET("anime/oploverz/list")
    suspend fun oploverzList(): Response<SankavollereiOploverzResponse>

    @GET("anime/oploverz/ongoing")
    suspend fun oploverzOngoing(): Response<SankavollereiOploverzResponse>

    @GET("anime/oploverz/completed")
    suspend fun oploverzCompleted(): Response<SankavollereiOploverzResponse>

    @GET("anime/oploverz/search/anime")
    suspend fun oploverzSearch(@Query("q") query: String): Response<SankavollereiOploverzResponse>

    @GET("anime/oploverz/anime/{slug}")
    suspend fun oploverzDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiOploverzDetailResponse>

    @GET("anime/oploverz/episode/{slug}")
    suspend fun oploverzEpisode(
        @Path("slug") slug: String
    ): Response<SankavollereiOploverzEpisodeResponse>

    // ── ANIME — NIMEGAMI ───────────────────────────────────────

    @GET("anime/nimegami/home")
    suspend fun nimegamiHome(): Response<SankavollereiNimegamiResponse>

    @GET("anime/nimegami/list")
    suspend fun nimegamiList(): Response<SankavollereiNimegamiResponse>

    @GET("anime/nimegami/search/anime")
    suspend fun nimegamiSearch(@Query("q") query: String): Response<SankavollereiNimegamiResponse>

    @GET("anime/nimegami/detail/{slug}")
    suspend fun nimegamiDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiNimegamiDetailResponse>

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
    ): Response<SankavollereiComicDetailResponse2>

    /** Chapter komik (field images/imagesproxy). */
    @GET("comic/chapter/{segment}")
    suspend fun comicChapter(
        @Path("segment") segment: String
    ): Response<SankavollereiComicChapterResponse2>

    // ── KOMIK — MULTI SOURCE ───────────────────────────────────
    // Setiap source punya path terpisah (latest / populer / detail / chapter).

    @GET("comic/komikindo/latest/{page}")
    suspend fun komikindoLatest(@Path("page") page: Int): Response<SankavollereiComicListResponse>

    @GET("comic/komikindo/populer/{page}")
    suspend fun komikindoPopular(@Path("page") page: Int): Response<SankavollereiComicListResponse>

    @GET("comic/komikindo/search/{query}/{page}")
    suspend fun komikindoSearch(
        @Path("query") query: String,
        @Path("page") page: Int
    ): Response<SankavollereiComicListResponse>

    @GET("comic/komikindo/detail/{slug}")
    suspend fun komikindoDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse2>

    @GET("comic/komikindo/chapter/{slug}")
    suspend fun komikindoChapter(
        @Path("slug") slug: String
    ): Response<SankavollereiComicChapterResponse2>

    @GET("comic/bacakomik/latest")
    suspend fun bacakomikLatest(): Response<SankavollereiComicListResponse>

    @GET("comic/bacakomik/populer")
    suspend fun bacakomikPopular(): Response<SankavollereiComicListResponse>

    @GET("comic/bacakomik/search/{query}")
    suspend fun bacakomikSearch(
        @Path("query") query: String
    ): Response<SankavollereiComicListResponse>

    @GET("comic/bacakomik/detail/{slug}")
    suspend fun bacakomikDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse2>

    @GET("comic/bacakomik/chapter/{slug}")
    suspend fun bacakomikChapter(
        @Path("slug") slug: String
    ): Response<SankavollereiComicChapterResponse2>

    @GET("comic/kiryuu/latest")
    suspend fun kiryuuLatest(): Response<SankavollereiComicListResponse>

    @GET("comic/kiryuu/popular")
    suspend fun kiryuuPopular(): Response<SankavollereiComicListResponse>

    @GET("comic/kiryuu/search/{query}")
    suspend fun kiryuuSearch(
        @Path("query") query: String
    ): Response<SankavollereiComicListResponse>

    @GET("comic/kiryuu/manga/{slug}")
    suspend fun kiryuuDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse2>

    @GET("comic/kiryuu/chapter/{slug}")
    suspend fun kiryuuChapter(
        @Path("slug") slug: String
    ): Response<SankavollereiComicChapterResponse2>

    @GET("comic/komikstation/home")
    suspend fun komikstationHome(): Response<SankavollereiComicListResponse>

    @GET("comic/komikstation/popular")
    suspend fun komikstationPopular(): Response<SankavollereiComicListResponse>

    @GET("comic/komikstation/manga/{slug}")
    suspend fun komikstationDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse2>

    @GET("comic/komikstation/chapter/{slug}")
    suspend fun komikstationChapter(
        @Path("slug") slug: String
    ): Response<SankavollereiComicChapterResponse2>

    @GET("comic/westmanga/latest")
    suspend fun westmangaLatest(): Response<SankavollereiComicListResponse>

    @GET("comic/westmanga/popular")
    suspend fun westmangaPopular(): Response<SankavollereiComicListResponse>

    @GET("comic/westmanga/completed")
    suspend fun westmangaCompleted(): Response<SankavollereiComicListResponse>

    @GET("comic/westmanga/home")
    suspend fun westmangaHome(): Response<SankavollereiComicListResponse>

    @GET("comic/westmanga/added")
    suspend fun westmangaAdded(): Response<SankavollereiComicListResponse>

    @GET("comic/westmanga/az")
    suspend fun westmangaAz(): Response<SankavollereiComicListResponse>

    @GET("comic/westmanga/colored")
    suspend fun westmangaColored(): Response<SankavollereiComicListResponse>

    @GET("comic/westmanga/genres")
    suspend fun westmangaGenres(): Response<SankavollereiGenreResponse>

    @GET("comic/westmanga/detail/{slug}")
    suspend fun westmangaDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse2>

    @GET("comic/westmanga/chapter/{slug}")
    suspend fun westmangaChapter(
        @Path("slug") slug: String
    ): Response<SankavollereiComicChapterResponse2>

    @GET("comic/shinigami/latest")
    suspend fun shinigamiLatest(): Response<SankavollereiComicListResponse>

    @GET("comic/shinigami/popular")
    suspend fun shinigamiPopular(): Response<SankavollereiComicListResponse>

    @GET("comic/shinigami/home")
    suspend fun shinigamiHome(): Response<SankavollereiComicListResponse>

    @GET("comic/shinigami/list")
    suspend fun shinigamiList(): Response<SankavollereiComicListResponse>

    @GET("comic/shinigami/genres")
    suspend fun shinigamiGenres(): Response<SankavollereiGenreResponse>

    @GET("comic/shinigami/detail/{slug}")
    suspend fun shinigamiDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse2>

    @GET("comic/shinigami/chapters/{slug}")
    suspend fun shinigamiChapter(
        @Path("slug") slug: String
    ): Response<SankavollereiComicChapterResponse2>

    @GET("comic/mangasusuku/latest")
    suspend fun mangasusukuLatest(): Response<SankavollereiComicListResponse>

    @GET("comic/mangasusuku/popular")
    suspend fun mangasusukuPopular(): Response<SankavollereiComicListResponse>

    @GET("comic/mangasusuku/home")
    suspend fun mangasusukuHome(): Response<SankavollereiComicListResponse>

    @GET("comic/mangasusuku/list")
    suspend fun mangasusukuList(): Response<SankavollereiComicListResponse>

    @GET("comic/mangasusuku/genres")
    suspend fun mangasusukuGenres(): Response<SankavollereiGenreResponse>

    @GET("comic/mangasusuku/detail/{slug}")
    suspend fun mangasusukuDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse2>

    @GET("comic/mangasusuku/chapter/{slug}")
    suspend fun mangasusukuChapter(
        @Path("slug") slug: String
    ): Response<SankavollereiComicChapterResponse2>

    @GET("comic/mangakita/home")
    suspend fun mangakitaHome(): Response<SankavollereiComicListResponse>

    @GET("comic/mangakita/daftar-manga")
    suspend fun mangakitaList(): Response<SankavollereiComicListResponse>

    @GET("comic/mangakita/genres")
    suspend fun mangakitaGenres(): Response<SankavollereiGenreResponse>

    @GET("comic/mangakita/genres/{genre}")
    suspend fun mangakitaGenre(
        @Path("genre") genre: String
    ): Response<SankavollereiComicListResponse>

    @GET("comic/mangakita/detail/{slug}")
    suspend fun mangakitaDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse2>

    @GET("comic/maid/home")
    suspend fun maidHome(): Response<SankavollereiComicListResponse>

    @GET("comic/maid/list")
    suspend fun maidList(): Response<SankavollereiComicListResponse>

    @GET("comic/maid/latest")
    suspend fun maidLatest(): Response<SankavollereiComicListResponse>

    @GET("comic/maid/genres")
    suspend fun maidGenres(): Response<SankavollereiGenreResponse>

    @GET("comic/maid/genres/{genre}")
    suspend fun maidGenre(
        @Path("genre") genre: String
    ): Response<SankavollereiComicListResponse>

    @GET("comic/maid/chapter/{slug}")
    suspend fun maidChapter(
        @Path("slug") slug: String
    ): Response<SankavollereiComicChapterResponse2>

    @GET("comic/softkomik/home")
    suspend fun softkomikHome(): Response<SankavollereiComicListResponse>

    @GET("comic/softkomik/list")
    suspend fun softkomikList(): Response<SankavollereiComicListResponse>

    @GET("comic/softkomik/completed")
    suspend fun softkomikCompleted(): Response<SankavollereiComicListResponse>

    @GET("comic/softkomik/genres")
    suspend fun softkomikGenres(): Response<SankavollereiGenreResponse>

    @GET("comic/softkomik/detail/{slug}")
    suspend fun softkomikDetail(
        @Path("slug") slug: String
    ): Response<SankavollereiComicDetailResponse2>

    @GET("comic/softkomik/chapter/{slug}/{part}")
    suspend fun softkomikChapter(
        @Path("slug") slug: String,
        @Path("part") part: String
    ): Response<SankavollereiComicChapterResponse2>

    // ── KOMIK — global (cross-source) ──────────────────────────

    @GET("comic/genre/{genre}")
    suspend fun comicGenre(@Path("genre") genre: String): Response<SankavollereiComicListResponse>

    @GET("comic/genres")
    suspend fun comicGenres(): Response<SankavollereiGenreResponse>

    @GET("comic/recommendations")
    suspend fun comicRecommendations(): Response<SankavollereiComicListResponse>

    @GET("comic/advanced-search")
    suspend fun comicAdvancedSearch(
        @Query("q") query: String
    ): Response<SankavollereiComicListResponse>

    @GET("comic/browse")
    suspend fun comicBrowse(@Query("page") page: Int = 1): Response<SankavollereiComicListResponse>

    @GET("comic/pustaka")
    suspend fun comicPustaka(@Query("page") page: Int = 1): Response<SankavollereiComicListResponse>

    @GET("comic/homepage")
    suspend fun comicHomepage(): Response<SankavollereiComicListResponse>

    @GET("comic/realtime")
    suspend fun comicRealtime(): Response<SankavollereiComicListResponse>

    @GET("comic/berwarna")
    suspend fun comicBerwarna(@Query("page") page: Int = 1): Response<SankavollereiComicListResponse>

    @GET("comic/scroll")
    suspend fun comicScroll(@Query("page") page: Int = 1): Response<SankavollereiComicListResponse>

    @GET("comic/unlimited")
    suspend fun comicUnlimited(@Query("page") page: Int = 1): Response<SankavollereiComicListResponse>

    @GET("comic/favorites")
    suspend fun comicFavorites(): Response<SankavollereiComicListResponse>

    @GET("comic/stats")
    suspend fun comicStats(): Response<SankavollereiComicListResponse>

    @GET("comic/analytics")
    suspend fun comicAnalytics(): Response<SankavollereiComicListResponse>

    @GET("comic/health")
    suspend fun comicHealth(): Response<SankavollereiComicListResponse>

    @GET("comic/fullstats")
    suspend fun comicFullstats(): Response<SankavollereiComicListResponse>

    @GET("comic/comparison")
    suspend fun comicComparison(): Response<SankavollereiComicListResponse>

    @GET("comic/infinite")
    suspend fun comicInfinite(@Query("page") page: Int = 1): Response<SankavollereiComicListResponse>

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
// ═══════════════════════════════════════════════════════════════
// MODEL — MULTI SOURCE (samehadaku / oploverz / nimegami)
// ═══════════════════════════════════════════════════════════════

/** Bentuk umum: data.recent/popular/completed/animeList. */
@JsonClass(generateAdapter = true)
data class SankavollereiSourceHomeResponse(
    val status: String? = null,
    val ok: Boolean? = null,
    val source: String? = null,
    val data: SankavollereiSourceHomeData? = null,
    val animeList: List<SankavollereiAnimeItem>? = null,
    val results: List<SankavollereiAnimeItem>? = null
) {
    val allItems: List<SankavollereiAnimeItem>
        get() {
            val out = ArrayList<SankavollereiAnimeItem>()
            data?.let { d ->
                d.recent?.animeList?.let { out.addAll(it) }
                d.popular?.animeList?.let { out.addAll(it) }
                d.completed?.animeList?.let { out.addAll(it) }
                d.ongoing?.animeList?.let { out.addAll(it) }
            }
            animeList?.let { out.addAll(it) }
            results?.let { out.addAll(it) }
            return out.distinctBy { it.title to it.href }
        }
}

@JsonClass(generateAdapter = true)
data class SankavollereiSourceHomeData(
    val recent: SankavollereiAnimeGroup? = null,
    val popular: SankavollereiAnimeGroup? = null,
    val completed: SankavollereiAnimeGroup? = null,
    val ongoing: SankavollereiAnimeGroup? = null
)

/** Oploverz memakai anime_list di top-level. */
@JsonClass(generateAdapter = true)
data class SankavollereiOploverzResponse(
    val status: String? = null,
    val source: String? = null,
    val anime_list: List<SankavollereiOploverzItem>? = null,
    val data: List<SankavollereiOploverzItem>? = null
) {
    val allItems: List<SankavollereiOploverzItem>
        get() = (anime_list ?: data ?: emptyList())
            .distinctBy { it.slug to it.title }
}

@JsonClass(generateAdapter = true)
data class SankavollereiOploverzItem(
    val title: String? = null,
    val slug: String? = null,
    val poster: String? = null,
    val type: String? = null,
    val episode: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val oploverz_url: String? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiOploverzDetailResponse(
    val status: String? = null,
    val data: SankavollereiOploverzDetail? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiOploverzDetail(
    val title: String? = null,
    val poster: String? = null,
    val status: String? = null,
    val type: String? = null,
    val episodes: String? = null,
    val rating: String? = null,
    val synopsis: String? = null,
    val genres: List<String>? = null,
    val episode_list: List<SankavollereiOploverzEpisodeItem>? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiOploverzEpisodeItem(
    val title: String? = null,
    val slug: String? = null,
    val episode: String? = null,
    val date: String? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiOploverzEpisodeResponse(
    val status: String? = null,
    val source: String? = null,
    val episode_title: String? = null,
    val streams: List<SankavollereiStream>? = null,
    val downloads: List<SankavollereiStream>? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiStream(
    val name: String? = null,
    val resolution: String? = null,
    val url: String? = null
)

/** Nimegami: data = anime_list. */
@JsonClass(generateAdapter = true)
data class SankavollereiNimegamiResponse(
    val status: String? = null,
    val source: String? = null,
    val anime_list: List<SankavollereiNimegamiItem>? = null,
    val data: List<SankavollereiNimegamiItem>? = null
) {
    val allItems: List<SankavollereiNimegamiItem>
        get() = (anime_list ?: data ?: emptyList())
            .distinctBy { it.slug to it.title }
}

@JsonClass(generateAdapter = true)
data class SankavollereiNimegamiItem(
    val title: String? = null,
    val slug: String? = null,
    val poster: String? = null,
    val type: String? = null,
    val episode: String? = null,
    val status: String? = null,
    val nimegami_url: String? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiNimegamiDetailResponse(
    val status: String? = null,
    val source: String? = null,
    val detail: SankavollereiNimegamiDetail? = null,
    val streams_by_episode: Map<String, List<SankavollereiStream>>? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiNimegamiDetail(
    val title: String? = null,
    val poster: String? = null,
    val synopsis: String? = null,
    val genres: List<SankavollereiNimegamiGenre>? = null,
    val info: Map<String, String>? = null
) {
    val plainSynopsis: String? get() = synopsis?.takeIf { it.isNotBlank() }
    val genreNames: List<String> get() = genres.orEmpty().mapNotNull { it.name }
    val episodeCount: Int? get() = info?.get("episode")?.toIntOrNull()
    val statusName: String? get() = info?.get("status")
}

@JsonClass(generateAdapter = true)
data class SankavollereiNimegamiGenre(
    val name: String? = null,
    val slug: String? = null,
    val url: String? = null
)

/** samehadaku detail. */
@JsonClass(generateAdapter = true)
data class SankavollereiSamehadakuDetailResponse(
    val status: String? = null,
    val data: SankavollereiSamehadakuDetail? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiSamehadakuDetail(
    val title: String? = null,
    val poster: String? = null,
    val status: String? = null,
    val synopsis: String? = null,
    val genres: List<SankavollereiGenreRef>? = null,
    val episodeList: List<SankavollereiEpisodeItem>? = null
) {
    val genreNames: List<String> get() = genres.orEmpty().mapNotNull { it.title }
    val plainSynopsis: String? get() = synopsis?.takeIf { it.isNotBlank() }
}

@JsonClass(generateAdapter = true)
data class SankavollereiGenreRef(
    val title: String? = null,
    val href: String? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiGenreResponse(
    val status: String? = null,
    val data: List<SankavollereiGenreItem>? = null,
    val genres: List<SankavollereiGenreItem>? = null
) {
    val all: List<SankavollereiGenreItem> get() = (data ?: genres ?: emptyList())
        .distinctBy { it.slug ?: it.title ?: it.name }
}

@JsonClass(generateAdapter = true)
data class SankavollereiGenreItem(
    val title: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val href: String? = null,
    val url: String? = null
) {
    val label: String? get() = title ?: name
}

// ═══════════════════════════════════════════════════════════════
// MODEL — KOMIK MULTI SOURCE
// ═══════════════════════════════════════════════════════════════

/** Detail komik per-source: bisa flat atau di dalam `data`. */
@JsonClass(generateAdapter = true)
data class SankavollereiComicDetailResponse2(
    val status: Boolean? = null,
    val success: Boolean? = null,
    val message: String? = null,
    val slug: String? = null,
    val title: String? = null,
    val image: String? = null,
    val cover: String? = null,
    val thumbnail: String? = null,
    val synopsis: String? = null,
    val synopsis_full: String? = null,
    val genres: List<String>? = null,
    val type: String? = null,
    val status_text: String? = null,
    val rating: String? = null,
    val chapters: List<SankavollereiComicChapterItem2>? = null,
    val data: SankavollereiComicDetailData2? = null
) {
    val bestCover: String? get() = cover ?: image ?: thumbnail ?: data?.cover ?: data?.thumbnail ?: data?.image
    val plainTitle: String? get() = title ?: data?.title
    val plainSynopsis: String? get() = synopsis ?: synopsis_full ?: data?.synopsis
    val allChapters: List<SankavollereiComicChapterItem2>
        get() = chapters ?: data?.chapters ?: emptyList()
    val allGenres: List<String> get() = genres ?: data?.genres ?: emptyList()
    val ratingValue: String? get() = rating ?: data?.rating
    val statusText: String? get() = status_text ?: data?.status ?: data?.status_text
}

@JsonClass(generateAdapter = true)
data class SankavollereiComicDetailData2(
    val title: String? = null,
    val cover: String? = null,
    val thumbnail: String? = null,
    val image: String? = null,
    val synopsis: String? = null,
    val genres: List<String>? = null,
    val type: String? = null,
    val status: String? = null,
    val status_text: String? = null,
    val rating: String? = null,
    val chapters: List<SankavollereiComicChapterItem2>? = null,
    val chapter: List<SankavollereiComicChapterItem2>? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiComicChapterItem2(
    val chapter: String? = null,
    val title: String? = null,
    val slug: String? = null,
    val link: String? = null,
    val href: String? = null,
    val date: String? = null
) {
    val label: String? get() = chapter ?: title
}

/** Chapter per-source: images bisa array of object {id,url} atau array of string. */
@JsonClass(generateAdapter = true)
data class SankavollereiComicChapterResponse2(
    val status: Boolean? = null,
    val success: Boolean? = null,
    val creator: String? = null,
    val manga_title: String? = null,
    val chapter_title: String? = null,
    val images: List<SankavollereiComicImageItem>? = null,
    val imagesproxy: List<String>? = null,
    val data: SankavollereiComicChapterData2? = null
) {
    val allImages: List<String>
        get() = buildList {
            images.orEmpty().forEach { item ->
                item.url?.let { add(it) }
            }
            if (isEmpty()) addAll(imagesproxy.orEmpty())
            data?.let { d ->
                if (isEmpty()) addAll(d.images.orEmpty())
            }
        }
    val title: String? get() = chapter_title ?: data?.title
}

@JsonClass(generateAdapter = true)
data class SankavollereiComicImageItem(
    val id: Int? = null,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiComicChapterData2(
    val title: String? = null,
    val chapter: String? = null,
    val images: List<String>? = null,
    val imagesproxy: List<String>? = null
)

/** Jadwal tayang anime per hari. */
@JsonClass(generateAdapter = true)
data class SankavollereiScheduleResponse(
    val status: String? = null,
    val data: List<SankavollereiScheduleDay>? = null
)

@JsonClass(generateAdapter = true)
data class SankavollereiScheduleDay(
    val day: String? = null,
    val anime_list: List<SankavollereiAnimeItem>? = null
)
