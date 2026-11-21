package com.jayra.app.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface KomikApi {

    /**
     * Single unified endpoint for komik (source: Komikindo via Nexadev).
     * Handles search (?q=), list/home (?page=), detail (?slug=), stream (?slug=chapter).
     * Detail & stream return the same daftar-manga list shape — caller must match by slug.
     */
    @GET("komik")
    suspend fun list(@QueryMap params: Map<String, String>): Response<KomikResponse>

    companion object {
        private const val BASE_URL = "https://api.nexadev.my.id/"

        fun create(): KomikApi {
            return NetworkClient.createRetrofit(BASE_URL).create(KomikApi::class.java)
        }
    }
}
