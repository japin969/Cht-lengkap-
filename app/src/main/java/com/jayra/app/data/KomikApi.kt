package com.jayra.app.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface KomikApi {
    @GET("api/komikindo/")
    suspend fun home(
        @Query("endpoint") endpoint: String = "home"
    ): Response<KomikHomeResponse>

    @GET("api/komikindo/")
    suspend fun list(
        @Query("endpoint") endpoint: String = "komik-terbaru",
        @Query("page") page: Int = 1
    ): Response<KomikListResponse>

    @GET("api/komikindo/")
    suspend fun genre(
        @Query("endpoint") endpoint: String = "genre",
        @Query("genre") genre: String,
        @Query("page") page: Int = 1
    ): Response<KomikListResponse>

    @GET("api/komikindo/")
    suspend fun search(
        @Query("endpoint") endpoint: String = "search",
        @Query("q") q: String
    ): Response<KomikListResponse>

    @GET("api/komikindo/")
    suspend fun detail(
        @Query("endpoint") endpoint: String = "detail",
        @Query("slug") slug: String
    ): Response<KomikDetailResponse>

    @GET("api/komikindo/")
    suspend fun stream(
        @Query("endpoint") endpoint: String = "stream",
        @Query("slug") slug: String
    ): Response<KomikStreamResponse>

    companion object {
        private const val BASE_URL = "https://api.nexadev.my.id/"

        fun create(): KomikApi {
            return NetworkClient.createRetrofit(BASE_URL).create(KomikApi::class.java)
        }
    }
}
