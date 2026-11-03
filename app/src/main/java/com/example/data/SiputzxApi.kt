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

interface SiputzxApi {

    @GET("api/ai/gptoss120b")
    suspend fun askAI(
        @Query("prompt") prompt: String,
        @Query("system") system: String = "You are a helpful assistant.",
        @Query("temperature") temperature: Float = 0.7f
    ): Response<AIResponse>

    companion object {
        private const val BASE_URL = "https://api.siputzx.my.id/"

        fun create(): SiputzxApi {
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
                .create(SiputzxApi::class.java)
        }
    }
}
