package com.jayra.app.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Pencarian musik: https://api.siputzx.my.id/api/s/applemusic?query=...&region=id
 * JSON: { status, data: [ { title, artist, link, image } ] }
 */
interface AppleMusicApi {

    @GET("api/s/applemusic")
    suspend fun search(
        @Query("query") query: String,
        @Query("region") region: String = "id"
    ): Response<AppleMusicResponse>

    companion object {
        private const val BASE_URL = "https://api.siputzx.my.id/"
        private const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        fun create(): AppleMusicApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", BROWSER_UA)
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
                .create(AppleMusicApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class AppleMusicResponse(
    val status: Boolean? = null,
    val data: List<AppleMusicItem>? = null
)

@JsonClass(generateAdapter = true)
data class AppleMusicItem(
    val title: String? = null,
    val artist: String? = null,
    val link: String? = null,
    val image: String? = null
)

/**
 * Apple Music search tidak mengembalikan URL audio. Untuk bisa diputar di app,
 * kita baca halaman album Apple Music (HTML publik) lalu ambil `previewUrl`
 * (preview AAC 30 detik) + durasi untuk track yang sesuai.
 *
 * Ini BUKAN WebView: hanya HTTP GET + parsing string, hasilnya tetap dirender
 * oleh Composable kita sendiri.
 */
object AppleMusicPreviewResolver {
    data class TrackMeta(val audioUrl: String?, val durationMs: Long?)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "text/html")
                    .build()
            )
        }
        .build()

    private val cache = ConcurrentHashMap<String, Map<String, TrackMeta>>()

    fun songId(link: String?): String? {
        if (link.isNullOrBlank()) return null
        return Regex("[?&]i=(\\d+)").find(link)?.groupValues?.getOrNull(1)
    }

    fun albumKey(link: String?): String? {
        if (link.isNullOrBlank()) return null
        val cut = link.substringBefore("?")
        return cut.substringBeforeLast("/").ifBlank { cut }
    }

    /**
     * Ambil semua track meta dari satu halaman album Apple Music.
     * Map key = storeAdamID.
     */
    suspend fun resolveAlbum(link: String?): Map<String, TrackMeta> = withContext(Dispatchers.IO) {
        val key = albumKey(link) ?: return@withContext emptyMap()
        cache[key]?.let { return@withContext it }
        try {
            val request = Request.Builder().url(link!!).get().build()
            val html = client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyMap()
                resp.body?.string().orEmpty()
            }
            if (html.isBlank()) return@withContext emptyMap()

            val result = mutableMapOf<String, TrackMeta>()
            val previewRegex = Regex("\"previewUrl\":\"(https:[^\"]+)\"")
            previewRegex.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                val start = (match.range.first - 3000).coerceAtLeast(0)
                val window = html.substring(start, match.range.first + 200)
                val id = Regex("\"storeAdamID\":\"(\\d+)\"").findAll(window)
                    .lastOrNull()?.groupValues?.getOrNull(1)
                val durationMs = Regex("\"duration\":(\\d{4,9})").findAll(window)
                    .lastOrNull()?.groupValues?.getOrNull(1)?.toLongOrNull()
                if (!id.isNullOrBlank() && !result.containsKey(id)) {
                    result[id] = TrackMeta(url, durationMs)
                }
            }
            if (result.isNotEmpty()) cache[key] = result
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun resolveTrack(link: String?): TrackMeta? {
        val id = songId(link) ?: return null
        val album = resolveAlbum(link)
        return album[id]
    }
}
