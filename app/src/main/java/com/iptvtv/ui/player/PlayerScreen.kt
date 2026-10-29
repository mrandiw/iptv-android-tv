package com.iptvtv.ui.player

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvtv.ui.appViewModel
import com.iptvtv.ui.components.ChannelLogo
import com.iptvtv.ui.components.MessageView
import com.iptvtv.ui.components.Spinner
import com.iptvtv.ui.theme.IptvColors
import kotlinx.coroutines.delay

private const val OVERLAY_MILLIS = 4_000L

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen() {
    val vm = appViewModel { app, c, h -> PlayerViewModel(app, c.repository, c.nsfwGate, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    // Each key press bumps this, which restarts the auto-hide timer.
    var overlayTick by remember { mutableIntStateOf(1) }
    var overlayVisible by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableIntStateOf(15) }

    LaunchedEffect(overlayTick, state.channel?.id) {
        overlayVisible = true
        delay(OVERLAY_MILLIS)
        overlayVisible = false
    }

    LaunchedEffect(state.buffering, state.channel?.id, state.streamIndex) {
        if (state.buffering && state.error == null) {
            secondsLeft = 15
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.player.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.player.play() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                overlayTick++
                when (event.key) {
                    Key.DirectionUp, Key.ChannelUp -> { vm.zap(-1); true }
                    Key.DirectionDown, Key.ChannelDown -> { vm.zap(+1); true }
                    Key.DirectionCenter, Key.Enter -> { if (state.error != null) vm.retry(); true }
                    else -> false
                }
            }
            .focusable(),
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    isFocusable = false
                    player = vm.player
                }
            },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.buffering && state.error == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spinner(size = 48.dp)
                Text(
                    text = if (secondsLeft > 0) {
                        "Connecting stream... (${secondsLeft}s)"
                    } else {
                        "Still connecting, please wait..."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = if (state.streams.size > 1) {
                        "Source ${state.streamIndex + 1} of ${state.streams.size} • Please wait up to 15 seconds"
                    } else {
                        "Please wait up to 15 seconds while the live stream connects"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = IptvColors.TextSecondary,
                )
            }
        }

        state.error?.let { message ->
            MessageView(
                title = state.channel?.name ?: "Playback error",
                message = "$message\nPress OK to retry, ▲▼ to switch channel, Back to return.",
                modifier = Modifier.background(Color.Black.copy(alpha = 0.7f)),
            )
        }

        AnimatedVisibility(
            visible = overlayVisible && state.channel != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            InfoOverlay(state)
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun InfoOverlay(state: PlayerUiState) {
    val channel = state.channel ?: return
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
            .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 160.dp, height = 90.dp)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                ChannelLogo(channel, Modifier.fillMaxSize().padding(10.dp))
            }
            Spacer(Modifier.width(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(channel.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val details = listOfNotNull(
                    if (state.total > 1) "${state.position} / ${state.total}" else null,
                    channel.countryCode,
                    state.stream?.quality,
                    if (state.streams.size > 1) "Source ${state.streamIndex + 1} of ${state.streams.size}" else null,
                ) + state.stream?.labels.orEmpty()
                Text(details.joinToString("  •  "), color = IptvColors.TextSecondary)
                if (state.total > 1) {
                    Text("▲ ▼  switch channel", color = IptvColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
