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

    private suspend fun <T : Any> safeApiCall(block: suspend () -> Response<T>): Result<T> = withContext(Dispatchers.IO) {
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
                val spotifyDeferred = async { safeApiCall<SpotifyResponse> { nexadevApi.searchSpotify(query) } }
                val appleDeferred = async { safeApiCall<AppleMusicResponse> { siputzxApi.searchAppleMusic(query) } }
                val ytDeferred = async { safeApiCall<YouTubeResponse> { nexadevApi.searchYouTube(query) } }

                val (spotifyResult, appleResult, ytResult) = awaitAll(spotifyDeferred, appleDeferred, ytDeferred)

                val allSongs = mutableListOf<Song>()

                // SpotifyPlay
                if (spotifyResult.isSuccess) {
                    val response = spotifyResult.getOrNull()
                    response?.data?.map { it.toSong() }?.let { allSongs.addAll(it) }
                }

                // AppleMusic
                if (appleResult.isSuccess) {
                    val response = appleResult.getOrNull()
                    response?.data?.map { it.toSong() }?.let { allSongs.addAll(it) }
                }

                // YTPlay
                if (ytResult.isSuccess) {
                    val response = ytResult.getOrNull()
                    response?.toSong()?.let { allSongs.add(it) }
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
            val spotifyResult = safeApiCall<SpotifyResponse> { nexadevApi.searchSpotify("top hits") }
            if (spotifyResult.isSuccess) {
                val response = spotifyResult.getOrNull()
                response?.data?.map { it.toSong() } ?: emptyList()
            } else {
                // Fallback to YTPlay
                val ytResult = safeApiCall<YouTubeResponse> { nexadevApi.searchYouTube("top hits") }
                if (ytResult.isSuccess) {
                    val response = ytResult.getOrNull()
                    response?.toSong()?.let { listOf(it) } ?: emptyList()
                } else {
                    emptyList()
                }
            }
        }
    }
}