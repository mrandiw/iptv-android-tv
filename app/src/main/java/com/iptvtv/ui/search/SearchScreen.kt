package com.iptvtv.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Text
import com.iptvtv.data.ChannelRepository
import com.iptvtv.data.NsfwGate
import com.iptvtv.model.Channel
import com.iptvtv.model.ChannelSource
import com.iptvtv.ui.appViewModel
import com.iptvtv.ui.components.ChannelGrid
import com.iptvtv.ui.components.rememberPinGuard
import com.iptvtv.ui.theme.IptvColors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

class SearchViewModel(repository: ChannelRepository, gate: NsfwGate) : ViewModel() {
    val query = MutableStateFlow("")
    val unlocked: StateFlow<Boolean> = gate.unlocked

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Channel>> = combine(query.debounce(300), gate.unlocked) { q, u -> q.trim() to u }
        .mapLatest { (q, unlocked) ->
            if (q.length < 2) emptyList() else repository.channels(ChannelSource.BySearch(q), includeNsfw = unlocked)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun SearchScreen(onPlay: (Channel, ChannelSource?) -> Unit) {
    val vm = appViewModel { _, c, _ -> SearchViewModel(c.repository, c.nsfwGate) }
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val guard = rememberPinGuard()
    val keyboard = LocalSoftwareKeyboardController.current
    val fieldFocus = remember { FocusRequester() }
    var fieldFocused by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
        BasicTextField(
            value = query,
            onValueChange = { vm.query.value = it },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 22.sp),
            cursorBrush = SolidColor(IptvColors.Red),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .focusRequester(fieldFocus)
                .onFocusChanged { fieldFocused = it.isFocused }
                .background(IptvColors.Surface, RoundedCornerShape(6.dp))
                .border(
                    BorderStroke(2.dp, if (fieldFocused) Color.White else Color.Transparent),
                    RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("Search channels, e.g. \"CNN\" or \"TVRI\"", color = IptvColors.TextMuted, fontSize = 22.sp)
                    inner()
                }
            },
        )
        Text(
            when {
                query.trim().length < 2 -> "Type at least 2 letters"
                results.isEmpty() -> "No channels found"
                else -> "${results.size} channels"
            },
            color = IptvColors.TextSecondary,
            modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp),
        )
        ChannelGrid(
            channels = results,
            unlocked = unlocked,
            onClick = { ch -> guard(ch.isNsfw) { onPlay(ch, ChannelSource.BySearch(query.trim())) } },
        )
    }
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }
}
