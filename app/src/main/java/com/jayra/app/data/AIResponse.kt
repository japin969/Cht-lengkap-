package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AIResponse(
    @Json(name = "status") val status: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: Any? = null,
    @Json(name = "result") val result: String? = null
) {
    fun getExtractedText(): String {
        val raw = when (val d = data) {
            is String -> d
            is Map<*, *> -> {
                val response = d["response"]?.toString()
                val results = d["results"] as? Map<*, *>
                val answer = results?.get("answer")?.toString()
                val text = d["text"]?.toString()
                val result = d["result"]?.toString()
                val msg = d["message"]?.toString()
                response ?: answer ?: text ?: result ?: msg ?: d.toString()
            }
            is List<*> -> d.firstOrNull()?.toString() ?: d.toString()
            null -> message ?: result ?: "Tidak ada balasan."
            else -> d.toString()
        }
        // Strip <think>...</think> tag (deepseek r1)
        return raw.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
    }
}
