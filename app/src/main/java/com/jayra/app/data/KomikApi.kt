package com.jayra.app.data

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface KomikApi {

    /**
     * Search komik: GET https://api.nexadev.my.id/komik?q=nano
     */
    @GET("komik")
    suspend fun search(@Query("q") q: String): Response<KomikListResponse>

    /**
     * List/Update komik: GET https://api.nexadev.my.id/komik?page=1
     */
    @GET("komik")
    suspend fun list(@Query("page") page: Int = 1): Response<KomikListResponse>

    /**
     * Detail komik: GET https://api.nexadev.my.id/komik?slug=the-great-ruler
     * Note: Returns same format as list (results list). We filter by slug client-side.
     */
    @GET("komik")
    suspend fun detail(@Query("slug") slug: String): Response<KomikListResponse>

    /**
     * Stream/Chapter: GET https://api.nexadev.my.id/komik?slug=the-great-ruler-chapter-304
     * Note: Returns same format as list. We filter by slug client-side.
     */
    @GET("komik")
    suspend fun stream(@Query("slug") slug: String): Response<KomikListResponse>

    companion object {
        private const val BASE_URL = "https://api.nexadev.my.id/"

        fun create(): KomikApi {
            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                        .addHeader("Referer", "https://komikindo.ch/")
                        .build()
                    chain.proceed(request)
                }
                .build()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            return Retrofit.Builder().baseUrl(BASE_URL).client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build().create(KomikApi::class.java)
        }
    }
}