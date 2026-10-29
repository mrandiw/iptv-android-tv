package com.iptvtv.ui.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.navigation.toRoute
import com.iptvtv.BuildConfig
import com.iptvtv.data.ChannelRepository
import com.iptvtv.data.NsfwGate
import com.iptvtv.model.Channel
import com.iptvtv.model.ChannelSource
import com.iptvtv.model.Stream
import com.iptvtv.ui.PlayerRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val channel: Channel? = null,
    val streams: List<Stream> = emptyList(),
    val streamIndex: Int = 0,
    val buffering: Boolean = true,
    val error: String? = null,
    val position: Int = 0,
    val total: Int = 0,
) {
    val stream: Stream? get() = streams.getOrNull(streamIndex)
}

/**
 * Owns the ExoPlayer so playback survives configuration changes.
 * When a stream fails, the next stream of the same channel is tried automatically.
 */
@OptIn(UnstableApi::class)
class PlayerViewModel(
    app: Application,
    private val repository: ChannelRepository,
    private val gate: NsfwGate,
    handle: SavedStateHandle,
) : ViewModel() {

    private val route = handle.toRoute<PlayerRoute>()
    private val source = ChannelSource.decode(route.source)
    private var zapList: List<Channel> = emptyList()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /**
     * Cheap TV boxes and emulators often have hardware decoders that break mid-stream.
     * After a decoding error we switch to software decoders for the rest of this session.
     */
    private var preferSoftwareDecoders = false

    private val codecSelector = MediaCodecSelector { mimeType, secure, tunneling ->
        val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, secure, tunneling)
        if (preferSoftwareDecoders) decoders.sortedByDescending { it.softwareOnly } else decoders
    }

    val player: ExoPlayer = ExoPlayer.Builder(
        app,
        DefaultRenderersFactory(app)
            .setMediaCodecSelector(codecSelector)
            .setEnableDecoderFallback(true), // Also try the next decoder if one fails to start.
    ).build().apply {
        if (BuildConfig.DEBUG) addAnalyticsListener(EventLogger()) // Logs every player event to logcat.
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update { it.copy(buffering = playbackState == Player.STATE_BUFFERING) }
            }

            override fun onPlayerError(error: PlaybackException) {
                val isDecodingError = error.errorCode in
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED..PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED
                if (isDecodingError && !preferSoftwareDecoders) {
                    preferSoftwareDecoders = true
                    _state.value.stream?.let(::startStream) // Same stream again, now with software decoders.
                } else {
                    tryNextStream(error.errorCodeName)
                }
            }
        })
    }

    init {
        viewModelScope.launch {
            val channel = repository.channel(route.channelId)
            if (channel == null) {
                _state.update { it.copy(error = "Channel not found.", buffering = false) }
                return@launch
            }
            zapList = source?.let { repository.channels(it, includeNsfw = gate.unlocked.value) }
                ?.takeIf { list -> list.any { it.id == channel.id } }
                ?: listOf(channel)
            play(channel)
        }
    }

    /** Switches to the next (+1) or previous (-1) channel of the list the user came from. */
    fun zap(direction: Int) {
        val current = _state.value.channel ?: return
        if (zapList.size < 2) return
        val index = zapList.indexOfFirst { it.id == current.id }
        val next = zapList[(index + direction).mod(zapList.size)]
        viewModelScope.launch { play(next) }
    }

    fun retry() {
        _state.value.channel?.let { viewModelScope.launch { play(it) } }
    }

    private suspend fun play(channel: Channel) {
        if (channel.isNsfw && !gate.unlocked.value) {
            player.stop()
            _state.update { it.copy(channel = channel, error = "This channel is locked.", buffering = false) }
            return
        }
        val streams = repository.streams(channel.id)
        _state.value = PlayerUiState(
            channel = channel,
            streams = streams,
            position = zapList.indexOfFirst { it.id == channel.id } + 1,
            total = zapList.size,
        )
        if (streams.isEmpty()) {
            _state.update { it.copy(error = "No stream available for this channel.", buffering = false) }
            return
        }
        startStream(streams.first())
    }

    private fun tryNextStream(reason: String) {
        val s = _state.value
        val nextIndex = s.streamIndex + 1
        if (nextIndex < s.streams.size) {
            _state.update { it.copy(streamIndex = nextIndex, error = null, buffering = true) }
            startStream(s.streams[nextIndex])
        } else {
            _state.update { it.copy(error = "This channel is not available right now ($reason).", buffering = false) }
        }
    }

    private fun startStream(stream: Stream) {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(stream.userAgent ?: DEFAULT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
        stream.referrer?.let { http.setDefaultRequestProperties(mapOf("Referer" to it)) }

        val item = MediaItem.Builder()
            .setUri(stream.url)
            .apply { mimeTypeFor(stream.url)?.let(::setMimeType) }
            .build()

        player.setMediaSource(DefaultMediaSourceFactory(http).createMediaSource(item))
        player.prepare()
        player.playWhenReady = true
    }

    override fun onCleared() {
        player.release()
    }

    companion object {
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

        internal fun mimeTypeFor(url: String): String? {
            val path = url.substringBefore('?').lowercase()
            return when {
                path.endsWith(".m3u8") || path.endsWith(".m3u") -> MimeTypes.APPLICATION_M3U8
                path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
                else -> null // Let ExoPlayer detect it from the content.
            }
        }
    }
}
