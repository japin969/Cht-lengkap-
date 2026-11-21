package com.jayra.app.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Penyimpanan ringan untuk Bookmark & History Komik/Anime.
 *
 * Sengaja memakai SharedPreferences + Moshi (bukan Room) supaya tidak mengubah
 * schema database yang sudah berjalan. Data disimpan sebagai JSON array.
 *
 * Semua operasi aman: kegagalan baca/tulis tidak pernah dilempar ke UI.
 */
object MediaLibraryStore {

    private const val PREFS = "jayra_media_library"
    private const val KEY_BOOKMARK = "bookmark_v1"
    private const val KEY_HISTORY = "history_v1"

    /** Entri library (bookmark / history). */
    data class Entry(
        val kind: String,          // "komik" | "anime"
        val id: String,            // slug / id / url
        val title: String = "",
        val image: String? = null,
        val subtitle: String? = null,
        val extra: String? = null, // mis. "Chapter 12" / "Episode 5"
        val savedAt: Long = System.currentTimeMillis()
    )

    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    private val listType by lazy {
        Types.newParameterizedType(List::class.java, Entry::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private val listAdapter by lazy {
        moshi.adapter<List<Entry>>(listType)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun read(context: Context, key: String): MutableList<Entry> {
        return runCatching {
            val raw = prefs(context).getString(key, null) ?: return mutableListOf()
            listAdapter.fromJson(raw) ?: mutableListOf()
        }.getOrElse { mutableListOf() }.toMutableList()
    }

    private fun write(context: Context, key: String, items: List<Entry>) {
        runCatching {
            prefs(context).edit().putString(key, listAdapter.toJson(items)).apply()
        }
    }

    // ── Bookmark ───────────────────────────────────────────────

    fun bookmarks(context: Context): List<Entry> = read(context, KEY_BOOKMARK)

    fun isBookmarked(context: Context, kind: String, id: String): Boolean =
        read(context, KEY_BOOKMARK).any { it.kind == kind && it.id == id }

    /** Toggle bookmark. Return true jika kini ter-bookmark. */
    fun toggleBookmark(context: Context, entry: Entry): Boolean {
        val items = read(context, KEY_BOOKMARK)
        val existing = items.indexOfFirst { it.kind == entry.kind && it.id == entry.id }
        val nowSaved = if (existing >= 0) {
            items.removeAt(existing)
            false
        } else {
            items.add(0, entry.copy(savedAt = System.currentTimeMillis()))
            true
        }
        write(context, KEY_BOOKMARK, items)
        return nowSaved
    }

    fun removeBookmark(context: Context, kind: String, id: String) {
        val items = read(context, KEY_BOOKMARK)
        items.removeAll { it.kind == kind && it.id == id }
        write(context, KEY_BOOKMARK, items)
    }

    // ── History ────────────────────────────────────────────────

    fun history(context: Context, kind: String? = null): List<Entry> {
        val items = read(context, KEY_HISTORY)
        return if (kind == null) items else items.filter { it.kind == kind }
    }

    /** Catat riwayat baca / tonton. Entri lama digantung di posisi teratas. */
    fun recordHistory(context: Context, entry: Entry) {
        val items = read(context, KEY_HISTORY)
        items.removeAll { it.kind == entry.kind && it.id == entry.id }
        items.add(0, entry.copy(savedAt = System.currentTimeMillis()))
        write(context, KEY_HISTORY, items.take(MAX_HISTORY))
    }

    fun removeHistory(context: Context, kind: String, id: String) {
        val items = read(context, KEY_HISTORY)
        items.removeAll { it.kind == kind && it.id == id }
        write(context, KEY_HISTORY, items)
    }

    fun clearHistory(context: Context) {
        write(context, KEY_HISTORY, emptyList())
    }

    fun clearBookmarks(context: Context) {
        write(context, KEY_BOOKMARK, emptyList())
    }

    private const val MAX_HISTORY = 200
}