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
 * ComicApi — sumber resmi comic: https://api.nexadev.my.id (komik) +
 * https://www.sankavollerei.web.id (comic). Hanya 2 hostname ini (sesuai aturan).
 *
 * Catatan: endpoint detail/stream komik di kedua host sedang bermasalah (500/403),
 * jadi ViewModel menampilkan error state yang jelas (tidak crash).
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

            val nexadev = Retrofit.Builder()
                .baseUrl(NEXADEV_BASE).client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi)).build()

            val sanka = Retrofit.Builder()
                .baseUrl(SANKA_BASE).client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi)).build()

            // Gabung dua Retrofit ke satu interface via dynamic proxy sederhana:
            // buat dua instance interface lalu delegasikan.
            val n = nexadev.create(ComicApi::class.java)
            val s = sanka.create(ComicApi::class.java)
            return object : ComicApi {
                override suspend fun searchNexadev(q: String) = n.searchNexadev(q)
                override suspend fun listNexadev(page: Int) = n.listNexadev(page)
                override suspend fun searchSanka(q: String) = s.searchSanka(q)
                override suspend fun genreSanka(genre: String) = s.genreSanka(genre)
            }
        }
    }
}

// ── sankavollerei comic models ──
@JsonClass(generateAdapter = true)
data class SankaComicSearchResponse(
    val status: Boolean? = null,
    val creator: String? = null,
    val q: String? = null,
    val total: Int? = null,
    val data: List<SankaComicItem>? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicItem(
    val title: String? = null,
    val slug: String? = null,
    val href: String? = null,
    val thumbnail: String? = null,
    val type: String? = null,
    val genre: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicGenreResponse(
    val creator: String? = null,
    val genre: String? = null,
    val comics: List<SankaComicGenreItem>? = null
)

@JsonClass(generateAdapter = true)
data class SankaComicGenreItem(
    val title: String? = null,
    val link: String? = null,
    val image: String? = null,
    val chapter: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val genre: String? = null
)