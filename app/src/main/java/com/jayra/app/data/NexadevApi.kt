package com.jayra.app.data

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

    @GET("api/spotifyplay/")
    suspend fun searchSpotify(@Query("q") query: String): Response<SpotifyResponse>

    @GET("api/ytplay/")
    suspend fun searchYouTube(@Query("q") query: String): Response<YouTubeResponse>

    @GET("api/spotify/")
    suspend fun getSpotifyByUrl(@Query("url") url: String): Response<SpotifyResponse>



    // ═══════════════════════════════════════════════════════════════
    // KOMIK — parlemen komikindo (endpoint yang sama, dibedakan
    // parameter `type`, mengikuti pola yang sudah dipakai di project).
    // ═══════════════════════════════════════════════════════════════

    /** Daftar komik terbaru. `page` mulai dari 1. */
    @GET("komik/")
    suspend fun getKomikList(@Query("page") page: Int = 1): Response<KomikListResponse>

    /** Cari komik berdasarkan judul. */
    @GET("komik/")
    suspend fun searchKomik(@Query("q") query: String): Response<KomikListResponse>

    /** Detail komik: sinopsis, genre, dan daftar chapter. */
    @GET("komik/")
    suspend fun getKomikDetail(
        @Query("slug") slug: String,
        @Query("type") type: String = "detail"
    ): Response<KomikDetailResponse>

    /** Halaman gambar untuk satu chapter (dipakai reader). */
    @GET("komik/")
    suspend fun getKomikPages(
        @Query("slug") slug: String,
        @Query("type") type: String = "stream"
    ): Response<KomikStreamResponse>

    // ═══════════════════════════════════════════════════════════════
    // ANIME — animasu
    // ═══════════════════════════════════════════════════════════════

    /** Anime sedang tayang. */
    @GET("animasu/")
    suspend fun getAnimeOngoing(
        @Query("ongoing") ongoing: Int = 1,
        @Query("page") page: Int = 1
    ): Response<AnimeListResponse>

    /** Anime yang sudah selesai tayang. */
    @GET("animasu/")
    suspend fun getAnimeCompleted(
        @Query("completed") completed: Int = 1,
        @Query("page") page: Int = 1
    ): Response<AnimeListResponse>

    /** Anime populer. */
    @GET("animasu/")
    suspend fun getAnimePopular(
        @Query("popular") popular: Int = 1,
        @Query("page") page: Int = 1
    ): Response<AnimeListResponse>

    /** Jadwal tayang anime (schedule). */
    @GET("animasu/")
    suspend fun getAnimeSchedule(
        @Query("schedule") schedule: Int = 1
    ): Response<AnimeListResponse>

    /** Cari anime berdasarkan judul. */
    @GET("animasu/")
    suspend fun searchAnime(@Query("search") query: String): Response<AnimeSearchResponse>

    /** Detail anime: info, genre, sinopsis, daftar episode. */
    @GET("animasu/")
    suspend fun getAnimeDetail(@Query("details") details: String): Response<AnimeDetailResponse>

    /** Link streaming satu episode anime. */
    @GET("animasu/")
    suspend fun getAnimeStream(@Query("stream") stream: String): Response<AnimeStreamResponse>

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
