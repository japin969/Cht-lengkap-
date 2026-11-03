package com.jayra.app.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class CallSignal(
    val callId: String = "",
    val senderId: String = "",
    val sdp: String = "",
    val type: String = "offer", // "offer", "answer", "candidate", "end"
    val candidate: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
