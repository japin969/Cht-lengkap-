package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeResponse(
    @Json(name = "status") val status: Boolean? = true,
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "channel") val channel: String? = null,
    @Json(name = "data") val data: YouTubeData? = null
) {
    fun toSong(fallbackId: String = System.currentTimeMillis().toString()): Song {
        return Song(
            id = id?.let { "yt_$it" } ?: fallbackId,
            title = title ?: "Lagu YouTube",
            artist = channel?.takeIf { it.isNotBlank() } ?: "YouTube Music",
            album = "YouTube Stream",
            duration = duration ?: "0:00",
            coverUrl = thumbnail ?: "",
            audioUrl = data?.url?.takeIf { it.isNotBlank() } ?: "",
            source = "youtube"
        )
    }
}

@JsonClass(generateAdapter = true)
data class YouTubeData(
    @Json(name = "url") val url: String? = null,
    @Json(name = "quality") val quality: String? = null
)
