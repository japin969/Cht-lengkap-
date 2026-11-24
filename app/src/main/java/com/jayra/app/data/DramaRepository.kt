package com.jayra.app.data

import android.content.Context
import android.content.SharedPreferences
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
        val cacheKey = "stream:$slug"
        val cached = prefs.getString(cacheKey, null)
        if (cached != null && isFresh(cacheKey)) return cached
        // donghua dulu, lalu donghub.
        var resp = runCatching { api.donghuaEpisode(slug) }.getOrNull()
        var url = resp?.body()?.bestStreamUrl
        if (url.isNullOrBlank()) {
            resp = runCatching { api.donghubEpisode(slug) }.getOrNull()
            url = resp?.body()?.bestStreamUrl
        }
        if (url.isNullOrBlank()) {
            resp = runCatching { api.nekopoiEpisode(slug) }.getOrNull()
            url = resp?.body()?.bestStreamUrl
        }
        if (!url.isNullOrBlank()) {
            prefs.edit().putString(cacheKey, url).putLong("$cacheKey.ts", now()).apply()
        }
        return url
    }

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