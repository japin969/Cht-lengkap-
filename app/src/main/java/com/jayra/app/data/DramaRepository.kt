package com.jayra.app.data

import android.content.Context
import android.content.SharedPreferences
import retrofit2.Response
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository Drama dengan cache 30 menit per endpoint (SharedPreferences).
 *
 * Alur (WAJIB):
 *  1. Cek cache. Kalau fresh (<30 menit) -> pakai cache, 0 request ke API.
 *  2. Kalau expired -> tembak API 1x (lewat rate limiter), simpan lagi.
 *  3. Kalau API error / 429 / 403 -> fallback ke cache lama (stale) +
 *     tandai offline, JANGAN blank.
 *  4. Cache kosong + API gagal -> kembalikan kosong (UI tampilkan empty state).
 *
 * Cache disimpan sebagai JSON (serialisasi manual, tanpa dependency baru).
 */
class DramaRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("drama_cache", Context.MODE_PRIVATE)

    private val api = DramaApi.create()

    companion object {
        private const val FRESH_MS = 30 * 60 * 1000L // 30 menit

        @Volatile
        private var instance: DramaRepository? = null

        fun get(context: Context): DramaRepository = instance ?: synchronized(this) {
            instance ?: DramaRepository(context).also { instance = it }
        }
    }

    // ── Home: gabungan semua section, di-cache per section ──────────────

    suspend fun home(): HomeResult {
        val sections = LinkedHashMap<String, List<DramaItem>>()
        var offline = false
        var anyFresh = false

        // Tiap section: cache dulu, kalau expired tembak API.
        val trending = section("trending") { api.trending() }
        sections["Trending"] = trending.items
        offline = offline || trending.offline
        anyFresh = anyFresh || trending.fresh

        val forYou = section("foryou") { api.forYou(1) }
        sections["For You"] = forYou.items
        offline = offline || forYou.offline
        anyFresh = anyFresh || forYou.fresh

        val hot = section("hotrank") { api.hotRank() }
        sections["Hot Rank"] = hot.items
        offline = offline || hot.offline
        anyFresh = anyFresh || hot.fresh

        val rec = section("recommended") { api.recommended() }
        sections["Recommended"] = rec.items
        offline = offline || rec.offline
        anyFresh = anyFresh || rec.fresh

        return HomeResult(
            sections = sections,
            offline = offline && !anyFresh,
            empty = sections.values.all { it.isEmpty() }
        )
    }

    // ── Search ──────────────────────────────────────────────────────────

    suspend fun search(query: String): SearchResult {
        val q = query.trim()
        if (q.isEmpty()) return SearchResult(emptyList(), false, false)
        val cacheKey = "search:$q"
        val cached = readList(cacheKey)
        val fresh = isFresh(cacheKey)
        if (cached != null && fresh) {
            return SearchResult(cached, false, false)
        }
        // Tembak API (lewat rate limiter). Search kadang lambat -> timeout
        // ditangani repository; kalau gagal pakai cache lama.
        val resp = runCatching {
            ApiRateLimiter.throttle { api.search(q) }
        }.getOrNull()
        val items = resp?.body()?.allItems
        if (resp != null && resp.isSuccessful && !items.isNullOrEmpty()) {
            writeList(cacheKey, items)
            return SearchResult(items, false, false)
        }
        // Gagal / 429 / timeout -> fallback cache lama.
        if (cached != null) return SearchResult(cached, true, false)
        return SearchResult(emptyList(), true, cached == null)
    }

    // ── Detail ──────────────────────────────────────────────────────────

    suspend fun detail(id: String): DetailResult {
        if (id.isBlank()) return DetailResult(null, emptyList(), true, false)
        val cacheKey = "detail:$id"
        val cached = readDetail(cacheKey)
        if (cached != null && isFresh(cacheKey)) {
            return DetailResult(cached, cached.episodeList.orEmpty(), false, false)
        }
        val resp = runCatching {
            ApiRateLimiter.throttle { api.detail(id) }
        }.getOrNull()
        val data = resp?.body()?.data
        if (resp != null && resp.isSuccessful && data != null && data.title != null) {
            // Lengkapi episodes dari detail (kalau ada) -> simpan.
            writeDetail(cacheKey, data)
            return DetailResult(data, data.episodeList.orEmpty(), false, false)
        }
        if (cached != null) {
            return DetailResult(cached, cached.episodeList.orEmpty(), true, false)
        }
        return DetailResult(null, emptyList(), true, cached == null)
    }

    // ── Episode (resolve mp4) ────────────────────────────────────────────

    suspend fun episode(id: String, ep: Int): String? {
        val cacheKey = "ep:$id:$ep"
        val cached = prefs.getString(cacheKey, null)
        if (cached != null && isFresh(cacheKey)) return cached
        val resp = runCatching {
            ApiRateLimiter.throttle { api.episode(id, ep) }
        }.getOrNull()
        val url = resp?.body()?.bestUrl
        if (resp != null && resp.isSuccessful && !url.isNullOrBlank()) {
            prefs.edit().putString(cacheKey, url).putLong("$cacheKey.ts", now()).apply()
            return url
        }
        // fallback cache lama (meski expired)
        return cached
    }

    fun clearCache() = prefs.edit().clear().apply()

    // ── Internal helpers ─────────────────────────────────────────────────

    private data class Section(val items: List<DramaItem>, val offline: Boolean, val fresh: Boolean)

    private suspend fun section(
        key: String,
        fetch: suspend () -> Response<DramaListResponse>
    ): Section {
        val cached = readList(key)
        if (cached != null && isFresh(key)) return Section(cached, false, true)
        val resp = runCatching { ApiRateLimiter.throttle { fetch() } }.getOrNull()
        val items = resp?.body()?.allItems
        if (resp != null && resp.isSuccessful && !items.isNullOrEmpty()) {
            writeList(key, items)
            return Section(items, false, true)
        }
        return Section(cached ?: emptyList(), cached == null, false)
    }

    private fun now() = System.currentTimeMillis()

    private fun isFresh(key: String): Boolean {
        val ts = prefs.getLong("$key.ts", 0L)
        return ts > 0 && now() - ts < FRESH_MS
    }

    // Serialisasi manual (tanpa Moshi/Gson -> hemat & aman).

    private fun writeList(key: String, items: List<DramaItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(itemToJson(it)) }
        prefs.edit().putString(key, arr.toString()).putLong("$key.ts", now()).apply()
    }

    private fun readList(key: String): List<DramaItem>? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { jsonToItem(arr.getJSONObject(it)) }
        }.getOrNull()
    }

    private fun writeDetail(key: String, item: DramaItem) {
        prefs.edit().putString(key, itemToJson(item).toString()).putLong("$key.ts", now()).apply()
    }

    private fun readDetail(key: String): DramaItem? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { jsonToItem(JSONObject(raw)) }.getOrNull()
    }

    private fun itemToJson(it: DramaItem): JSONObject = JSONObject().apply {
        put("id", it.id ?: JSONObject.NULL)
        put("dramaId", it.dramaId ?: JSONObject.NULL)
        put("title", it.title ?: JSONObject.NULL)
        put("cover", it.cover ?: JSONObject.NULL)
        put("posterImg", it.posterImg ?: JSONObject.NULL)
        put("description", it.description ?: JSONObject.NULL)
        put("synopsis", it.synopsis ?: JSONObject.NULL)
        put("totalEpisodes", it.totalEpisodes ?: JSONObject.NULL)
        put("isCompleted", it.isCompleted ?: JSONObject.NULL)
        put("categoryNames", JSONArray(it.categoryNames.orEmpty()))
        put("publishedAt", it.publishedAt ?: JSONObject.NULL)
        // episodes (ringkas: simpan videoUrl per episode)
        val eps = JSONArray()
        it.episodeList.orEmpty().forEach { ep ->
            eps.put(JSONObject().apply {
                put("episodeNumber", ep.episodeNumber ?: JSONObject.NULL)
                put("number", ep.number ?: JSONObject.NULL)
                put("episodeTitle", ep.episodeTitle ?: JSONObject.NULL)
                put("title", ep.title ?: JSONObject.NULL)
                put("locked", ep.locked ?: JSONObject.NULL)
                put("videoUrl", ep.videoUrl ?: JSONObject.NULL)
            })
        }
        put("episodeList", eps)
    }

    private fun jsonToItem(o: JSONObject): DramaItem {
        val epsArr = o.optJSONArray("episodeList")
        val eps = if (epsArr == null) emptyList() else (0 until epsArr.length()).map { i ->
            val e = epsArr.getJSONObject(i)
            DramaEpisode(
                episodeNumber = if (e.isNull("episodeNumber")) null else e.getInt("episodeNumber"),
                number = if (e.isNull("number")) null else e.getInt("number"),
                episodeTitle = e.optString("episodeTitle").ifBlank { null },
                title = e.optString("title").ifBlank { null },
                locked = if (e.isNull("locked")) null else e.getBoolean("locked"),
                videoUrl = e.optString("videoUrl").ifBlank { null }
            )
        }
        return DramaItem(
            id = o.optString("id").ifBlank { null },
            dramaId = o.optString("dramaId").ifBlank { null },
            title = o.optString("title").ifBlank { null },
            cover = o.optString("cover").ifBlank { null },
            posterImg = o.optString("posterImg").ifBlank { null },
            description = o.optString("description").ifBlank { null },
            synopsis = o.optString("synopsis").ifBlank { null },
            totalEpisodes = if (o.isNull("totalEpisodes")) null else o.getInt("totalEpisodes"),
            isCompleted = o.optString("isCompleted").ifBlank { null },
            categoryNames = runCatching {
                val a = o.optJSONArray("categoryNames")
                if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }
            }.getOrDefault(emptyList()),
            publishedAt = o.optString("publishedAt").ifBlank { null },
            episodes = if (eps.isEmpty()) null else DramaEpisodesField(list = eps)
        )
    }

    // ── Hasil ────────────────────────────────────────────────────────────

    data class HomeResult(
        val sections: Map<String, List<DramaItem>>,
        val offline: Boolean,
        val empty: Boolean
    )

    data class SearchResult(
        val items: List<DramaItem>,
        val offline: Boolean,
        val empty: Boolean
    )

    data class DetailResult(
        val detail: DramaItem?,
        val episodes: List<DramaEpisode>,
        val offline: Boolean,
        val empty: Boolean
    )
}