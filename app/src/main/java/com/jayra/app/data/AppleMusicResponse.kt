package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppleMusicResponse(
    @Json(name = "status") val status: Boolean? = true,
    @Json(name = "data") val data: AppleMusicData? = null,
    @Json(name = "result") val result: AppleMusicData? = null
)

@JsonClass(generateAdapter = true)
data class AppleMusicData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "album") val album: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "url") val url: String? = null
) {
    fun toSong(fallbackId: String = id ?: System.currentTimeMillis().toString()): Song {
        return Song(
            id = fallbackId,
            title = title ?: "Lagu Apple Music",
            artist = artist ?: "Artis Apple Music",
            album = album ?: "Album Apple Music",
            duration = duration ?: "4:30",
            coverUrl = cover ?: "",
            audioUrl = url ?: ""
        )
    }
}