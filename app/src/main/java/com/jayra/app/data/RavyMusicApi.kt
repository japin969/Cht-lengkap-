package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

/**
 * Ravy Music API — https://rayy-music.vercel.app
 *
 * Endpoint resmi (diambil dari player.js web tersebut):
 *  - GET  /api/search?query=...      -> daftar lagu (title, videoId, thumbnail,
 *                                       artist, album, duration)
 *  - GET  /api/suggest?q=...         -> saran pencarian
 *  - POST /api/ytplay {query: url}   -> { result.download.audio = URL mp3 }
 *
 * Tanpa API key. Cukup User-Agent + Accept.
 */
interface RavyMusicApi {

    @GET("api/search")
    suspend fun search(@Query("query") query: String): Response<RavySearchResponse>

    @GET("api/suggest")
    suspend fun suggest(@Query("q") query: String): Response<List<String>>

    @POST("api/ytplay")
    suspend fun ytPlay(@Body body: RavyYtPlayRequest): Response<RavyYtPlayResponse>

    companion object {
        private const val BASE_URL = "https://rayy-music.vercel.app/"
        private const val UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        @Volatile private var instance: RavyMusicApi? = null
        fun create(): RavyMusicApi = instance ?: synchronized(this) {
            instance ?: build().also { instance = it }
        }

        private fun build(): RavyMusicApi {
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
                .create(RavyMusicApi::class.java)
        }
    }
}

// Alias typo-safe

@JsonClass(generateAdapter = true)
data class RavySearchResponse(
    @Json(name = "status") val status: Boolean? = null,
    @Json(name = "creator") val creator: String? = null,
    @Json(name = "result") val result: RavySearchResult? = null
)

@JsonClass(generateAdapter = true)
data class RavySearchResult(
    @Json(name = "query") val query: String? = null,
    @Json(name = "totalSongs") val totalSongs: Int? = null,
    @Json(name = "songs") val songs: List<RavySong>? = null
)

@JsonClass(generateAdapter = true)
data class RavySong(
    @Json(name = "title") val title: String? = null,
    @Json(name = "videoId") val videoId: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "artistId") val artistId: String? = null,
    @Json(name = "album") val album: String? = null,
    @Json(name = "albumId") val albumId: String? = null,
    @Json(name = "duration") val duration: String? = null
) {
    /** URL youtube yang dipakai untuk resolve mp3. */
    val playUrl: String? get() = url ?: videoId?.let { "https://music.youtube.com/watch?v=$it" }
}

@JsonClass(generateAdapter = true)
data class RavyYtPlayRequest(
    @Json(name = "query") val query: String
)

@JsonClass(generateAdapter = true)
data class RavyYtPlayResponse(
    @Json(name = "status") val status: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "result") val result: RavyYtPlayResult? = null
)

@JsonClass(generateAdapter = true)
data class RavyYtPlayResult(
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "download") val download: RavyDownload? = null
)

@JsonClass(generateAdapter = true)
data class RavyDownload(
    @Json(name = "audio") val audio: String? = null
)