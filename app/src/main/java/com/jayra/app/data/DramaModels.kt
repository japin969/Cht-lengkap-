package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Model Drama — mengikuti JSON asli dari https://api.anichin.bio/dramabox/
 * (DramaBox / Dracin aggregator). Field mengikuti response smaple:
 *   trending / foryou / hotrank / recommended -> { code, hasMore, items:[Item] }
 *   detail?id=... -> { code, data:Item (episodes terisi) }
 *   episode?id=..&ep=N -> { code, qualityList:[{label,url,isDefault}], videoUrl }
 *
 * CATATAN PENTING: API menolak (401) bila header X-API-Key dikirim. Jadi
 * request TIDAK memakai X-API-Key — cukup User-Agent + Accept.
 */

/** Bentuk list dari endpoint home (trending/foryou/hotrank/recommended/search). */
@JsonClass(generateAdapter = true)
data class DramaListResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "hasMore") val hasMore: Boolean? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "items") val items: List<DramaItem>? = null,
    @Json(name = "data") val data: List<DramaItem>? = null
) {
    /** Gabung dua bentuk payload (items / data). */
    val allItems: List<DramaItem> get() = items ?: data ?: emptyList()
}

/** Detail: { code, msg, data:Item } */
@JsonClass(generateAdapter = true)
data class DramaDetailResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "data") val data: DramaItem? = null
)

/**
 * Satu drama. `id` dipakai untuk request detail & episode. `cover` adalah URL
 * gambar langsung (bisa dipakai Coil tanpa API lagi).
 */
@JsonClass(generateAdapter = true)
data class DramaItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "dramaId") val dramaId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "posterImg") val posterImg: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "totalEpisodes") val totalEpisodes: Int? = null,
    /** Field `episodes` : Int (di list) ATAU array episode (di detail). */
    @Json(name = "episodes") val episodes: DramaEpisodesField? = null,
    @Json(name = "isCompleted") val isCompleted: String? = null,
    @Json(name = "categoryNames") val categoryNames: List<String>? = null,
    @Json(name = "publishedAt") val publishedAt: String? = null,
    @Json(name = "viewCount") val viewCount: Int? = null,
    @Json(name = "likeCount") val likeCount: Int? = null,
    @Json(name = "favoriteCount") val favoriteCount: Int? = null
) {
    val episodeList: List<DramaEpisode>? get() = episodes?.list
    /** ID yang dipakai untuk request detail/episode. */
    val stableId: String? get() = id ?: dramaId

    /** Cover terbaik (cover > posterImg). */
    val bestCover: String? get() = cover?.takeIf { it.isNotBlank() } ?: posterImg

    val bestSynopsis: String?
        get() = synopsis?.takeIf { it.isNotBlank() }
            ?: description?.takeIf { it.isNotBlank() }

    val epsCount: Int?
        get() = totalEpisodes ?: episodes?.count ?: episodes?.list?.size

    val completed: Boolean get() = isCompleted == "1" || isCompleted.equals("true", true)
}

/**
 * Satu episode di dalam detail. `videoUrl` bisa berupa link HLS
 * (priv-api.anichin.bio/api/dramabox/hls?...) yang perlu di-resolve ke mp4,
 * atau langsung mp4.
 */
@JsonClass(generateAdapter = true)
data class DramaEpisode(
    @Json(name = "episodeNumber") val episodeNumber: Int? = null,
    @Json(name = "number") val number: Int? = null,
    @Json(name = "episodeTitle") val episodeTitle: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "locked") val locked: Boolean? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null
) {
    val num: Int? get() = episodeNumber ?: number
    val label: String? get() = episodeTitle ?: title
}

/** Response /episode: qualityList berisi mp4 per kualitas. */
@JsonClass(generateAdapter = true)
data class DramaEpisodeResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "episodeNumber") val episodeNumber: Int? = null,
    @Json(name = "number") val number: Int? = null,
    @Json(name = "locked") val locked: Boolean? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null,
    @Json(name = "qualityList") val qualityList: List<DramaQuality>? = null
) {
    /** URL default (isDefault) atau kualitas tertinggi. */
    val bestUrl: String?
        get() = qualityList?.firstOrNull { it.isDefault == true }?.url
            ?: qualityList?.maxByOrNull { it.height }?.url
            ?: qualityList?.firstOrNull()?.url
            ?: videoUrl
}

@JsonClass(generateAdapter = true)
data class DramaQuality(
    @Json(name = "label") val label: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "isDefault") val isDefault: Boolean? = null
) {
    /** "720p" -> 720 (untuk memilih kualitas tertinggi). */
    val height: Int get() = label?.filter { it.isDigit() }?.toIntOrNull() ?: 0
}
/**
 * Field `episodes` yang BISA berupa Int (jumlah, di list) ATAU array
 * [DramaEpisode] (di detail). Adapter Moshi kustom [DramaEpisodesAdapter]
 * membaca keduanya tanpa gagal parse.
 */
data class DramaEpisodesField(
    val count: Int? = null,
    val list: List<DramaEpisode>? = null
)

/** Adapter Moshi untuk [DramaEpisodesField] (Int atau Array). */
class DramaEpisodesAdapter(private val episodeAdapter: com.squareup.moshi.JsonAdapter<DramaEpisode>) :
    com.squareup.moshi.JsonAdapter<DramaEpisodesField>() {

    override fun fromJson(reader: com.squareup.moshi.JsonReader): DramaEpisodesField? {
        return when (reader.peek()) {
            com.squareup.moshi.JsonReader.Token.NUMBER ->
                DramaEpisodesField(count = reader.nextInt())
            com.squareup.moshi.JsonReader.Token.BEGIN_ARRAY -> {
                val list = com.squareup.moshi.Types.newParameterizedType(
                    List::class.java, DramaEpisode::class.java
                )
                // Baca array manual memakai adapter episode.
                val out = ArrayList<DramaEpisode>()
                reader.beginArray()
                while (reader.hasNext()) {
                    episodeAdapter.fromJsonValue(reader)?.let { out.add(it) }
                }
                reader.endArray()
                DramaEpisodesField(list = out)
            }
            com.squareup.moshi.JsonReader.Token.NULL -> {
                reader.nextNull<Any?>(); null
            }
            else -> { reader.skipValue(); null }
        }
    }

    override fun toJson(writer: com.squareup.moshi.JsonWriter, value: DramaEpisodesField?) {
        if (value == null) { writer.nullValue(); return }
        if (value.list != null) {
            writer.beginArray()
            value.list.forEach { episodeAdapter.toJson(writer, it) }
            writer.endArray()
        } else {
            writer.value(value.count ?: 0)
        }
    }
}
