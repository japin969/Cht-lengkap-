package com.example.data

import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: String = "0:00",
    val coverUrl: String = "",
    val audioUrl: String = ""
)
