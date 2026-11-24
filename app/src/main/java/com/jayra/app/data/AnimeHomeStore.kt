package com.jayra.app.data

import android.content.Context
import android.content.SharedPreferences
import com.jayra.app.JayraApp
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cache Home Anime PERSISTEN (SharedPreferences) supaya buka app / balik ke
 * Home langsung tampil dari disk (0 request), lalu refresh di background.
 * Menyimpan ringan (judul/link/image per kategori) — cukup untuk render cepat.
 */
object AnimeHomeStore {

    private const val FILE = "anime_home_cache"
    private const val KEY = "home"
    private const val FRESH_MS = 30 * 60 * 1000L

    data class Snapshot(
        val ongoing: List<Item>,
        val popular: List<Item>,
        val recent: List<Item>,
        val completed: List<Item>
    )

    data class Item(
        val title: String?,
        val link: String?,
        val image: String?,
        val latestEpisode: String?,
        val type: String?,
        val rating: String?,
        val status: String?
    )

    private val prefs: SharedPreferences?
        get() = JayraApp.appContextOrNull()?.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): Snapshot? {
        val raw = prefs?.getString(KEY, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            Snapshot(
                ongoing = parse(o.optJSONArray("ongoing")),
                popular = parse(o.optJSONArray("popular")),
                recent = parse(o.optJSONArray("recent")),
                completed = parse(o.optJSONArray("completed"))
            )
        }.getOrNull()
    }

    fun save(s: Snapshot) {
        val o = JSONObject().apply {
            put("ongoing", toArr(s.ongoing))
            put("popular", toArr(s.popular))
            put("recent", toArr(s.recent))
            put("completed", toArr(s.completed))
        }
        prefs?.edit()?.putString(KEY, o.toString())?.putLong("ts", System.currentTimeMillis())?.apply()
    }

    fun isFresh(): Boolean {
        val ts = prefs?.getLong("ts", 0L) ?: 0L
        return ts > 0 && System.currentTimeMillis() - ts < FRESH_MS
    }

    private fun toArr(items: List<Item>): JSONArray {
        val a = JSONArray()
        items.forEach {
            a.put(JSONObject().apply {
                put("title", it.title ?: JSONObject.NULL)
                put("link", it.link ?: JSONObject.NULL)
                put("image", it.image ?: JSONObject.NULL)
                put("latestEpisode", it.latestEpisode ?: JSONObject.NULL)
                put("type", it.type ?: JSONObject.NULL)
                put("rating", it.rating ?: JSONObject.NULL)
                put("status", it.status ?: JSONObject.NULL)
            })
        }
        return a
    }

    private fun parse(a: JSONArray?): List<Item> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Item(
                title = o.optString("title").ifBlank { null },
                link = o.optString("link").ifBlank { null },
                image = o.optString("image").ifBlank { null },
                latestEpisode = o.optString("latestEpisode").ifBlank { null },
                type = o.optString("type").ifBlank { null },
                rating = o.optString("rating").ifBlank { null },
                status = o.optString("status").ifBlank { null }
            )
        }
    }
}