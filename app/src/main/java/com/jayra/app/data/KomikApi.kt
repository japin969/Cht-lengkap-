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

interface KomikApi {
    @GET("komik")
    suspend fun search(@Query("q") q: String): Response<KomikListResponse>

    @GET("komik/update")
    suspend fun update(@Query("page") page: Int = 1): Response<KomikListResponse>

    @GET("komik/detail")
    suspend fun detail(@Query("slug") slug: String): Response<KomikDetailResponse>

    @GET("komik/stream")
    suspend fun stream(@Query("slug") slug: String): Response<KomikStreamResponse>

    companion object {
        private const val BASE_URL = "https://apii.nexadev.my.id/"
        fun create(): KomikApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).build()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            return Retrofit.Builder().baseUrl(BASE_URL).client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build().create(KomikApi::class.java)
        }
    }
}
