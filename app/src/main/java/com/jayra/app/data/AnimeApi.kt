package com.jayra.app.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

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
        private const val BASE_URL = "https://api.nexadev.my.id/"

        fun create(): AnimeApi {
            return NetworkClient.createRetrofit(BASE_URL).create(AnimeApi::class.java)
        }
    }
}
