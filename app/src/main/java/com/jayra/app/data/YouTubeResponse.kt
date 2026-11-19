package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeResponse(
    @Json(name = "status") val status: Boolean? = true,
    @Json(name = "title") val title: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "data") val data: YouTubeData? = null
) {
    fun toSongs(): List<Song> {
        return listOf(
            Song(
                id = "yt_${System.currentTimeMillis()}",
                title = title ?: "Lagu YouTube",
                artist = "YouTube Music",
                album = "YouTube Stream",
                duration = duration ?: "3:45",
                coverUrl = thumbnail ?: "",
                audioUrl = data?.url ?: data?.audioUrl ?: "",
                source = "youtube"
            )
        )
    }
}

@JsonClass(generateAdapter = true)
data class YouTubeData(
    @Json(name = "url") val url: String? = null
)
