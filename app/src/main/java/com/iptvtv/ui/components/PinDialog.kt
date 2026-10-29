package com.iptvtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.iptvtv.data.SettingsStore
import com.iptvtv.ui.theme.IptvColors
import kotlinx.coroutines.launch

private val digitKeys = mapOf(
    Key.Zero to '0', Key.One to '1', Key.Two to '2', Key.Three to '3', Key.Four to '4',
    Key.Five to '5', Key.Six to '6', Key.Seven to '7', Key.Eight to '8', Key.Nine to '9',
    Key.NumPad0 to '0', Key.NumPad1 to '1', Key.NumPad2 to '2', Key.NumPad3 to '3', Key.NumPad4 to '4',
    Key.NumPad5 to '5', Key.NumPad6 to '6', Key.NumPad7 to '7', Key.NumPad8 to '8', Key.NumPad9 to '9',
)

/**
 * 4-digit PIN entry with an on-screen keypad. Remote number keys also work.
 * [onSubmit] returns false to show [errorText] and clear the input.
 */
@Composable
fun PinDialog(
    title: String,
    onSubmit: suspend (String) -> Boolean,
    onDismiss: () -> Unit,
    subtitle: String = "Enter your ${SettingsStore.PIN_LENGTH}-digit PIN",
    errorText: String = "Wrong PIN. Try again.",
) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val firstKey = remember { FocusRequester() }

    fun add(c: Char) {
        if (pin.length >= SettingsStore.PIN_LENGTH) return
        pin += c
        error = false
        if (pin.length == SettingsStore.PIN_LENGTH) {
            val entered = pin
            scope.launch {
                if (!onSubmit(entered)) {
                    error = true
                    pin = ""
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            colors = SurfaceDefaults.colors(containerColor = IptvColors.Surface),
            modifier = Modifier
                .width(360.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    digitKeys[event.key]?.let { add(it); true } ?: false
                },
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (error) errorText else subtitle,
                    color = if (error) IptvColors.Red else IptvColors.TextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(SettingsStore.PIN_LENGTH) { i ->
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(if (i < pin.length) Color.White else IptvColors.SurfaceHigh, CircleShape),
                        )
                    }
                }
                val rows = listOf("123", "456", "789")
                rows.forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEachIndexed { c, digit ->
                            KeyButton(
                                label = digit.toString(),
                                onClick = { add(digit) },
                                modifier = if (r == 0 && c == 0) Modifier.focusRequester(firstKey) else Modifier,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KeyButton("Cancel", onDismiss)
                    KeyButton("0", { add('0') })
                    KeyButton("⌫", { pin = pin.dropLast(1); error = false })
                }
            }
        }
        LaunchedEffect(Unit) { firstKey.requestFocus() }
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.width(88.dp)) {
        Text(label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
