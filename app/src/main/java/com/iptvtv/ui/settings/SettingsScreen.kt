package com.iptvtv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.iptvtv.data.ChannelRepository
import com.iptvtv.data.NsfwGate
import com.iptvtv.data.SettingsStore
import com.iptvtv.data.SyncState
import com.iptvtv.model.Country
import com.iptvtv.ui.appViewModel
import com.iptvtv.ui.components.PinDialog
import com.iptvtv.ui.theme.IptvColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class SettingsViewModel(
    private val repository: ChannelRepository,
    private val settings: SettingsStore,
    val gate: NsfwGate,
) : ViewModel() {
    val preferredCountry = settings.preferredCountry.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsStore.DEFAULT_COUNTRY)
    val lastSync = settings.lastSyncFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val syncState: StateFlow<SyncState> = repository.syncState
    val countries: StateFlow<List<Country>> = flow { emit(repository.countries()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setCountry(code: String) = viewModelScope.launch { settings.setPreferredCountry(code) }
    fun refresh() = viewModelScope.launch { repository.syncIfNeeded(force = true) }
    suspend fun checkPin(pin: String) = settings.checkPin(pin)
    suspend fun setPin(pin: String) = settings.setPin(pin)
}

private enum class PinStep { Current, New, Confirm }

@Composable
fun SettingsScreen() {
    val vm = appViewModel { _, c, _ -> SettingsViewModel(c.repository, c.settings, c.nsfwGate) }
    val countryCode by vm.preferredCountry.collectAsStateWithLifecycle()
    val countries by vm.countries.collectAsStateWithLifecycle()
    val lastSync by vm.lastSync.collectAsStateWithLifecycle()
    val sync by vm.syncState.collectAsStateWithLifecycle()
    val unlocked by vm.gate.unlocked.collectAsStateWithLifecycle()

    var pickCountry by remember { mutableStateOf(false) }
    var pinStep by remember { mutableStateOf<PinStep?>(null) }
    var newPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Section("Home country", "The first row on Home shows channels from this country.") {
            val country = countries.firstOrNull { it.code == countryCode }
            Button(onClick = { pickCountry = true }) {
                Text(country?.let { "${it.flag}  ${it.name}" } ?: countryCode)
            }
        }

        Section("Adult content", "Adult channels are locked with a PIN. Default PIN is ${SettingsStore.DEFAULT_PIN}.") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { pinStep = PinStep.Current }) { Text("Change PIN") }
                if (unlocked) OutlinedButton(onClick = { vm.gate.lock() }) { Text("Lock now") }
            }
            message?.let { Text(it, color = IptvColors.TextSecondary) }
        }

        Section(
            "Channel data",
            when (val s = sync) {
                is SyncState.Running -> "Updating: ${s.step}…"
                is SyncState.Failed -> "Last update failed: ${s.message}"
                SyncState.Idle -> if (lastSync == 0L) "Never updated" else
                    "Last updated ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastSync))}. Updates daily."
            },
        ) {
            Button(onClick = { vm.refresh() }, enabled = sync !is SyncState.Running) { Text("Update now") }
        }

        Section(
            "About",
            "Channel list from the iptv-org project (github.com/iptv-org/iptv). " +
                "This app does not host any streams; it only links to publicly available ones. " +
                "Some channels may be offline or geo-blocked.",
        ) {}
    }

    if (pickCountry) {
        CountryPicker(
            countries = countries,
            selected = countryCode,
            onPick = { vm.setCountry(it); pickCountry = false },
            onDismiss = { pickCountry = false },
        )
    }

    when (pinStep) {
        PinStep.Current -> PinDialog(
            title = "Current PIN",
            onSubmit = { pin -> vm.checkPin(pin).also { if (it) pinStep = PinStep.New } },
            onDismiss = { pinStep = null },
        )
        PinStep.New -> PinDialog(
            title = "New PIN",
            subtitle = "Choose a new ${SettingsStore.PIN_LENGTH}-digit PIN",
            onSubmit = { pin -> newPin = pin; pinStep = PinStep.Confirm; true },
            onDismiss = { pinStep = null },
        )
        PinStep.Confirm -> PinDialog(
            title = "Confirm PIN",
            subtitle = "Enter the new PIN again",
            errorText = "PINs do not match. Try again.",
            onSubmit = { pin ->
                (pin == newPin).also { ok ->
                    if (ok) {
                        vm.setPin(pin)
                        message = "PIN changed."
                        pinStep = null
                    }
                }
            },
            onDismiss = { pinStep = null },
        )
        null -> Unit
    }
}

@Composable
private fun Section(title: String, description: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(description, color = IptvColors.TextSecondary)
        content()
    }
}

@Composable
private fun CountryPicker(
    countries: List<Country>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            colors = SurfaceDefaults.colors(containerColor = IptvColors.Surface),
            modifier = Modifier.width(420.dp).height(480.dp),
        ) {
            LazyColumn(Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
                items(countries, key = { it.code }) { country ->
                    ListItem(
                        selected = country.code == selected,
                        onClick = { onPick(country.code) },
                        headlineContent = { Text("${country.flag}  ${country.name}") },
                        trailingContent = { Text("${country.channelCount}", color = IptvColors.TextMuted) },
                    )
                }
            }
        }
    }
}
