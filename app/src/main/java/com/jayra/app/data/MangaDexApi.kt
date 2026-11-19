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
 * MangaDex API v5 (https://api.mangadex.org) — pengganti API komik lama.
 * Gratis, tanpa API key, sertifikat SSL valid.
 */
interface MangaDexApi {

    @GET("manga")
    suspend fun list(
        @Query("limit") limit: Int = 20,
        @Query("includes[]") includes: List<String> = listOf("cover_art"),
        @Query("order[followedCount]") order: String = "desc",
        @Query("contentRating[]") ratings: List<String> = listOf("safe", "suggestive")
    ): Response<MdxListResponse>

    @GET("manga")
    suspend fun search(
        @Query("title") title: String,
        @Query("limit") limit: Int = 20,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): Response<MdxListResponse>

    @GET("manga/{id}")
    suspend fun detail(
        @Path("id") id: String,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): Response<MdxSingleResponse>

    @GET("manga/{id}/feed")
    suspend fun chapters(
        @Path("id") id: String,
        @Query("limit") limit: Int = 100,
        @Query("translatedLanguage[]") languages: List<String> = listOf("en"),
        @Query("order[chapter]") order: String = "desc",
        @Query("includeExternalUrl") includeExternal: Int = 0
    ): Response<MdxChapterListResponse>

    @GET("at-home/server/{chapterId}")
    suspend fun chapterImages(@Path("chapterId") chapterId: String): Response<MdxAtHomeResponse>

    companion object {
        private const val BASE_URL = "https://api.mangadex.org/"

        fun create(): MangaDexApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor { chain ->
                    // MangaDex menolak request tanpa User-Agent yang jelas (HTTP 400),
                    // dan justru menantang bot kalau UA-nya mirip browser.
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "JayraApp/1.0 (Android; +https://jayra.app)")
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
                .create(MangaDexApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class MdxListResponse(
    val result: String? = null,
    val data: List<MdxManga>? = null,
    val total: Int? = null
)

@JsonClass(generateAdapter = true)
data class MdxSingleResponse(
    val result: String? = null,
    val data: MdxManga? = null
)

@JsonClass(generateAdapter = true)
data class MdxChapterListResponse(
    val result: String? = null,
    val data: List<MdxChapter>? = null
)

@JsonClass(generateAdapter = true)
data class MdxManga(
    val id: String? = null,
    val attributes: MdxMangaAttributes? = null,
    val relationships: List<MdxRelationship>? = null
) {
    fun coverUrl(): String? {
        val fileName = relationships
            ?.firstOrNull { it.type == "cover_art" }
            ?.attributes?.fileName
            ?: return null
        // Nama file kadang mengandung spasi/karakter khusus → wajib di-encode.
        val encoded = android.net.Uri.encode(fileName)
        return "https://uploads.mangadex.org/covers/$id/$encoded.512.jpg"
    }
}

@JsonClass(generateAdapter = true)
data class MdxMangaAttributes(
    val title: Map<String, String>? = null,
    val altTitles: List<Map<String, String>>? = null,
    val description: Map<String, String>? = null,
    val status: String? = null,
    val year: Int? = null,
    val lastChapter: String? = null
) {
    fun displayTitle(): String =
        title?.get("en")
            ?: title?.values?.firstOrNull()
            ?: altTitles?.firstOrNull()?.get("en")
            ?: altTitles?.firstOrNull()?.values?.firstOrNull()
            ?: "Komik"

    fun displayDescription(): String =
        description?.get("en") ?: description?.values?.firstOrNull() ?: ""
}

@JsonClass(generateAdapter = true)
data class MdxRelationship(
    val id: String? = null,
    val type: String? = null,
    val attributes: MdxRelationshipAttributes? = null
)

@JsonClass(generateAdapter = true)
data class MdxRelationshipAttributes(
    val fileName: String? = null
)

@JsonClass(generateAdapter = true)
data class MdxChapter(
    val id: String? = null,
    val attributes: MdxChapterAttributes? = null
)

@JsonClass(generateAdapter = true)
data class MdxChapterAttributes(
    val title: String? = null,
    val chapter: String? = null,
    val volume: String? = null,
    @com.squareup.moshi.Json(name = "translatedLanguage") val translatedLanguage: String? = null,
    @com.squareup.moshi.Json(name = "externalUrl") val externalUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class MdxAtHomeResponse(
    val baseUrl: String? = null,
    val chapter: MdxAtHomeChapter? = null
)

@JsonClass(generateAdapter = true)
data class MdxAtHomeChapter(
    val hash: String? = null,
    val data: List<String>? = null,
    val dataSaver: List<String>? = null
)
