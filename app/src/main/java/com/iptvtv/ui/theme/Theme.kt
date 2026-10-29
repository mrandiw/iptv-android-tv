package com.iptvtv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object IptvColors {
    val Red = Color(0xFFE50914)
    val Background = Color(0xFF141414)
    val Surface = Color(0xFF1F1F1F)
    val SurfaceHigh = Color(0xFF2B2B2B)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB3B3B3)
    val TextMuted = Color(0xFF808080)
}

@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = IptvColors.Red,
            onPrimary = Color.White,
            background = IptvColors.Background,
            onBackground = IptvColors.TextPrimary,
            surface = IptvColors.Surface,
            onSurface = IptvColors.TextPrimary,
            surfaceVariant = IptvColors.SurfaceHigh,
            onSurfaceVariant = IptvColors.TextSecondary,
            border = Color.White,
        ),
    ) {
        // Text outside a tv Surface has no content color by default (renders black), so set it app-wide.
        CompositionLocalProvider(LocalContentColor provides IptvColors.TextPrimary, content = content)
    }
}
