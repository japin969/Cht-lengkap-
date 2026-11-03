package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AIResponse(
    @Json(name = "status") val status: Boolean? = true,
    @Json(name = "result") val result: String? = null,
    @Json(name = "response") val response: String? = null,
    @Json(name = "data") val data: String? = null,
    @Json(name = "message") val message: String? = null
) {
    fun getText(): String {
        return result
            ?: response
            ?: data
            ?: message
            ?: "Maaf, Jayra AI tidak dapat memproses jawaban saat ini."
    }
}
