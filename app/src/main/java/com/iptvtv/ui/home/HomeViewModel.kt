package com.iptvtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvtv.data.ChannelRepository
import com.iptvtv.data.NsfwGate
import com.iptvtv.data.SettingsStore
import com.iptvtv.data.SyncState
import com.iptvtv.model.Channel
import com.iptvtv.model.ChannelSource
import com.iptvtv.model.NSFW_CATEGORY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeRow(
    val title: String,
    val source: ChannelSource,
    val channels: List<Channel>,
    val locked: Boolean = false,
)

sealed interface HomeUiState {
    data class Loading(val message: String) : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Content(val rows: List<HomeRow>) : HomeUiState
}

class HomeViewModel(
    private val repository: ChannelRepository,
    private val settings: SettingsStore,
    private val gate: NsfwGate,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading("Loading channels…"))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // Rebuild rows whenever the home country or the adult lock changes.
        viewModelScope.launch {
            combine(settings.preferredCountry, gate.unlocked) { c, u -> c to u }
                .distinctUntilChanged()
                .collect { (country, unlocked) -> if (repository.hasData()) _state.value = buildContent(country, unlocked) }
        }
        // Show sync progress only on first run, when there is nothing cached to show.
        viewModelScope.launch {
            repository.syncState.collect { sync ->
                if (repository.hasData()) return@collect
                _state.value = when (sync) {
                    is SyncState.Running -> HomeUiState.Loading("${sync.step}…")
                    is SyncState.Failed -> HomeUiState.Error(sync.message)
                    SyncState.Idle -> _state.value
                }
            }
        }
        refresh(force = false)
    }

    fun refresh(force: Boolean) {
        viewModelScope.launch {
            repository.syncIfNeeded(force)
            if (repository.hasData()) {
                _state.value = buildContent(settings.preferredCountry.first(), gate.unlocked.value)
            }
        }
    }

    private suspend fun buildContent(countryCode: String, unlocked: Boolean): HomeUiState.Content {
        val rows = mutableListOf<HomeRow>()

        val country = repository.country(countryCode)
        val local = repository.channels(ChannelSource.ByCountry(countryCode), includeNsfw = unlocked, limit = ROW_LIMIT)
        if (country != null && local.isNotEmpty()) {
            rows += HomeRow("${country.flag} Live in ${country.name}", ChannelSource.ByCountry(countryCode), local)
        }

        val categories = repository.categories().sortedBy { CATEGORY_ORDER.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
        for (category in categories) {
            if (category.id == NSFW_CATEGORY) continue
            val channels = repository.channels(ChannelSource.ByCategory(category.id), includeNsfw = unlocked, limit = ROW_LIMIT)
            if (channels.isNotEmpty()) rows += HomeRow(category.name, ChannelSource.ByCategory(category.id), channels)
        }

        // Adult row is always last and shown as locked tiles until the PIN is entered.
        val adult = repository.channels(ChannelSource.ByCategory(NSFW_CATEGORY), includeNsfw = true, limit = ROW_LIMIT)
        if (adult.isNotEmpty()) {
            rows += HomeRow("Adult (18+)", ChannelSource.ByCategory(NSFW_CATEGORY), adult, locked = !unlocked)
        }
        return HomeUiState.Content(rows)
    }

    companion object {
        private const val ROW_LIMIT = 30
        private val CATEGORY_ORDER = listOf(
            "news", "sports", "movies", "series", "entertainment", "kids", "music", "documentary",
            "general", "comedy", "animation", "family", "lifestyle", "cooking", "travel", "science",
        )
    }
}
