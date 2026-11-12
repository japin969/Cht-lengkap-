package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PinterestResponse(
    @Json(name = "status") val status: Boolean? = null,
    @Json(name = "data") val data: List<PinterestItem>? = null
)

@JsonClass(generateAdapter = true)
data class PinterestItem(
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "grid_title") val gridTitle: String? = null
)
