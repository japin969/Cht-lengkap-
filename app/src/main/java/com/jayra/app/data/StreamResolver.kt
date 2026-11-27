package com.jayra.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * StreamResolver — mengubah URL embed (ok.ru, dll) menjadi URL media LANGSUNG
 * (.m3u8 / .mp4) yang bisa diputar ExoPlayer/Media3 TANPA WebView.
 *
 * PENTING: halaman embed ok.ru men-encode kutip sebagai entity HTML
 * (`&quot;`) dan ampersand sebagai `\u0026`. Karena itu teks di-unescape
 * DULU sebelum di-regex, kalau tidak field `hlsManifestUrl` tidak akan
 * pernah cocok (inilah penyebab layar hitam sebelumnya).
 */
object StreamResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    /** Resolve embed URL -> direct media URL (m3u8/mp4), atau null. */
    fun resolve(embedUrl: String): String? {
        return when {
            embedUrl.contains("ok.ru") -> resolveOkru(embedUrl)
            else -> resolveGeneric(embedUrl)
        }
    }

    private fun resolveOkru(url: String): String? {
        val raw = httpGet(url) ?: return null
        // Unescape entity HTML dulu: &quot; -> " , \u0026 -> & , \/ -> /
        val html = raw
            .replace("&quot;", "\"")
            .replace("\\u0026", "&")
            .replace("\\/", "/")

        // 1) HLS manifest (paling ideal untuk ExoPlayer)
        val hls = extractField(html, "hlsManifestUrl")
        if (!hls.isNullOrBlank() && hls.contains(".m3u8")) {
            return hls
        }

        // 2) fallback: videos[].url (mp4 progressive) — utamakan full/hd
        val vids = Pattern.compile("\"name\":\"(full|hd|sd|low|lowest|mobile)\",\"url\":\"([^\"]+)\"")
            .matcher(html)
        var fallback: String? = null
        while (vids.find()) {
            val q = vids.group(1)
            val u = vids.group(2)
            if (fallback == null) fallback = u
            if (q == "full" || q == "hd") return u
        }
        return fallback
    }

    private fun resolveGeneric(url: String): String? {
        val raw = httpGet(url) ?: return null
        val html = raw.replace("&quot;", "\"").replace("\\u0026", "&").replace("\\/", "/")
        val m = Pattern.compile("https?://[^\"'\\\\ ]+\\.(?:m3u8|mp4)[^\"'\\\\ ]*")
            .matcher(html)
        return if (m.find()) m.group(0) else null
    }

    private fun extractField(html: String, field: String): String? {
        val p = Pattern.compile("\"$field\":\"([^\"]+)\"")
        val m = p.matcher(html)
        if (!m.find()) return null
        return m.group(1)
            .replace("\\u0026", "&")
            .replace("&quot;", "\"")
            .replace("\\/", "/")
    }

    private fun httpGet(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Referer", "https://ok.ru/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }
}