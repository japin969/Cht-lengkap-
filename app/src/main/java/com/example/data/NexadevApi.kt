package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface NexadevApi {

    @GET("api/spotifyplay")
    suspend fun searchSpotify(@Query("q") query: String): Response<SpotifyResponse>

    @GET("api/ytplay")
    suspend fun searchYouTube(@Query("q") query: String): Response<YouTubeResponse>

    @GET("api/spotify")
    suspend fun getSpotifyByUrl(@Query("url") url: String): Response<SpotifyResponse>

    companion object {
        private const val BASE_URL = "https://api.nexadev.my.id/"

        fun create(): NexadevApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(NexadevApi::class.java)
        }
    }
}
