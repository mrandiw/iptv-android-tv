package com.iptvtv.data

import com.iptvtv.data.local.ChannelDao
import com.iptvtv.data.remote.IptvApi
import com.iptvtv.model.Category
import com.iptvtv.model.Channel
import com.iptvtv.model.ChannelSource
import com.iptvtv.model.Country
import com.iptvtv.model.Stream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val step: String) : SyncState
    data class Failed(val message: String) : SyncState
}

/**
 * Single source of truth for channel data.
 * Reads always come from the local Room cache; [sync] refreshes the cache from iptv-org.
 */
class ChannelRepository(
    private val api: IptvApi,
    private val dao: ChannelDao,
    private val settings: SettingsStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun hasData(): Boolean = dao.channelCount() > 0

    suspend fun isStale(): Boolean = clock() - settings.lastSyncMillis() > MAX_AGE_MILLIS

    /** Downloads the catalog when it is missing or older than a day (or always when [force]). */
    suspend fun syncIfNeeded(force: Boolean = false) {
        if (!force && hasData() && !isStale()) return
        sync()
    }

    private suspend fun sync() = syncMutex.withLock {
        try {
            val assembler = CatalogAssembler()
            step("Checking blocklist") { api.forEachBlocked { assembler.addBlocked(it.channel) } }
            step("Downloading streams") { api.forEachStream(assembler::addStream) }
            step("Downloading logos") { api.forEachLogo(assembler::addLogo) }
            step("Downloading channels") { api.forEachChannel(assembler::addChannel) }
            step("Downloading countries") {
                api.forEachCountry(assembler::addCountry)
                api.forEachCategory(assembler::addCategory)
            }
            step("Saving") {
                val catalog = withContext(Dispatchers.Default) { assembler.build() }
                dao.replaceAll(catalog.channels, catalog.streams, catalog.countries, catalog.categories)
                settings.setLastSyncMillis(clock())
            }
            _syncState.value = SyncState.Idle
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _syncState.value = SyncState.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private suspend inline fun step(name: String, block: () -> Unit) {
        _syncState.value = SyncState.Running(name)
        block()
    }

    suspend fun channel(id: String): Channel? = dao.channel(id)?.toModel()

    suspend fun streams(channelId: String): List<Stream> = dao.streams(channelId).map { it.toModel() }

    suspend fun countries(): List<Country> = dao.countries().map { it.toModel() }

    suspend fun country(code: String): Country? = dao.country(code)?.toModel()

    suspend fun categories(): List<Category> = dao.categories().map { it.toModel() }

    suspend fun channels(source: ChannelSource, includeNsfw: Boolean, limit: Int = -1): List<Channel> {
        val rows = when (source) {
            is ChannelSource.ByCountry -> dao.byCountry(source.code, includeNsfw, limit)
            is ChannelSource.ByCategory -> dao.byCategory(source.id, includeNsfw, limit)
            is ChannelSource.BySearch -> dao.search(source.query.trim(), includeNsfw, if (limit > 0) limit else 200)
        }
        return rows.map { it.toModel() }
    }

    companion object {
        const val MAX_AGE_MILLIS = 24L * 60 * 60 * 1000
    }
}
