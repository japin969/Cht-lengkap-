package com.jayra.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository tunggal untuk semua pencarian musik.
 * Menjalankan 3 API secara paralel: SpotifyPlay, AppleMusic, YTPlay.
 * Hasil digabung jadi 1 list, source label disembunyikan.
 */
class MusicRepository(
    private val nexadevApi: NexadevApi = NexadevApi.create(),
    private val siputzxApi: SiputzxApi = SiputzxApi.create()
) {

    /**
     * Cari lagu dari 3 sumber paralel.
     * Return: List<Song> yang sudah digabung & di-deduplicate by title+artist.
     */
    suspend fun searchAll(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            coroutineScope {
                val spotifyDeferred = async { searchSpotifyPlay(query) }
                val appleDeferred = async { searchAppleMusic(query) }
                val ytDeferred = async { searchYtPlay(query) }

                val (spotifyResult, appleResult, ytResult) = awaitAll(spotifyDeferred, appleDeferred, ytDeferred)

                val allSongs = mutableListOf<Song>()

                // SpotifyPlay
                spotifyResult.onSuccess { (response: Response<SpotifyResponse>) ->
                    if (response.isSuccessful) {
                        response.body()?.data?.map { it.toSong() }?.let { allSongs.addAll(it) }
                    }
                }

                // AppleMusic
                appleResult.onSuccess { (response: Response<AppleMusicResponse>) ->
                    if (response.isSuccessful) {
                        response.body()?.data?.map { it.toSong() }?.let { allSongs.addAll(it) }
                    }
                }

                // YTPlay
                ytResult.onSuccess { (response: Response<YouTubeResponse>) ->
                    if (response.isSuccessful) {
                        response.body()?.toSong()?.let { allSongs.add(it) }
                    }
                }

                // Deduplicate by title + artist (case insensitive)
                val seen = mutableSetOf<String>()
                allSongs.filter { song ->
                    val key = "${song.title.lowercase()}|${song.artist.lowercase()}"
                    if (key in seen) false else { seen.add(key); true }
                }
            }
        }
    }

    // --- Individual API calls (untuk fallback / testing) ---

    private suspend fun searchSpotifyPlay(query: String): Result<Response<SpotifyResponse>> =
        withContext(Dispatchers.IO) { runCatching { nexadevApi.searchSpotify(query) } }

    private suspend fun searchAppleMusic(query: String): Result<Response<AppleMusicResponse>> =
        withContext(Dispatchers.IO) { runCatching { siputzxApi.searchAppleMusic(query) } }

    private suspend fun searchYtPlay(query: String): Result<Response<YouTubeResponse>> =
        withContext(Dispatchers.IO) { runCatching { nexadevApi.searchYouTube(query) } }

    // --- Chart/Popular (for empty query) ---
    suspend fun getPopularChart(): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            // Try SpotifyPlay first
            val response = nexadevApi.searchSpotify("top hits")
            if (response.isSuccessful) {
                response.body()?.data?.map { it.toSong() } ?: emptyList()
            } else {
                // Fallback to YTPlay
                val ytResponse = nexadevApi.searchYouTube("top hits")
                if (ytResponse.isSuccessful) {
                    ytResponse.body()?.toSong()?.let { listOf(it) } ?: emptyList()
                } else {
                    emptyList()
                }
            }
        }
    }
}