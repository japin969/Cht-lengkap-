package com.jayra.app.data

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

/**
 * Klien API Drama — https://api.anichin.bio/dramabox/
 *
 * PENTING (hasil tes 2026-11-22):
 *  - Header X-API-Key: TRIAL-ANICHIN-2026 -> 401 (DITOLAK). Jadi TIDAK dikirim.
 *  - Tanpa header khusus (cukup User-Agent + Accept) -> 200 OK.
 *  - Rate limit trial: ~50 req/menit. Semua call lewat [ApiRateLimiter].
 *  - Endpoint search kadang timeout di sisi server; repository menangani
 *    dengan cache + fallback.
 *
 * Timeout 15 detik sesuai spesifikasi.
 */
interface DramaApi {

    @GET("trending")
    suspend fun trending(): Response<DramaListResponse>

    @GET("foryou")
    suspend fun forYou(@Query("page") page: Int = 1): Response<DramaListResponse>

    @GET("hotrank")
    suspend fun hotRank(): Response<DramaListResponse>

    @GET("recommended")
    suspend fun recommended(): Response<DramaListResponse>

    @GET("search")
    suspend fun search(@Query("query") query: String): Response<DramaListResponse>

    @GET("detail")
    suspend fun detail(@Query("id") id: String): Response<DramaDetailResponse>

    @GET("episode")
    suspend fun episode(
        @Query("id") id: String,
        @Query("ep") ep: Int
    ): Response<DramaEpisodeResponse>

    companion object {
        private const val BASE_URL = "https://api.anichin.bio/dramabox/"

        /** User-Agent browser (menghindari penolakan sederhana). */
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        @Volatile
        private var instance: DramaApi? = null

        fun create(): DramaApi = instance ?: synchronized(this) {
            instance ?: build().also { instance = it }
        }

        private fun build(): DramaApi {
            // OkHttp default + trust store sistem. TIDAK ada SSL bypass.
            // Hanya menambah User-Agent + Accept agar tidak ditolak.
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val baseMoshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val episodeAdapter = baseMoshi.adapter(DramaEpisode::class.java)
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .add(DramaEpisodesField::class.java, DramaEpisodesAdapter(episodeAdapter))
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(DramaApi::class.java)
        }
    }
}