package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppleMusicResponse(
    @Json(name = "status") val status: Boolean? = true,
    @Json(name = "data") val data: List<AppleMusicItem>? = null
) {
    @JsonClass(generateAdapter = true)
    data class AppleMusicItem(
        @Json(name = "title") val title: String? = null,
        @Json(name = "artist") val artist: String? = null,
        @Json(name = "link") val link: String? = null,
        @Json(name = "image") val image: String? = null
    ) {
        fun toSong(fallbackId: String = System.currentTimeMillis().toString()): Song {
            // Apple Music hanya menyediakan link web, tidak ada direct audio URL.
            # Simpan link sebagai audioUrl; saat diputar, fallback ke YouTube jika gagal.
            val audioUrl = link?.takeIf { it.isNotBlank() } ?: ""
            return Song(
                id = "am_${title?.let { it.hashCode().toString() } ?: fallbackId}",
                title = title ?: "Lagu Tanpa Judul",
                artist = parseArtist(artist ?: "Artis Tidak Diketahui"),
                album = "Apple Music Single",
                duration = "0:00",
                coverUrl = image ?: "",
                audioUrl = audioUrl,
                source = "applemusic"
            )
        }

        private fun parseArtist(raw: String): String {
            // Format: "Song · Last Child" atau "Artist"
            return raw.split("·").lastOrNull()?.trim() ?: raw.trim()
        }
    }
}
