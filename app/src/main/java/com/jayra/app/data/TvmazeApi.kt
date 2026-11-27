package com.jayra.app.data

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

/**
 * TVMaze API (https://api.tvmaze.com) — pengganti API drama lama yang
 * sertifikatnya bermasalah (CertPathValidatorException).
 * Gratis, tanpa API key, sertifikat SSL valid.
 */
interface TvmazeApi {

    @GET("shows")
    suspend fun shows(@Query("page") page: Int = 0): Response<List<TvmazeShow>>

    @GET("search/shows")
    suspend fun search(@Query("q") query: String): Response<List<TvmazeSearchItem>>

    @GET("shows/{id}")
    suspend fun detail(
        @Path("id") id: Long,
        @Query("embed") embed: String = "episodes"
    ): Response<TvmazeShow>

    companion object {
        private const val BASE_URL = "https://api.tvmaze.com/"
        private const val REFERRER = "https://www.tvmaze.com/"

        fun create(): TvmazeApi {
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
                .create(TvmazeApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class TvmazeSearchItem(
    val score: Double? = null,
    val show: TvmazeShow? = null
)

@JsonClass(generateAdapter = true)
data class TvmazeShow(
    val id: Long? = null,
    val name: String? = null,
    val type: String? = null,
    val language: String? = null,
    val genres: List<String>? = null,
    val status: String? = null,
    val premiered: String? = null,
    val summary: String? = null,
    val image: TvmazeImage? = null,
    val rating: TvmazeRating? = null,
    @com.squareup.moshi.Json(name = "_embedded") val embedded: TvmazeEmbedded? = null
)

@JsonClass(generateAdapter = true)
data class TvmazeImage(
    val medium: String? = null,
    val original: String? = null
)

@JsonClass(generateAdapter = true)
data class TvmazeRating(val average: Double? = null)

@JsonClass(generateAdapter = true)
data class TvmazeEmbedded(
    val episodes: List<TvmazeEpisode>? = null
)

@JsonClass(generateAdapter = true)
data class TvmazeEpisode(
    val id: Long? = null,
    val name: String? = null,
    val season: Int? = null,
    val number: Int? = null
)

private fun stripHtml(html: String): String = html
    .replace(Regex("<[^>]*>"), "")
    .replace("&nbsp;", " ")
    .replace("&", "&")
    .replace("<", "<")
    .replace(">", ">")
    .replace("\"", "\\\"")
    .trim()