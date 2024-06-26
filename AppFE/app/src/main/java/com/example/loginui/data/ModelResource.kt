package com.example.loginui.data

import com.google.gson.annotations.SerializedName


data class ModelResource(
    @SerializedName("model_id") val modelId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("model_name") val modelName: String,
    val classes: List<String>,
    @SerializedName("crawl_number") val crawlNumber: Int,
    @SerializedName("created_at") var createdAt: String? = null,
    @SerializedName("img_urls") val imgUrls: List<String?>? = null,
    @SerializedName("token") val token: String,
    @SerializedName("status") val status: Int? = 0,
    @SerializedName("accuracy") val accuracy: Float? = 0.0f
)
