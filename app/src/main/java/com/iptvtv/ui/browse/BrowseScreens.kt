package com.iptvtv.ui.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.iptvtv.data.ChannelRepository
import com.iptvtv.data.NsfwGate
import com.iptvtv.model.Category
import com.iptvtv.model.Channel
import com.iptvtv.model.ChannelSource
import com.iptvtv.model.Country
import com.iptvtv.model.NSFW_CATEGORY
import com.iptvtv.ui.GridRoute
import com.iptvtv.ui.appViewModel
import com.iptvtv.ui.components.ChannelGrid
import com.iptvtv.ui.components.LoadingView
import com.iptvtv.ui.components.MessageView
import com.iptvtv.ui.components.channelTint
import com.iptvtv.ui.components.rememberPinGuard
import com.iptvtv.ui.theme.IptvColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

// ---- Countries ----

class CountriesViewModel(repository: ChannelRepository) : ViewModel() {
    val countries: StateFlow<List<Country>?> = flow { emit(repository.countries()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun CountriesScreen(onOpen: (ChannelSource, String) -> Unit) {
    val vm = appViewModel { _, c, _ -> CountriesViewModel(c.repository) }
    val countries by vm.countries.collectAsStateWithLifecycle()
    val list = countries ?: return LoadingView("Loading countries…")
    TileGrid(title = "Countries") {
        items(list, key = { it.code }) { country ->
            Tile(
                seed = country.code,
                big = country.flag.ifEmpty { country.code },
                title = country.name,
                subtitle = "${country.channelCount} channels",
                onClick = { onOpen(ChannelSource.ByCountry(country.code), "${country.flag} ${country.name}") },
            )
        }
    }
}

// ---- Categories ----

class CategoriesViewModel(repository: ChannelRepository) : ViewModel() {
    val categories: StateFlow<List<Category>?> = flow { emit(repository.categories()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun CategoriesScreen(onOpen: (ChannelSource, String) -> Unit) {
    val vm = appViewModel { _, c, _ -> CategoriesViewModel(c.repository) }
    val categories by vm.categories.collectAsStateWithLifecycle()
    val guard = rememberPinGuard()
    val list = categories ?: return LoadingView("Loading categories…")
    TileGrid(title = "Categories") {
        items(list, key = { it.id }) { category ->
            val adult = category.id == NSFW_CATEGORY
            Tile(
                seed = category.id,
                big = if (adult) "18+" else category.name.take(1),
                title = if (adult) "Adult" else category.name,
                subtitle = "${category.channelCount} channels",
                onClick = { guard(adult) { onOpen(ChannelSource.ByCategory(category.id), category.name) } },
            )
        }
    }
}

// ---- Channel grid ("See all") ----

class ChannelGridViewModel(repository: ChannelRepository, gate: NsfwGate, handle: SavedStateHandle) : ViewModel() {
    private val route = handle.toRoute<GridRoute>()
    val title = route.title
    val source: ChannelSource? = ChannelSource.decode(route.source)
    val unlocked: StateFlow<Boolean> = gate.unlocked

    @OptIn(ExperimentalCoroutinesApi::class)
    val channels: StateFlow<List<Channel>?> = gate.unlocked
        .mapLatest { unlocked -> source?.let { repository.channels(it, includeNsfw = unlocked) }.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ChannelGridScreen(onPlay: (Channel, ChannelSource?) -> Unit) {
    val vm = appViewModel { _, c, h -> ChannelGridViewModel(c.repository, c.nsfwGate, h) }
    val channels by vm.channels.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val guard = rememberPinGuard()

    Column(Modifier.fillMaxSize().background(IptvColors.Background)) {
        Text(
            vm.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 32.dp, top = 32.dp, bottom = 16.dp),
        )
        val list = channels
        when {
            list == null -> LoadingView("Loading…")
            list.isEmpty() -> MessageView("Nothing here", "No channels in this list.")
            else -> ChannelGrid(list, unlocked, onClick = { ch -> guard(ch.isNsfw) { onPlay(ch, vm.source) } })
        }
    }
}

// ---- Shared tiles ----

@Composable
private fun TileGrid(title: String, content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 32.dp, top = 32.dp, bottom = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 170.dp),
            contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 48.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun Tile(seed: String, big: String, title: String, subtitle: String, onClick: () -> Unit) {
    val tint = channelTint(seed)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, androidx.compose.ui.graphics.Color.White), shape = RoundedCornerShape(8.dp)),
        ),
        colors = ClickableSurfaceDefaults.colors(containerColor = tint, focusedContainerColor = tint),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(big, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = IptvColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}
