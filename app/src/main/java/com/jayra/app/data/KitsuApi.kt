package com.jayra.app.data

import com.squareup.moshi.Json
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
 * Kitsu API (https://kitsu.io/api/edge) — pengganti API anime lama yang sudah mati.
 * Gratis, tanpa API key, dan memakai sertifikat SSL yang valid.
 */
interface KitsuApi {

    @GET("anime")
    suspend fun list(
        @Query("page[limit]") limit: Int = 20,
        @Query("sort") sort: String = "-userCount",
        @Query("filter[status]") status: String? = null
    ): Response<KitsuListResponse>

    @GET("anime")
    suspend fun search(
        @Query("filter[text]") query: String,
        @Query("page[limit]") limit: Int = 20
    ): Response<KitsuListResponse>

    @GET("anime/{id}")
    suspend fun detail(@Path("id") id: String): Response<KitsuSingleResponse>

    companion object {
        private const val BASE_URL = "https://kitsu.io/api/edge/"

        fun create(): KitsuApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Accept", "application/vnd.api+json")
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
                .create(KitsuApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class KitsuListResponse(val data: List<KitsuAnime>? = null)

@JsonClass(generateAdapter = true)
data class KitsuSingleResponse(val data: KitsuAnime? = null)

@JsonClass(generateAdapter = true)
data class KitsuAnime(
    val id: String? = null,
    val attributes: KitsuAttributes? = null
)

@JsonClass(generateAdapter = true)
data class KitsuAttributes(
    @Json(name = "canonicalTitle") val canonicalTitle: String? = null,
    @Json(name = "titles") val titles: Map<String, String>? = null,
    val synopsis: String? = null,
    val status: String? = null,
    @Json(name = "episodeCount") val episodeCount: Int? = null,
    @Json(name = "averageRating") val averageRating: String? = null,
    @Json(name = "startDate") val startDate: String? = null,
    val subtype: String? = null,
    @Json(name = "posterImage") val posterImage: KitsuImage? = null,
    @Json(name = "coverImage") val coverImage: KitsuImage? = null
) {
    fun displayTitle(): String =
        canonicalTitle?.takeIf { it.isNotBlank() }
            ?: titles?.get("en")
            ?: titles?.get("en_jp")
            ?: titles?.values?.firstOrNull()
            ?: "Anime"
}

@JsonClass(generateAdapter = true)
data class KitsuImage(
    val tiny: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    val original: String? = null
) {
    fun best(): String = large ?: original ?: medium ?: small ?: tiny ?: ""
}
