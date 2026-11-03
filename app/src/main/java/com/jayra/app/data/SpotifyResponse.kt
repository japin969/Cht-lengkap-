package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpotifyResponse(
    @Json(name = "status") val status: Boolean? = true,
    @Json(name = "data") val data: SpotifyData? = null,
    @Json(name = "result") val result: SpotifyData? = null
)

@JsonClass(generateAdapter = true)
data class SpotifyData(
    @Json(name = "title") val title: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "album") val album: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "mp3_url") val mp3Url: String? = null,
    @Json(name = "preview") val preview: String? = null,
    @Json(name = "url") val url: String? = null
) {
    fun toSong(fallbackId: String = System.currentTimeMillis().toString()): Song {
        return Song(
            id = fallbackId,
            title = title ?: "Lagu Tanpa Judul",
            artist = artist ?: "Artis Tidak Diketahui",
            album = album ?: "Single",
            duration = duration ?: "3:30",
            coverUrl = cover ?: "",
            audioUrl = mp3Url ?: url ?: preview ?: ""
        )
    }
}
