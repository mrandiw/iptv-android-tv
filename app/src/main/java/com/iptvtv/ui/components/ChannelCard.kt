package com.iptvtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.iptvtv.model.Channel
import com.iptvtv.ui.theme.IptvColors

/** Stable, distinct dark tint per channel so cards without good logos still look varied. */
fun channelTint(seed: String): Color {
    val hue = ((seed.hashCode() and 0x7fffffff) % 360).toFloat()
    return Color.hsv(hue, 0.45f, 0.30f)
}

/** Netflix-like 16:9 tile: grows and gets a white border when focused, title below. */
@Composable
fun ChannelCard(
    channel: Channel,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 196.dp,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val tint = channelTint(channel.id)
    Column(modifier.width(width), verticalArrangement = Arrangement.Top) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(6.dp)),
            ),
            colors = ClickableSurfaceDefaults.colors(containerColor = tint, focusedContainerColor = tint),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.35f)))),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    locked -> LockedBadge()
                    else -> ChannelLogo(channel, Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (locked) "Adult channel" else channel.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) IptvColors.TextPrimary else IptvColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ChannelLogo(channel: Channel, modifier: Modifier = Modifier) {
    // Many logo URLs in the dataset are dead; fall back to initials when loading fails.
    var failed by remember(channel.logoUrl) { mutableStateOf(false) }
    if (channel.logoUrl != null && !failed) {
        AsyncImage(
            model = channel.logoUrl,
            contentDescription = channel.name,
            contentScale = ContentScale.Fit,
            onError = { failed = true },
            modifier = modifier,
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = initials(channel.name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun LockedBadge() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text("18+", fontWeight = FontWeight.Bold, color = Color.White)
    }
}

internal fun initials(name: String): String =
    name.split(' ', '-', '_').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        .ifEmpty { "TV" }
