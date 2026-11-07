package com.jayra.app.data

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface AnimeApi {
    @GET("animasu")
    suspend fun search(@Query("search") q: String): Response<AnimeSearchResponse>

    @GET("animasu")
    suspend fun detail(@Query("details") url: String): Response<AnimeDetailResponse>

    @GET("animasu")
    suspend fun stream(@Query("stream") url: String): Response<AnimeStreamResponse>

    @GET("animasu")
    suspend fun ongoing(@Query("ongoing") on: Int = 1, @Query("page") page: Int = 1): Response<AnimeListResponse>

    @GET("animasu")
    suspend fun completed(@Query("completed") on: Int = 1, @Query("page") page: Int = 1): Response<AnimeListResponse>

    @GET("animasu")
    suspend fun popular(@Query("popular") on: Int = 1, @Query("page") page: Int = 1): Response<AnimeListResponse>

    @GET("animasu")
    suspend fun schedule(@Query("schedule") on: Int = 1): Response<AnimeListResponse>

    companion object {
        private const val BASE_URL = "https://apii.nexadev.my.id/"

        fun create(): AnimeApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AnimeApi::class.java)
        }
    }
}
