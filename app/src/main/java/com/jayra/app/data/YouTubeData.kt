package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeData(
    @Json(name = "url") val url: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "album") val album: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "coverUrl") val coverUrl: String? = null,
    @Json(name = "audioUrl") val audioUrl: String? = null
)