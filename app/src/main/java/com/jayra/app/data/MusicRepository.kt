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

    private suspend fun <T : Any> callApi(block: suspend () -> Response<T>): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = block()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cari lagu dari 3 sumber paralel.
     * Return: List<Song> yang sudah digabung & di-deduplicate by title+artist.
     */
    suspend fun searchAll(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            coroutineScope {
                val spotifyDeferred = async { callApi<SpotifyResponse> { nexadevApi.searchSpotify(query) } }
                val appleDeferred = async { callApi<AppleMusicResponse> { siputzxApi.searchAppleMusic(query) } }
                val ytDeferred = async { callApi<YouTubeResponse> { nexadevApi.searchYouTube(query) } }

                val (spotifyResult, appleResult, ytResult) = awaitAll(spotifyDeferred, appleDeferred, ytDeferred)

                val allSongs = mutableListOf<Song>()

                // SpotifyPlay
                when (spotifyResult) {
                    is Result.Success -> {
                        val response = spotifyResult.getOrNull()
                        if (response is SpotifyResponse) {
                            response.data?.map { it.toSong() }?.let { allSongs.addAll(it) }
                        }
                    }
                    is Result.Failure -> {}
                }

                // AppleMusic
                when (appleResult) {
                    is Result.Success -> {
                        val response = appleResult.getOrNull()
                        if (response is AppleMusicResponse) {
                            response.data?.map { it.toSong() }?.let { allSongs.addAll(it) }
                        }
                    }
                    is Result.Failure -> {}
                }

                // YTPlay
                when (ytResult) {
                    is Result.Success -> {
                        val response = ytResult.getOrNull()
                        if (response is YouTubeResponse) {
                            response.toSong()?.let { allSongs.add(it) }
                        }
                    }
                    is Result.Failure -> {}
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

    // --- Chart/Popular (for empty query) ---
    suspend fun getPopularChart(): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            // Try SpotifyPlay first
            val spotifyResult = callApi<SpotifyResponse> { nexadevApi.searchSpotify("top hits") }
            if (spotifyResult.isSuccess) {
                val response = spotifyResult.getOrNull()
                if (response is SpotifyResponse) {
                    response.data?.map { it.toSong() } ?: emptyList()
                } else emptyList()
            } else {
                // Fallback to YTPlay
                val ytResult = callApi<YouTubeResponse> { nexadevApi.searchYouTube("top hits") }
                if (ytResult.isSuccess) {
                    val response = ytResult.getOrNull()
                    if (response is YouTubeResponse) {
                        response.toSong()?.let { listOf(it) } ?: emptyList()
                    } else emptyList()
                } else {
                    emptyList()
                }
            }
        }
    }
}