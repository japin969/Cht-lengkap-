package com.jayra.app.data

import android.content.Context
import com.jayra.app.JayraApp
import org.json.JSONArray
import org.json.JSONObject

/**
 * Penyimpanan ringan (SharedPreferences) untuk Bookmark & History tiap fitur
 * (comic/anime/strem). Terpisah per fitur, tanpa perubahan skema Room.
 *
 * Pola aplikasi terkenal: bookmark = koleksi tersimpan + progress baca/tonton,
 * history = riwayat terakhir dibuka dengan progress.
 */
data class MediaEntry(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val cover: String = "",
    val progress: Float = 0f,       // 0..1
    val progressLabel: String = "", // "Ch. 12" / "Ep. 5"
    val addedAt: Long = System.currentTimeMillis()
)

object MediaStore {

    private fun prefs(context: Context) =
        context.getSharedPreferences("media_store", Context.MODE_PRIVATE)

    private fun key(feature: String, kind: String) = "${feature}_$kind"

    fun bookmarks(context: Context, feature: String): List<MediaEntry> =
        read(key(feature, "bookmarks"))

    fun history(context: Context, feature: String): List<MediaEntry> =
        read(key(feature, "history"))

    fun isBookmarked(context: Context, feature: String, id: String): Boolean =
        bookmarks(context, feature).any { it.id == id }

    fun toggleBookmark(context: Context, feature: String, entry: MediaEntry) {
        val list = bookmarks(context, feature).toMutableList()
        val idx = list.indexOfFirst { it.id == entry.id }
        if (idx >= 0) list.removeAt(idx) else list.add(0, entry)
        save(key(feature, "bookmarks"), list)
    }

    fun pushHistory(context: Context, feature: String, entry: MediaEntry) {
        val list = history(context, feature).toMutableList()
        list.removeAll { it.id == entry.id }
        list.add(0, entry)
        if (list.size > 100) list.subList(100, list.size).clear()
        save(key(feature, "history"), list)
    }

    fun removeHistory(context: Context, feature: String, id: String) {
        val list = history(context, feature).toMutableList()
        list.removeAll { it.id == id }
        save(key(feature, "history"), list)
    }

    fun clearHistory(context: Context, feature: String) {
        save(key(feature, "history"), emptyList())
    }

    fun updateProgress(context: Context, feature: String, id: String, progress: Float, label: String) {
        listOf("history", "bookmarks").forEach { kind ->
            val k = key(feature, kind)
            val list = read(k).toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) {
                list[idx] = list[idx].copy(progress = progress, progressLabel = label)
                save(k, list)
            }
        }
    }

    private fun read(k: String): List<MediaEntry> {
        val raw = prefsApp().getString(k, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MediaEntry(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    subtitle = o.optString("subtitle"),
                    cover = o.optString("cover"),
                    progress = o.optDouble("progress", 0.0).toFloat(),
                    progressLabel = o.optString("progressLabel"),
                    addedAt = o.optLong("addedAt")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save(k: String, list: List<MediaEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("subtitle", e.subtitle)
                put("cover", e.cover)
                put("progress", e.progress.toDouble())
                put("progressLabel", e.progressLabel)
                put("addedAt", e.addedAt)
            })
        }
        prefsApp().edit().putString(k, arr.toString()).apply()
    }

    private fun prefsApp() =
        JayraApp.appContextOrNull()?.let { prefs(it) }
            ?: throw IllegalStateException("JayraApp belum diinisialisasi")
}