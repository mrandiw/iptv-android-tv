package com.iptvtv.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvtv.model.Channel
import com.iptvtv.model.ChannelSource
import com.iptvtv.ui.appViewModel
import com.iptvtv.ui.components.ChannelCard
import com.iptvtv.ui.components.ChannelLogo
import com.iptvtv.ui.components.LoadingView
import com.iptvtv.ui.components.MessageView
import com.iptvtv.ui.components.channelTint
import com.iptvtv.ui.components.rememberPinGuard
import com.iptvtv.ui.theme.IptvColors

@Composable
fun HomeScreen(
    onPlay: (Channel, ChannelSource?) -> Unit,
    onSeeAll: (ChannelSource, String) -> Unit,
) {
    val vm = appViewModel { _, c, _ -> HomeViewModel(c.repository, c.settings, c.nsfwGate) }
    val state by vm.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is HomeUiState.Loading -> LoadingView(s.message)
        is HomeUiState.Error -> MessageView(
            title = "Could not load channels",
            message = s.message,
            actionLabel = "Try again",
            onAction = { vm.refresh(force = true) },
        )
        is HomeUiState.Content -> HomeContent(s.rows, onPlay, onSeeAll)
    }
}

@Composable
private fun HomeContent(
    rows: List<HomeRow>,
    onPlay: (Channel, ChannelSource?) -> Unit,
    onSeeAll: (ChannelSource, String) -> Unit,
) {
    val guard = rememberPinGuard()
    var hero by remember(rows) { mutableStateOf(rows.firstOrNull()?.channels?.firstOrNull()) }
    var heroLocked by remember(rows) { mutableStateOf(false) }
    // Start on the first channel, not on the navigation rail.
    val contentFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { contentFocus.requestFocus() }

    Column(Modifier.fillMaxSize()) {
        Hero(
            channel = hero,
            locked = heroLocked,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().focusRequester(contentFocus),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(rows, key = { it.source.encode() }) { row ->
                ChannelRow(
                    row = row,
                    onFocused = { channel ->
                        hero = channel
                        heroLocked = row.locked
                    },
                    onClick = { channel -> guard(channel.isNsfw) { onPlay(channel, row.source) } },
                    onSeeAll = { guard(row.locked) { onSeeAll(row.source, row.title) } },
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ChannelRow(
    row: HomeRow,
    onFocused: (Channel) -> Unit,
    onClick: (Channel) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column {
        Text(
            row.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 32.dp, bottom = 10.dp),
        )
        LazyRow(
            // focusRestorer: coming back to a row puts focus on the last card used, not the first.
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(row.channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    locked = row.locked,
                    onClick = { onClick(channel) },
                    onFocused = { onFocused(channel) },
                )
            }
            item(key = "see-all") {
                SeeAllTile(onSeeAll)
            }
        }
    }
}

@Composable
private fun SeeAllTile(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.padding(top = 30.dp)) {
        Text("See all")
    }
}

/** Big banner for the focused channel, faded into the background on the left and bottom. */
@Composable
private fun Hero(channel: Channel?, locked: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = channel to locked,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier,
        label = "hero",
    ) { (ch, isLocked) ->
        if (ch == null) return@AnimatedContent
        Box(Modifier.fillMaxSize()) {
            val tint = channelTint(ch.id)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(tint, IptvColors.Background), radius = 1400f)),
            )
            if (!isLocked) {
                ChannelLogo(
                    ch,
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 64.dp, top = 24.dp, bottom = 24.dp)
                        .size(width = 340.dp, height = 190.dp),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(0f to IptvColors.Background, 0.55f to Color.Transparent)),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to IptvColors.Background)),
            )
            HeroText(ch, isLocked, Modifier.align(Alignment.CenterStart).padding(start = 32.dp).width(460.dp))
        }
    }
}

@Composable
private fun HeroText(channel: Channel, locked: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LIVE",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .background(IptvColors.Red, RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                listOfNotNull(channel.countryCode, channel.network).joinToString("  •  "),
                color = IptvColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            if (locked) "Adult channel" else channel.name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            channel.categories.joinToString("  •  ") { it.replaceFirstChar(Char::uppercase) },
            color = IptvColors.TextSecondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (locked) "PIN required" else "Press OK to watch",
                color = IptvColors.TextMuted,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}
