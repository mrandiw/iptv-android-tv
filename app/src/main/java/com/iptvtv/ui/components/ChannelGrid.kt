package com.iptvtv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptvtv.model.Channel

@Composable
fun ChannelGrid(
    channels: List<Channel>,
    unlocked: Boolean,
    onClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        modifier = modifier,
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(channels, key = { it.id }) { channel ->
            ChannelCard(
                channel = channel,
                locked = channel.isNsfw && !unlocked,
                onClick = { onClick(channel) },
                width = 180.dp,
            )
        }
    }
}
