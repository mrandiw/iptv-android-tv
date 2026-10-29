package com.iptvtv

import android.content.Context
import com.iptvtv.data.ChannelRepository
import com.iptvtv.data.NsfwGate
import com.iptvtv.data.SettingsStore
import com.iptvtv.data.local.AppDatabase
import com.iptvtv.data.remote.IptvApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Manual dependency injection. Small enough that a DI framework is not worth it yet. */
class AppContainer(context: Context) {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    val settings = SettingsStore(context)
    val nsfwGate = NsfwGate(settings)
    private val database = AppDatabase.create(context)
    val repository = ChannelRepository(IptvApi(httpClient, json), database.channelDao(), settings)
}
