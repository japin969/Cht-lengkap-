package com.jayra.app.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NexadevApi {

    @GET("api/spotifyplay/")
    suspend fun searchSpotify(@Query("q") query: String): Response<SpotifyResponse>

    @GET("api/ytplay/")
    suspend fun searchYouTube(@Query("q") query: String): Response<YouTubeResponse>

    @GET("api/spotify/")
    suspend fun getSpotifyByUrl(@Query("url") url: String): Response<SpotifyResponse>

    @GET("api/dramabox/home")
    suspend fun getDramaHome(): Response<DramaHomeResponse>

    @GET("api/dramabox/detail")
    suspend fun getDramaDetail(@Query("url") url: String): Response<DramaDetailResponse>

    companion object {
        private const val BASE_URL = "https://api.nexadev.my.id/"

        fun create(): NexadevApi {
            return NetworkClient.createRetrofit(BASE_URL).create(NexadevApi::class.java)
        }
    }
}
