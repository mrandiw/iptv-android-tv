package com.iptvtv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptvtv.ui.appContainer

/**
 * Returns a function that runs an action directly, or asks for the PIN first
 * when the action needs it and adult content is still locked this session.
 */
@Composable
fun rememberPinGuard(): (needsPin: Boolean, action: () -> Unit) -> Unit {
    val gate = appContainer().nsfwGate
    val unlocked by gate.unlocked.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    pending?.let { action ->
        PinDialog(
            title = "Adult content",
            onSubmit = { pin ->
                gate.tryUnlock(pin).also { ok ->
                    if (ok) {
                        pending = null
                        action()
                    }
                }
            },
            onDismiss = { pending = null },
        )
    }

    return { needsPin, action -> if (needsPin && !unlocked) pending = action else action() }
}
