package com.jayra.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * DramaRepository — satu-satunya pintu ke API sankavollerei (Donghua + Donghub).
 * Parsing manual karena struktur JSON kedua sumber berbeda.
 */
class DramaRepository(private val api: SankavollereiApi = SankavollereiApi.create()) {

    data class HomeResult(
        val featured: List<DramaItem>,   // banner/hero (donghub slider + donghua first)
        val donghua: List<DramaItem>,
        val donghub: List<DramaItem>
    )

    /**
     * Home PARALEL: donghua & donghub di-call bersamaan (async). Kalau satu gagal,
     * yang lain tetap jalan.
     */
    suspend fun loadHome(): HomeResult = coroutineScope {
        val dhDeferred = async(Dispatchers.IO) { runCatching { loadDonghuaHome() } }
        val dhubDeferred = async(Dispatchers.IO) { runCatching { loadDonghubHome() } }

        val dh = dhDeferred.await().getOrNull().orEmpty()
        val dhub = dhubDeferred.await().getOrNull().orEmpty()

        val featured = (dhub.take(1) + dh.take(1)).filterNotNull()
        HomeResult(featured = featured, donghua = dh, donghub = dhub)
    }

    private suspend fun loadDonghuaHome(): List<DramaItem> {
        val resp = api.donghuaHome()
        val body = resp.body() ?: return emptyList()
        val out = mutableListOf<DramaItem>()
        // latest_release[] -> poster/title/slug
        @Suppress("UNCHECKED_CAST")
        (body["latest_release"] as? List<Map<String, Any?>>)?.forEach { m ->
            out.add(m.toDramaItem("donghua"))
        }
        return out
    }

    private suspend fun loadDonghubHome(): List<DramaItem> {
        val resp = api.donghubHome()
        val body = resp.body() ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as? Map<String, Any?> ?: return emptyList()
        val out = mutableListOf<DramaItem>()
        // slider[] (hero) + latest[]
        @Suppress("UNCHECKED_CAST")
        (data["slider"] as? List<Map<String, Any?>>)?.forEach { m ->
            out.add(m.toDramaItem("donghub"))
        }
        @Suppress("UNCHECKED_CAST")
        (data["latest"] as? List<Map<String, Any?>>)?.forEach { m ->
            out.add(m.toDramaItem("donghub"))
        }
        return out
    }

    /** Detail + daftar episode, otomatis deteksi sumber dari slug/href. */
    suspend fun loadDetail(slug: String, source: String): DramaDetail? = withContext(Dispatchers.IO) {
        when (source) {
            "donghua" -> loadDonghuaDetail(slug)
            else -> loadDonghubDetail(slug)
        }
    }

    private suspend fun loadDonghuaDetail(slug: String): DramaDetail? {
        val body = api.donghuaDetail(slug).body() ?: return null
        val item = DramaItem(
            id = slug,
            title = str(body["title"]),
            poster = str(body["poster"]),
            cover = str(body["poster"]),
            intro = str(body["synopsis"]),
            status = str(body["status"]),
            type = str(body["type"]),
            slug = slug,
            source = "donghua",
            chapterCount = intOrNull(body["episodes_count"])
        )
        val eps = mutableListOf<DramaEpisode>()
        @Suppress("UNCHECKED_CAST")
        (body["episodes_list"] as? List<Map<String, Any?>>)?.forEach { m ->
            eps.add(
                DramaEpisode(
                    index = intOrNull(m["episode_number"]),
                    name = str(m["episode"]) ?: str(m["episode_number"]),
                    unlock = true,
                    slug = str(m["slug"]),
                    href = str(m["href"]),
                    episodeNumber = str(m["episode_number"])
                )
            )
        }
        return DramaDetail(item, eps)
    }

    private suspend fun loadDonghubDetail(slug: String): DramaDetail? {
        val body = api.donghubDetail(slug).body() ?: return null
        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as? Map<String, Any?> ?: return null
        val info = data["info"] as? Map<String, Any?>
        val item = DramaItem(
            id = slug,
            title = str(data["title"]),
            poster = str(data["poster"]),
            cover = str(data["poster"]),
            intro = str(data["synopsis"]),
            status = info?.let { str(it["status"]) },
            type = info?.let { str(it["type"]) },
            slug = slug,
            source = "donghub",
            chapterCount = intOrNull(info?.get("episodes"))
        )
        val eps = mutableListOf<DramaEpisode>()
        @Suppress("UNCHECKED_CAST")
        (data["episodes"] as? List<Map<String, Any?>>)?.forEach { m ->
            eps.add(
                DramaEpisode(
                    index = intOrNull(m["episode"]),
                    name = str(m["title"]) ?: str(m["episode"]),
                    unlock = true,
                    slug = str(m["slug"]),
                    href = str(m["url"]),
                    episodeNumber = str(m["episode"])
                )
            )
        }
        return DramaDetail(item, eps)
    }

    /**
     * Ambil daftar server embed untuk episode, lalu resolve ke URL media langsung.
     * Mengembalikan list DramaStream (name + url m3u8/mp4) yang SIAP diputar.
     */
    suspend fun loadStreams(slug: String, source: String): List<DramaStream> =
        withContext(Dispatchers.IO) {
            val embeds: List<Pair<String, String>> = when (source) {
                "donghua" -> donghuaEmbeds(slug)
                else -> donghubEmbeds(slug)
            }
            embeds.mapNotNull { (name, url) ->
                val direct = StreamResolver.resolve(url) ?: return@mapNotNull null
                DramaStream(name = name, url = direct)
            }
        }

    private suspend fun donghuaEmbeds(slug: String): List<Pair<String, String>> {
        val body = api.donghuaEpisode(slug).body() ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val streaming = body["streaming"] as? Map<String, Any?> ?: return emptyList()
        val out = mutableListOf<Pair<String, String>>()
        (streaming["main_url"] as? Map<String, Any?>)?.let { m ->
            val u = str(m["url"]); val n = str(m["name"]) ?: "Main"
            if (!u.isNullOrBlank()) out.add(n to u)
        }
        @Suppress("UNCHECKED_CAST")
        (streaming["servers"] as? List<Map<String, Any?>>)?.forEach { m ->
            val u = str(m["url"]); val n = str(m["name"]) ?: "Server"
            if (!u.isNullOrBlank()) out.add(n to u)
        }
        return out
    }

    private suspend fun donghubEmbeds(slug: String): List<Pair<String, String>> {
        val body = api.donghubEpisode(slug).body() ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as? Map<String, Any?> ?: return emptyList()
        val out = mutableListOf<Pair<String, String>>()
        @Suppress("UNCHECKED_CAST")
        (data["streams"] as? List<Map<String, Any?>>)?.forEach { m ->
            val u = str(m["url"]); val n = str(m["server"]) ?: "Server"
            if (!u.isNullOrBlank()) out.add(n to u)
        }
        return out
    }

    // ---- helpers ----
    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toDramaItem(source: String): DramaItem = DramaItem(
        id = str(this["slug"]),
        title = str(this["title"]),
        poster = str(this["poster"]),
        cover = str(this["poster"]),
        intro = str(this["synopsis"]),
        episode = str(this["episode"]),
        currentEpisode = str(this["current_episode"]),
        status = str(this["status"]),
        type = str(this["type"]),
        slug = str(this["slug"]),
        url = str(this["href"]) ?: str(this["url"]),
        source = source
    )

    private fun str(v: Any?): String? = when (v) {
        null -> null
        is String -> v.ifBlank { null }
        else -> v.toString()
    }

    private fun intOrNull(v: Any?): Int? = when (v) {
        is Int -> v
        is Number -> v.toInt()
        is String -> v.filter { it.isDigit() }.toIntOrNull()
        else -> null
    }
}