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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SiputzxApi {

    @GET("api/s/pinterest")
    suspend fun searchPinterest(
        @Query("query") query: String
    ): Response<PinterestResponse>

    @GET("api/s/applemusic")
    suspend fun searchAppleMusic(
        @Query("query") query: String,
        @Query("region") region: String = "id"
    ): Response<AppleMusicResponse>

    @GET("api/ai/gptoss120b")
    suspend fun askGptOss(
        @Query("prompt") prompt: String,
        @Query("system") system: String = "You are a helpful assistant.",
        @Query("temperature") temperature: Float = 0.7f
    ): Response<AIResponse>

    @GET("api/ai/deepseekr1")
    suspend fun askDeepseekR1(
        @Query("prompt") prompt: String,
        @Query("system") system: String = "You are a helpful assistant.",
        @Query("temperature") temperature: Float = 0.7f
    ): Response<AIResponse>

    companion object {
        private const val AI_BASE_URL = "https://api.siputzx.my.id/"
        private const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        fun create(): SiputzxApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .header("User-Agent", BROWSER_UA)
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(req)
                }
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(AI_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SiputzxApi::class.java)
        }
    }
}

// Apple Music API Response
@JsonClass(generateAdapter = true)
data class AppleMusicResponse(
    @Json(name = "status") val status: Boolean? = null,
    @Json(name = "data") val data: List<AppleMusicTrack>? = null
)

@JsonClass(generateAdapter = true)
data class AppleMusicTrack(
    @Json(name = "title") val title: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "album") val album: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "artwork") val artwork: String? = null,
    @Json(name = "preview") val preview: String? = null,
    @Json(name = "url") val url: String? = null
) {
    fun toSong(fallbackId: String = System.currentTimeMillis().toString()): Song {
        return Song(
            id = fallbackId,
            title = title ?: "Lagu Tanpa Judul",
            artist = artist ?: "Artis Tidak Diketahui",
            album = album ?: "Single",
            duration = duration ?: "3:30",
            coverUrl = artwork ?: "",
            audioUrl = preview ?: url ?: "",
            source = "" // No source label - hidden from UI
        )
    }
}
