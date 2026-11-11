package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NexadevAiResponse(
    @Json(name = "status") val status: Boolean? = null,
    @Json(name = "result") val result: String? = null,
    @Json(name = "response") val response: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: Any? = null
) {
    fun extractText(): String {
        return result ?: response ?: message ?: when (val d = data) {
            is String -> d
            is Map<*, *> -> (d["text"] ?: d["response"] ?: d["result"] ?: d["message"])?.toString() ?: d.toString()
            null -> "Tidak ada balasan."
            else -> d.toString()
        }
    }
}

interface NexadevAiApi {
    @GET("ai/gpt4")
    suspend fun askGpt4(@Query("q") query: String): Response<NexadevAiResponse>

    @GET("ai/chatgptpro")
    suspend fun askChatGptPro(@Query("q") query: String): Response<NexadevAiResponse>

    companion object {
        private const val BASE_URL = "https://api.nexadev.my.id/"

        fun create(): NexadevAiApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(NexadevAiApi::class.java)
        }
    }
}
