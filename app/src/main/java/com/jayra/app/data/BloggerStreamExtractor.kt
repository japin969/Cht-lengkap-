package com.jayra.app.data

import okhttp3.Request
import org.json.JSONObject

object BloggerStreamExtractor {

    fun isBloggerUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains("blogger.com/video.g", ignoreCase = true)
    }

    fun extract(bloggerUrl: String): List<AnimeStreamServer> {
        return runCatching {
            val request = Request.Builder()
                .url(bloggerUrl)
                .header("User-Agent", NetworkClient.BROWSER_UA)
                .header("Referer", "https://www.blogger.com/")
                .build()

            val html = NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                response.body?.string().orEmpty()
            }

            val configJson = extractVideoConfigJson(html) ?: return emptyList()
            val root = JSONObject(configJson)
            val streams = root.optJSONArray("streams") ?: return emptyList()

            val extracted = mutableListOf<Pair<Int, AnimeStreamServer>>()
            for (i in 0 until streams.length()) {
                val item = streams.optJSONObject(i) ?: continue
                val rawUrl = item.optString("play_url").trim()
                if (rawUrl.isBlank()) continue

                val cleanUrl = rawUrl
                    .replace("\\u003d", "=")
                    .replace("\\u0026", "&")
                    .replace("\\/", "/")

                val formatId = item.optInt("format_id", 0)
                val qualityLabel = when (formatId) {
                    37 -> "1080p FHD"
                    22 -> "720p HD"
                    18 -> "360p SD"
                    else -> if (formatId > 0) "${formatId}p" else "Auto"
                }
                val priority = when (formatId) {
                    22 -> 3
                    37 -> 2
                    18 -> 1
                    else -> 0
                }
                extracted.add(
                    priority to AnimeStreamServer(
                        name = "Jayra Stream",
                        quality = qualityLabel,
                        url = cleanUrl
                    )
                )
            }

            extracted
                .sortedByDescending { it.first }
                .map { it.second }
                .distinctBy { it.url }
        }.getOrDefault(emptyList())
    }

    private fun extractVideoConfigJson(html: String): String? {
        val markerIdx = html.indexOf("VIDEO_CONFIG")
        if (markerIdx == -1) return null
        val startBrace = html.indexOf('{', markerIdx)
        if (startBrace == -1) return null

        var depth = 0
        var inString = false
        var escaped = false

        for (i in startBrace until html.length) {
            val ch = html[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\' && inString) {
                escaped = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (ch == '{') depth++
                else if (ch == '}') {
                    depth--
                    if (depth == 0) {
                        return html.substring(startBrace, i + 1)
                    }
                }
            }
        }
        return null
    }
}
