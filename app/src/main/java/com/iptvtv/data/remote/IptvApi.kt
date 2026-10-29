package com.iptvtv.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Client for the public iptv-org API (https://github.com/iptv-org/api).
 * This is the same data the iptv-org/iptv repository uses to generate its M3U playlists.
 *
 * Some files are several MB, so arrays are decoded as a stream, item by item,
 * instead of loading the whole document into memory.
 */
class IptvApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: String = BASE_URL,
) {
    suspend fun forEachChannel(onItem: (ChannelDto) -> Unit) = forEach("channels.json", ChannelDto.serializer(), onItem)
    suspend fun forEachStream(onItem: (StreamDto) -> Unit) = forEach("streams.json", StreamDto.serializer(), onItem)
    suspend fun forEachLogo(onItem: (LogoDto) -> Unit) = forEach("logos.json", LogoDto.serializer(), onItem)
    suspend fun forEachCategory(onItem: (CategoryDto) -> Unit) = forEach("categories.json", CategoryDto.serializer(), onItem)
    suspend fun forEachCountry(onItem: (CountryDto) -> Unit) = forEach("countries.json", CountryDto.serializer(), onItem)
    suspend fun forEachBlocked(onItem: (BlocklistDto) -> Unit) = forEach("blocklist.json", BlocklistDto.serializer(), onItem)

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun <T> forEach(file: String, strategy: DeserializationStrategy<T>, onItem: (T) -> Unit) =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(baseUrl + file).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("GET $file failed: HTTP ${response.code}")
                response.body.byteStream().use { input ->
                    json.decodeToSequence(input, strategy, DecodeSequenceMode.ARRAY_WRAPPED).forEach(onItem)
                }
            }
        }

    companion object {
        const val BASE_URL = "https://iptv-org.github.io/api/"
    }
}
