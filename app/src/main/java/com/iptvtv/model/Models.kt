package com.iptvtv.model

data class Channel(
    val id: String,
    val name: String,
    val countryCode: String,
    val categories: List<String>,
    val isNsfw: Boolean,
    val logoUrl: String?,
    val network: String?,
    val website: String?,
    val streamCount: Int,
)

data class Stream(
    val channelId: String,
    val url: String,
    val quality: String?,
    val userAgent: String?,
    val referrer: String?,
    val labels: List<String>,
)

data class Country(val code: String, val name: String, val flag: String, val channelCount: Int)

data class Category(val id: String, val name: String, val channelCount: Int)

/** Where a list of channels came from. Used for "see all" screens and channel zapping in the player. */
sealed interface ChannelSource {
    data class ByCountry(val code: String) : ChannelSource
    data class ByCategory(val id: String) : ChannelSource
    data class BySearch(val query: String) : ChannelSource

    fun encode(): String = when (this) {
        is ByCountry -> "country:$code"
        is ByCategory -> "category:$id"
        is BySearch -> "search:$query"
    }

    companion object {
        fun decode(value: String?): ChannelSource? {
            if (value.isNullOrBlank()) return null
            val (type, arg) = value.split(":", limit = 2).takeIf { it.size == 2 } ?: return null
            return when (type) {
                "country" -> ByCountry(arg)
                "category" -> ByCategory(arg)
                "search" -> BySearch(arg)
                else -> null
            }
        }
    }
}

const val NSFW_CATEGORY = "xxx"
