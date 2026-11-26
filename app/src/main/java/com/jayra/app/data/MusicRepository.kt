package com.jayra.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * MusicRepository - Parallel fetch from multiple music sources.
 * Sources: Deezer (primary) + YouTube (via NexadevApi)
 */
class MusicRepository : BaseRepository<Song>() {

    private val deezerApi = DeezerApi.create()
    private val nexadevApi = NexadevApi.create()

    override val cacheTtlMs = 10 * 60 * 1000 // 10 minutes

    // Curated fallback songs
    private val curatedSongs = listOf(
        Song(
            id = "seed_1",
            title = "Starboy",
            artist = "The Weeknd, Daft Punk",
            album = "Starboy",
            duration = "3:50",
            coverUrl = "",
            audioUrl = "",
            source = "curated"
        )
    )

    // ═══════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════

    /** Search songs from all sources in parallel */
    suspend fun search(query: String): RepositoryResult<Song> {
        return fetchParallel(
            "music_search_$query",
            FetchSource("deezer") { fetchDeezerSearch(query) },
            FetchSource("youtube") { fetchYouTubeSearch(query) }
        )
    }

    /** Get curated songs when search fails */
    suspend fun getCurated(): List<Song> = curatedSongs

    // ═══════════════════════════════════════════
    // DEEZER API
    // ═══════════════════════════════════════════

    private suspend fun fetchDeezerSearch(query: String): List<Song> {
        val response = deezerApi.search(query = query, limit = 25)
        val tracks = response.body()?.data.orEmpty()
        return tracks.map { it.toSong() }
    }

    // ═══════════════════════════════════════════
    // YOUTUBE API (via NexadevApi)
    // ═══════════════════════════════════════════

    private suspend fun fetchYouTubeSearch(query: String): List<Song> {
        val response = nexadevApi.searchYouTube(query)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            listOf(body.toSong())
        } else emptyList()
    }

    // ═══════════════════════════════════════════
    // DEDUPLICATION & MERGE
    // ═══════════════════════════════════════════

    override fun getId(item: Song): String = item.id

    override fun mergeDuplicates(items: List<Song>): List<Song> {
        val byId = mutableMapOf<String, Song>()
        items.forEach { song ->
            val existing = byId[song.id]
            byId[song.id] = if (existing == null) song else mergeSong(existing, song)
        }
        return byId.values.toList()
    }

    private fun mergeSong(a: Song, b: Song): Song {
        // Prefer song with audio URL, then with cover
        val hasAudioA = a.audioUrl.isNotBlank()
        val hasAudioB = b.audioUrl.isNotBlank()
        val hasCoverA = a.coverUrl.isNotBlank()
        val hasCoverB = b.coverUrl.isNotBlank()

        return when {
            hasAudioA && !hasAudioB -> a
            !hasAudioA && hasAudioB -> b
            hasCoverA && !hasCoverB -> a
            !hasCoverA && hasCoverB -> b
            else -> a // Prefer first (Deezer)
        }
    }

    // ═══════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════

    data class Song(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val duration: String,
        val coverUrl: String,
        val audioUrl: String,
        val source: String
    )
}

// ═══════════════════════════════════════════
// DEEZER EXTENSIONS
// ═══════════════════════════════════════════

private fun DeezerTrack.toSong(): MusicRepository.Song {
    return MusicRepository.Song(
        id = "dz_${id ?: System.currentTimeMillis()}",
        title = title ?: "Tanpa Judul",
        artist = artist?.name ?: "Unknown",
        album = album?.title ?: "",
        duration = formatDuration(duration ?: 0),
        coverUrl = album?.coverBig ?: album?.coverMedium ?: "",
        audioUrl = preview ?: "",
        source = "deezer"
    )
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// ═══════════════════════════════════════════
// YOUTUBE EXTENSIONS (via NexadevApi.YouTubeResponse)
// ═══════════════════════════════════════════

private fun YouTubeResponse.toSong(): MusicRepository.Song {
    // YouTubeResponse structure depends on actual API response
    // This is a placeholder - adjust based on actual response
    return MusicRepository.Song(
        id = "yt_${System.currentTimeMillis()}",
        title = "YouTube Result",
        artist = "YouTube",
        album = "",
        duration = "0:00",
        coverUrl = "",
        audioUrl = "",
        source = "youtube"
    )
}