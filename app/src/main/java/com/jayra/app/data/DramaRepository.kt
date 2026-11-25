package com.jayra.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.jayra.app.JayraApp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository Drama (Strem) — Sankavollerei donghua + donghub.
 * Cache-first 30 menit (SharedPreferences). Paralel. Rate-limit aman.
 *
 * Alur: cek cache -> fresh? pakai. expired? tembak API 1x + simpan.
 * error/429 -> fallback cache lama + flag offline.
 */
class DramaRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("drama_cache", Context.MODE_PRIVATE)
    private val api = DramaApi.create()

    companion object {
        private const val FRESH_MS = 30 * 60 * 1000L
        private val MP4_REGEX = Regex("https?://[^\"'\\s]+\\.mp4[^\"'\\s]*")
        private val M3U8_REGEX = Regex("https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*")
        @Volatile private var instance: DramaRepository? = null
        fun get(context: Context): DramaRepository = instance ?: synchronized(this) {
            instance ?: DramaRepository(context).also { instance = it }
        }
    }

    suspend fun home(): HomeResult = coroutineScope {
        // donghua (ongoing/completed/latest) + donghub (popular/latest) PARALEL.
        val ongoingD = async { safeList("donghua_ongoing") { api.donghuaOngoing(1) } }
        val completedD = async { safeList("donghua_completed") { api.donghuaCompleted(1) } }
        val latestD = async { safeList("donghua_latest") { api.donghuaLatest(1) } }
        val donghubD = async { safeList("donghub_home") { api.donghubHome() } }
        val donghubPopD = async { safeList("donghub_popular") { api.donghubPopular() } }
        val nekopoiD = async { safeList("nekopoi_home") { api.nekopoiHome() } }

        val sections = linkedMapOf<String, List<DramaItem>>()
        var offline = false
        var empty = true

        fun put(name: String, items: List<DramaItem>) {
            if (items.isNotEmpty()) { sections[name] = items; empty = false }
        }
        // gabung ongoing
        val ongoing = (ongoingD.await() + latestD.await()).distinctBy { it.stableId ?: it.title }
        val completed = completedD.await()
        val donghub = (donghubD.await() + donghubPopD.await()).distinctBy { it.stableId ?: it.title }
        val nekopoi = nekopoiD.await()
        put("Ongoing", ongoing)
        put("Populer", donghub)
        put("Completed", completed)
        put("Latest", latestD.await())
        put("Nekopoi", nekopoi)

        HomeResult(sections, offline, empty)
    }

    suspend fun search(query: String): SearchResult {
        // donghua search (path keyword) + donghub search tidak ada -> pakai donghua.
        val q = query.trim()
        if (q.isEmpty()) return SearchResult(emptyList(), false, false)
        val cacheKey = "search:$q"
        val cached = readList(cacheKey)
        if (cached != null && isFresh(cacheKey)) return SearchResult(cached, false, false)
        val resp = runCatching { api.donghuaSearch(q, 1) }.getOrNull()
        val items = resp?.body()?.allItems
        if (resp != null && resp.isSuccessful && !items.isNullOrEmpty()) {
            writeList(cacheKey, items)
            return SearchResult(items, false, false)
        }
        if (cached != null) return SearchResult(cached, true, false)
        return SearchResult(emptyList(), true, cached == null)
    }

    suspend fun detail(slug: String): DetailResult {
        if (slug.isBlank()) return DetailResult(null, emptyList(), true, false)
        val cacheKey = "detail:$slug"
        val cached = readDetail(cacheKey)
        if (cached != null && isFresh(cacheKey)) {
            return DetailResult(cached, cached.resolvedEpisodes, false, false)
        }
        // coba donghua dulu, lalu donghub.
        var resp = runCatching { api.donghuaDetail(slug) }.getOrNull()
        var detail = resp?.body()
        if (detail == null || detail.resolvedTitle == null) {
            resp = runCatching { api.donghubDetail(slug) }.getOrNull()
            detail = resp?.body()
        }
        if (detail == null || detail.resolvedTitle == null) {
            resp = runCatching { api.nekopoiDetail(slug) }.getOrNull()
            detail = resp?.body()
        }
        if (detail != null && (detail.resolvedTitle != null || detail.resolvedEpisodes.isNotEmpty())) {
            writeDetail(cacheKey, detail)
            return DetailResult(detail, detail.resolvedEpisodes, false, false)
        }
        if (cached != null) return DetailResult(cached, cached.resolvedEpisodes, true, false)
        return DetailResult(null, emptyList(), true, cached == null)
    }

    suspend fun stream(slug: String): String? {
        // Stream URL (ok.ru/googlevideo) berumur pendek (token + IP). JANGAN
        // di-cache 30 menit — itu bikin play berikutnya memakai token kedaluwarsa
        // dan muncul "Source error". Selalu resolve fresh.
        val candidates = buildList {
            runCatching { api.donghuaEpisode(slug) }.getOrNull()?.body()?.serverCandidates
                ?.let { addAll(it) }
            runCatching { api.donghubEpisode(slug) }.getOrNull()?.body()?.serverCandidates
                ?.let { addAll(it) }
            runCatching { api.nekopoiEpisode(slug) }.getOrNull()?.body()?.serverCandidates
                ?.let { addAll(it) }
        }.distinct()
        Log.d("JayraStream", "Kandidat server ($slug): ${candidates.size}")

        // Coba tiap kandidat. Kalau sudah media langsung pakai; kalau embed,
        // resolve ke mp4/m3u8. JANGAN pakai URL embed sebagai fallback — ExoPlayer
        // tidak bisa memutar halaman HTML (itu penyebab layar hitam).
        for ((index, candidate) in candidates.withIndex()) {
            val direct = if (isDirectMedia(candidate)) candidate else resolveEmbedToMp4(candidate)
            if (!direct.isNullOrBlank()) {
                Log.d("JayraStream", "Server #${index + 1} OK: $direct")
                return direct
            }
            Log.w("JayraStream", "Server #${index + 1} gagal resolve: $candidate")
        }
        Log.w("JayraStream", "Tidak ada sumber video yang bisa di-resolve (slug=$slug)")
        return null
    }

    /** URL yang sudah berupa media asli (bukan halaman embed HTML). */
    private fun isDirectMedia(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") ||
            lower.contains(".mp4") ||
            lower.contains("videoplayback") ||
            lower.contains(".mkv")
    }

    /** Fetch halaman embed, cari mp4/m3u8 di HTML/JSON. */
    private suspend fun resolveEmbedToMp4(embedUrl: String): String? = runCatching {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val req = okhttp3.Request.Builder().url(embedUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                )
                .header("Referer", embedUrl)
                .header("Accept", "*/*")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w("JayraStream", "Embed ${resp.code} untuk $embedUrl")
                    return@use null
                }
                val raw = resp.body?.string() ?: return@use null
                // Decode entity HTML (&quot; &amp; \u0026 \/ ) supaya regex URL valid.
                val html = decodeEntities(raw)
                // ok.ru / rumble / player lain: HLS manifest (.m3u8) didahulukan.
                M3U8_REGEX.find(html)?.value
                    ?: MP4_REGEX.find(html)?.value
            }
        }
    }.getOrNull()

    private fun decodeEntities(s: String): String = s
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .replace("\\u0026", "&")
        .replace("\\/", "/")
        .replace("\\u002F", "/")

    // ── internal ────────────────────────────────────────────────────────

    private suspend fun safeList(
        key: String,
        fetch: suspend () -> retrofit2.Response<DramaListResponse>
    ): List<DramaItem> {
        val cached = readList(key)
        if (cached != null && isFresh(key)) return cached
        val resp = runCatching { fetch() }.getOrNull()
        val items = resp?.body()?.allItems
        if (resp != null && resp.isSuccessful && !items.isNullOrEmpty()) {
            writeList(key, items)
            return items
        }
        return cached ?: emptyList()
    }

    private fun now() = System.currentTimeMillis()
    private fun isFresh(key: String): Boolean {
        val ts = prefs.getLong("$key.ts", 0L)
        return ts > 0 && now() - ts < FRESH_MS
    }

    private fun writeList(key: String, items: List<DramaItem>) {
        val a = JSONArray()
        items.forEach { a.put(itemToJson(it)) }
        prefs.edit().putString(key, a.toString()).putLong("$key.ts", now()).apply()
    }
    private fun readList(key: String): List<DramaItem>? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { jsonToItem(a.getJSONObject(it)) }
        }.getOrNull()
    }
    private fun writeDetail(key: String, item: DramaDetailResponse) {
        // simpan sebagai DramaItem (episodes di-flatten ke json)
        val o = JSONObject().apply {
            put("title", item.resolvedTitle ?: JSONObject.NULL)
            put("poster", item.resolvedCover ?: JSONObject.NULL)
            put("synopsis", item.resolvedSynopsis ?: JSONObject.NULL)
            put("rating", item.resolvedRating ?: JSONObject.NULL)
            val eps = JSONArray()
            item.resolvedEpisodes.forEach { ep ->
                eps.put(JSONObject().apply {
                    put("title", ep.title ?: JSONObject.NULL)
                    put("slug", ep.slug ?: JSONObject.NULL)
                    put("href", ep.href ?: JSONObject.NULL)
                })
            }
            put("episodes", eps)
        }
        prefs.edit().putString(key, o.toString()).putLong("$key.ts", now()).apply()
    }
    private fun readDetail(key: String): DramaDetailResponse? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val eps = o.optJSONArray("episodes")
            val epList = if (eps == null) emptyList() else (0 until eps.length()).map { i ->
                val e = eps.getJSONObject(i)
                DramaEpisode(
                    title = e.optString("title").ifBlank { null },
                    slug = e.optString("slug").ifBlank { null },
                    href = e.optString("href").ifBlank { null }
                )
            }
            DramaDetailResponse(
                title = o.optString("title").ifBlank { null },
                poster = o.optString("poster").ifBlank { null },
                synopsis = o.optString("synopsis").ifBlank { null },
                rating = o.optString("rating").ifBlank { null },
                episodes = epList
            )
        }.getOrNull()
    }

    private fun itemToJson(it: DramaItem): JSONObject = JSONObject().apply {
        put("title", it.title ?: JSONObject.NULL)
        put("slug", it.slug ?: JSONObject.NULL)
        put("poster", it.poster ?: JSONObject.NULL)
        put("status", it.status ?: JSONObject.NULL)
        put("type", it.type ?: JSONObject.NULL)
        put("href", it.href ?: JSONObject.NULL)
    }
    private fun jsonToItem(o: JSONObject): DramaItem = DramaItem(
        title = o.optString("title").ifBlank { null },
        slug = o.optString("slug").ifBlank { null },
        poster = o.optString("poster").ifBlank { null },
        status = o.optString("status").ifBlank { null },
        type = o.optString("type").ifBlank { null },
        href = o.optString("href").ifBlank { null }
    )

    data class HomeResult(
        val sections: Map<String, List<DramaItem>>,
        val offline: Boolean,
        val empty: Boolean
    )
    data class SearchResult(val items: List<DramaItem>, val offline: Boolean, val empty: Boolean)
    data class DetailResult(
        val detail: DramaDetailResponse?,
        val episodes: List<DramaEpisode>,
        val offline: Boolean,
        val empty: Boolean
    )
}