package com.jayra.app.data

import android.content.Context
import android.content.SharedPreferences
import com.jayra.app.JayraApp
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cache Home Komik PERSISTEN (SharedPreferences) supaya buka app / balik ke
 * Home langsung tampil dari disk (0 request), lalu refresh di background.
 */
object KomikHomeStore {

    private const val FILE = "komik_home_cache"
    private const val KEY = "home"
    private const val FRESH_MS = 30 * 60 * 1000L

    data class Item(
        val title: String?,
        val link: String?,
        val slug: String?,
        val cover: String?,
        val type: String?,
        val latestChapter: String?,
        val rating: String?
    )

    private val prefs: SharedPreferences?
        get() = JayraApp.appContextOrNull()?.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): List<Item>? {
        val raw = prefs?.getString(KEY, null) ?: return null
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Item(
                    title = o.optString("title").ifBlank { null },
                    link = o.optString("link").ifBlank { null },
                    slug = o.optString("slug").ifBlank { null },
                    cover = o.optString("cover").ifBlank { null },
                    type = o.optString("type").ifBlank { null },
                    latestChapter = o.optString("latestChapter").ifBlank { null },
                    rating = o.optString("rating").ifBlank { null }
                )
            }
        }.getOrNull()
    }

    fun save(items: List<Item>) {
        val a = JSONArray()
        items.forEach {
            a.put(JSONObject().apply {
                put("title", it.title ?: JSONObject.NULL)
                put("link", it.link ?: JSONObject.NULL)
                put("slug", it.slug ?: JSONObject.NULL)
                put("cover", it.cover ?: JSONObject.NULL)
                put("type", it.type ?: JSONObject.NULL)
                put("latestChapter", it.latestChapter ?: JSONObject.NULL)
                put("rating", it.rating ?: JSONObject.NULL)
            })
        }
        prefs?.edit()?.putString(KEY, a.toString())?.putLong("ts", System.currentTimeMillis())?.apply()
    }

    fun isFresh(): Boolean {
        val ts = prefs?.getLong("ts", 0L) ?: 0L
        return ts > 0 && System.currentTimeMillis() - ts < FRESH_MS
    }
}