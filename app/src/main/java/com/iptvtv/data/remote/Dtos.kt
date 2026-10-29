package com.iptvtv.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Shapes of https://iptv-org.github.io/api/*.json. Only the fields we use are declared.

@Serializable
data class ChannelDto(
    val id: String,
    val name: String,
    val network: String? = null,
    val country: String = "",
    val categories: List<String> = emptyList(),
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    val closed: String? = null,
    val website: String? = null,
)

@Serializable
data class StreamDto(
    val channel: String? = null,
    val feed: String? = null,
    val title: String = "",
    val url: String,
    val quality: String? = null,
    val labels: List<String> = emptyList(),
    @SerialName("user_agent") val userAgent: String? = null,
    val referrer: String? = null,
)

@Serializable
data class LogoDto(
    val channel: String,
    val feed: String? = null,
    @SerialName("in_use") val inUse: Boolean = true,
    val width: Int = 0,
    val height: Int = 0,
    val format: String? = null,
    val url: String,
)

@Serializable
data class CategoryDto(val id: String, val name: String)

@Serializable
data class CountryDto(val code: String, val name: String, val flag: String = "")

@Serializable
data class BlocklistDto(val channel: String, val reason: String = "")
