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

interface SiputzxApi {

    @GET("api/s/pinterest")
    suspend fun searchPinterest(
        @Query("query") query: String
    ): Response<PinterestResponse>

    @GET("api/ai/gptoss120b")
    suspend fun askGptOss(
        @Query("prompt") prompt: String,
        @Query("system") system: String = "You are a helpful assistant.",
        @Query("temperature") temperature: Float = 0.7f
    ): Response<AIResponse>

    @GET("api/ai/glm47flash")
    suspend fun askGlm47Flash(
        @Query("prompt") prompt: String,
        @Query("system") system: String = "You are a helpful assistant.",
        @Query("temperature") temperature: Float = 0.7f
    ): Response<AIResponse>

    @GET("api/ai/phi2")
    suspend fun askPhi2(
        @Query("prompt") prompt: String,
        @Query("system") system: String = "You are a helpful assistant.",
        @Query("temperature") temperature: Float = 0.7f
    ): Response<AIResponse>

    @GET("api/ai/qwq32b")
    suspend fun askQwq32b(
        @Query("prompt") prompt: String,
        @Query("system") system: String = "You are a helpful assistant.",
        @Query("temperature") temperature: Float = 0.7f
    ): Response<AIResponse>

    @GET("api/ai/deepseekr1")
    suspend fun askDeepseekR1(
        @Query("prompt") prompt: String,
        @Query("system") system: String = "You are a helpful assistant.",
        @Query("temperature") temperature: Float = 0.7f
    ): Response<AIResponse>

    @GET("api/ai/gita")
    suspend fun askGita(@Query("q") q: String): Response<AIResponse>

    companion object {
        private const val AI_BASE_URL = "https://api.siputzx.my.id/"

        fun create(): SiputzxApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(AI_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SiputzxApi::class.java)
        }
    }
}
