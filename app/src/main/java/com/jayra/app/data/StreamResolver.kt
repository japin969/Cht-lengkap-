package com.jayra.app.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * StreamResolver — ekstrak direct video URL (mp4/m3u8) dari halaman embed
 * (blogger video.g, ok.ru, dst) SECARA NATIVE (OkHttp + regex), TANPA WebView.
 *
 * Mengembalikan null bila tidak ditemukan (UI tampilkan error jelas, bukan hitam).
 */
object StreamResolver {

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", UA)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()
            )
        }
        .build()

    private val MP4 = Pattern.compile("https?://[^\"'\\\\ ]+?\\.mp4[^\"'\\\\ ]*")
    private val M3U8 = Pattern.compile("https?://[^\"'\\\\ ]+?\\.m3u8[^\"'\\\\ ]*")
    private val GOOGLEVIDEO = Pattern.compile("https?://[^\"'\\\\ ]*googlevideo\\.com/videoplayback[^\"'\\\\ ]*")

    /** Cek apakah URL adalah direct media (mp4/m3u8) yang bisa diputar ExoPlayer. */
    fun isDirect(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.lowercase()
        return u.endsWith(".mp4") || u.contains(".mp4?") || u.endsWith(".m3u8") ||
            u.contains(".m3u8?") || u.contains("googlevideo.com/videoplayback")
    }

    /**
     * Jika url adalah embed HTML, fetch & ekstrak direct media URL.
     * Jika sudah direct, kembalikan apa adanya.
     */
    fun resolve(url: String): String? {
        if (isDirect(url)) return url
        if (url.isNullOrBlank() || !url.startsWith("http")) return null
        return try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string() ?: return null
                // Decode unicode escapes \u003d -> = , \u0026 -> &
                val decoded = html
                    .replace("\\u003d", "=")
                    .replace("\\u0026", "&")
                    .replace("\\u003d", "=")
                    .replace("&quot;", "\"")
                    .replace("\\/", "/")
                // Prioritas: googlevideo > m3u8 > mp4
                firstMatch(GOOGLEVIDEO, decoded)
                    ?: firstMatch(M3U8, decoded)
                    ?: firstMatch(MP4, decoded)
            }
        } catch (e: Exception) {
            if (Log.isLoggable("StreamResolver", Log.DEBUG)) Log.d("StreamResolver", "resolve fail: ${e.message}")
            null
        }
    }

    private fun firstMatch(p: Pattern, s: String): String? {
        val m = p.matcher(s)
        return if (m.find()) m.group()?.replace("&amp;", "&") else null
    }
}