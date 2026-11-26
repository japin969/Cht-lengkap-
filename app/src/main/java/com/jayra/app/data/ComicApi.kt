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
 * ComicApi — sumber comic:
 *  - https://api.nexadev.my.id (komik)
 *  - https://www.sankavollerei.web.id (comic, bacakomik, maid)
 * Hanya host diizinkan. SSL valid (Cloudflare), tanpa bypass.
 */
interface ComicApi {

    // ── nexadev (komik) ──
    @GET("komik")
    suspend fun searchNexadev(@Query("q") q: String): Response<KomikListResponse>

    @GET("komik")
    suspend fun listNexadev(@Query("page") page: Int = 1): Response<KomikListResponse>

    // ── sankavollerei (comic) ──
    @GET("comic/search")
    suspend fun searchSanka(@Query("q") q: String): Response<SankaComicSearchResponse>

    @GET("comic/genre/{genre}")
    suspend fun genreSanka(@Path("genre") genre: String): Response<SankaComicGenreResponse>

    // ── bacakomik ──
    @GET("comic/bacakomik/latest")
    suspend fun bkLatest(): Response<BkListResponse>

    @GET("comic/bacakomik/populer")
    suspend fun bkPopuler(): Response<BkListResponse>

    @GET("comic/bacakomik/top")
    suspend fun bkTop(): Response<BkListResponse>

    @GET("comic/bacakomik/recomen")
    suspend fun bkRecomen(): Response<BkListResponse>

    @GET("comic/bacakomik/search/{query}")
    suspend fun bkSearch(@Path("query") query: String): Response<BkListResponse>

    @GET("comic/bacakomik/detail/{slug}")
    suspend fun bkDetail(@Path("slug") slug: String): Response<BkDetailResponse>

    @GET("comic/bacakomik/chapter/{slug}")
    suspend fun bkChapter(@Path("slug") slug: String): Response<BkChapterResponse>

    // ── maid ──
    @GET("comic/maid/latest")
    suspend fun maidLatest(@Query("page") page: Int = 1): Response<MaidListResponse>

    @GET("comic/maid/list")
    suspend fun maidList(): Response<MaidListResponse>

    @GET("comic/maid/genres")
    suspend fun maidGenres(): Response<MaidGenreListResponse>

    @GET("comic/maid/genres/{slug}")
    suspend fun maidGenre(@Path("slug") slug: String, @Query("page") page: Int = 1): Response<MaidListResponse>

    @GET("comic/maid/search")
    suspend fun maidSearch(@Query("title") title: String, @Query("page") page: Int = 1): Response<MaidListResponse>

    @GET("comic/maid/manga/{slug}")
    suspend fun maidManga(@Path("slug") slug: String): Response<MaidMangaResponse>

    @GET("comic/maid/chapter/{slug}")
    suspend fun maidChapter(@Path("slug") slug: String): Response<BkChapterResponse>

    companion object {
        private const val NEXADEV_BASE = "https://api.nexadev.my.id/"
        private const val SANKA_BASE = "https://www.sankavollerei.web.id/"
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        fun create(): ComicApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", UA)
                            .header("Accept", "application/json")
                            .header("Referer", SANKA_BASE)
                            .build()
                    )
                }
                .build()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

            val n = Retrofit.Builder().baseUrl(NEXADEV_BASE).client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi)).build()
                .create(ComicApi::class.java)
            val s = Retrofit.Builder().baseUrl(SANKA_BASE).client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi)).build()
                .create(ComicApi::class.java)

            return object : ComicApi {
                override suspend fun searchNexadev(q: String) = n.searchNexadev(q)
                override suspend fun listNexadev(page: Int) = n.listNexadev(page)
                override suspend fun searchSanka(q: String) = s.searchSanka(q)
                override suspend fun genreSanka(genre: String) = s.genreSanka(genre)
                override suspend fun bkLatest() = s.bkLatest()
                override suspend fun bkPopuler() = s.bkPopuler()
                override suspend fun bkTop() = s.bkTop()
                override suspend fun bkRecomen() = s.bkRecomen()
                override suspend fun bkSearch(query: String) = s.bkSearch(query)
                override suspend fun bkDetail(slug: String) = s.bkDetail(slug)
                override suspend fun bkChapter(slug: String) = s.bkChapter(slug)
                override suspend fun maidLatest(page: Int) = s.maidLatest(page)
                override suspend fun maidList() = s.maidList()
                override suspend fun maidGenres() = s.maidGenres()
                override suspend fun maidGenre(slug: String, page: Int) = s.maidGenre(slug, page)
                override suspend fun maidSearch(title: String, page: Int) = s.maidSearch(title, page)
                override suspend fun maidManga(slug: String) = s.maidManga(slug)
                override suspend fun maidChapter(slug: String) = s.maidChapter(slug)
            }
        }
    }
}

// ── sankavollerei comic (existing) ──
@JsonClass(generateAdapter = true)
data class SankaComicSearchResponse(
    val status: Boolean? = null, val creator: String? = null, val q: String? = null,
    val total: Int? = null, val data: List<SankaComicItem>? = null
)
@JsonClass(generateAdapter = true)
data class SankaComicItem(
    val title: String? = null, val slug: String? = null, val href: String? = null,
    val thumbnail: String? = null, val type: String? = null, val genre: String? = null,
    val description: String? = null
)
@JsonClass(generateAdapter = true)
data class SankaComicGenreResponse(
    val creator: String? = null, val genre: String? = null, val comics: List<SankaComicGenreItem>? = null
)
@JsonClass(generateAdapter = true)
data class SankaComicGenreItem(
    val title: String? = null, val link: String? = null, val image: String? = null,
    val chapter: String? = null, val status: String? = null, val rating: String? = null, val genre: String? = null
)

// ── bacakomik ──
@JsonClass(generateAdapter = true)
data class BkListResponse(
    val creator: String? = null, val success: Boolean? = null,
    val komikList: List<BkItem>? = null, val hasNextPage: Boolean? = null, val currentPage: Int? = null
)
@JsonClass(generateAdapter = true)
data class BkItem(
    val title: String? = null, val slug: String? = null, val cover: String? = null,
    val chapter: String? = null, val date: String? = null, val type: String? = null
)
@JsonClass(generateAdapter = true)
data class BkDetailResponse(
    val creator: String? = null, val success: Boolean? = null, val detail: BkDetail? = null
)
@JsonClass(generateAdapter = true)
data class BkDetail(
    val title: String? = null, val cover: String? = null, val rating: String? = null,
    val otherTitle: String? = null, val status: String? = null, val type: String? = null,
    val synopsis: String? = null, val chapters: List<BkChapterItem>? = null
)
@JsonClass(generateAdapter = true)
data class BkChapterItem(
    val title: String? = null, val slug: String? = null, val date: String? = null
)
@JsonClass(generateAdapter = true)
data class BkChapterResponse(
    val creator: String? = null, val success: Boolean? = null, val title: String? = null,
    val images: List<String>? = null, val navigation: BkNavigation? = null
)
@JsonClass(generateAdapter = true)
data class BkNavigation(val next: String? = null, val prev: String? = null)

// ── maid ──
@JsonClass(generateAdapter = true)
data class MaidListResponse(
    val creator: String? = null, val success: Boolean? = null,
    val pagination: MaidPagination? = null, val data: List<MaidItem>? = null
)
@JsonClass(generateAdapter = true)
data class MaidPagination(
    val currentPage: Int? = null, val nextPage: Int? = null, val prevPage: Int? = null,
    val hasNext: Boolean? = null, val hasPrev: Boolean? = null
)
@JsonClass(generateAdapter = true)
data class MaidItem(
    val title: String? = null, val slug: String? = null, val banner: String? = null,
    val rating: String? = null, val latest_chapter: MaidLatestChapter? = null
)
@JsonClass(generateAdapter = true)
data class MaidLatestChapter(val title: String? = null, val time: String? = null)
@JsonClass(generateAdapter = true)
data class MaidGenreListResponse(
    val creator: String? = null, val success: Boolean? = null, val data: List<MaidGenre>? = null
)
@JsonClass(generateAdapter = true)
data class MaidGenre(val title: String? = null, val slug: String? = null, val count: String? = null)
@JsonClass(generateAdapter = true)
data class MaidMangaResponse(
    val creator: String? = null, val success: Boolean? = null, val data: MaidManga? = null
)
@JsonClass(generateAdapter = true)
data class MaidManga(
    val title: String? = null, val banner: String? = null, val description: String? = null,
    val type: String? = null, val status: String? = null, val rating: String? = null,
    val author: String? = null, val total_chapter: String? = null,
    val genres: List<String>? = null, val chapters: List<MaidChapter>? = null
)
@JsonClass(generateAdapter = true)
data class MaidChapter(
    val title: String? = null, val slug: String? = null, val release_time: String? = null
)