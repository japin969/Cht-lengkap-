package com.jayra.app.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Deezer API (https://api.deezer.com) — pencarian lagu gratis tanpa API key,
 * menyediakan cover dan preview MP3 30 detik, dengan sertifikat SSL valid.
 */
interface DeezerApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25
    ): Response<DeezerSearchResponse>

    companion object {
        private const val BASE_URL = "https://api.deezer.com/"

        fun create(): DeezerApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(DeezerApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class DeezerSearchResponse(
    val data: List<DeezerTrack>? = null,
    val total: Int? = null
)

@JsonClass(generateAdapter = true)
data class DeezerTrack(
    val id: Long? = null,
    val title: String? = null,
    val duration: Int? = null,
    val preview: String? = null,
    val artist: DeezerArtist? = null,
    val album: DeezerAlbum? = null
)

@JsonClass(generateAdapter = true)
data class DeezerArtist(val name: String? = null)

@JsonClass(generateAdapter = true)
data class DeezerAlbum(
    val title: String? = null,
    @com.squareup.moshi.Json(name = "cover_medium") val coverMedium: String? = null,
    @com.squareup.moshi.Json(name = "cover_big") val coverBig: String? = null
)
