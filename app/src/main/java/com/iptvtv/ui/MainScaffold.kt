package com.iptvtv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import com.iptvtv.ui.theme.IptvColors

enum class Tab(val label: String, val icon: ImageVector, val route: Any) {
    Search("Search", Icons.Default.Search, SearchRoute),
    Home("Home", Icons.Default.Home, HomeRoute),
    Countries("Countries", Icons.Default.Place, CountriesRoute),
    Categories("Categories", Icons.AutoMirrored.Filled.List, CategoriesRoute),
    Settings("Settings", Icons.Default.Settings, SettingsRoute),
}

/** Collapsed icon rail on the left that expands over the content on focus, like Netflix on TV. */
@Composable
fun MainScaffold(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        scrimBrush = Brush.horizontalGradient(listOf(Color.Black, Color.Black.copy(alpha = 0.0f))),
        drawerContent = { drawerValue ->
            Column(
                Modifier
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Color.Black, Color.Black.copy(alpha = 0.6f))))
                    .padding(12.dp)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterVertically),
            ) {
                if (drawerValue == DrawerValue.Open) {
                    Text(
                        "IPTV",
                        color = IptvColors.Red,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(start = 12.dp, bottom = 24.dp),
                    )
                }
                Tab.entries.forEach { tab ->
                    NavigationDrawerItem(
                        selected = tab == selected,
                        onClick = { if (tab != selected) onSelect(tab) },
                        leadingContent = { Icon(tab.icon, contentDescription = null) },
                    ) {
                        Text(tab.label)
                    }
                }
            }
        },
    ) {
        // Leave room for the collapsed rail so it never covers the first card.
        Box(Modifier.fillMaxSize().background(IptvColors.Background).padding(start = 72.dp)) {
            content()
        }
    }
}
