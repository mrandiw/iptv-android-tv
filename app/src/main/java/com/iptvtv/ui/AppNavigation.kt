package com.iptvtv.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iptvtv.model.Channel
import com.iptvtv.model.ChannelSource
import com.iptvtv.ui.browse.CategoriesScreen
import com.iptvtv.ui.browse.ChannelGridScreen
import com.iptvtv.ui.browse.CountriesScreen
import com.iptvtv.ui.home.HomeScreen
import com.iptvtv.ui.player.PlayerScreen
import com.iptvtv.ui.search.SearchScreen
import com.iptvtv.ui.settings.SettingsScreen

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val selectTab: (Tab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(HomeRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val play: (Channel, ChannelSource?) -> Unit = { channel, source ->
        navController.navigate(PlayerRoute(channel.id, source?.encode()))
    }
    val openGrid: (ChannelSource, String) -> Unit = { source, title ->
        navController.navigate(GridRoute(source.encode(), title))
    }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            MainScaffold(Tab.Home, selectTab) { HomeScreen(onPlay = play, onSeeAll = openGrid) }
        }
        composable<SearchRoute> {
            MainScaffold(Tab.Search, selectTab) { SearchScreen(onPlay = play) }
        }
        composable<CountriesRoute> {
            MainScaffold(Tab.Countries, selectTab) { CountriesScreen(onOpen = openGrid) }
        }
        composable<CategoriesRoute> {
            MainScaffold(Tab.Categories, selectTab) { CategoriesScreen(onOpen = openGrid) }
        }
        composable<SettingsRoute> {
            MainScaffold(Tab.Settings, selectTab) { SettingsScreen() }
        }
        composable<GridRoute> {
            ChannelGridScreen(onPlay = play)
        }
        composable<PlayerRoute> {
            PlayerScreen()
        }
    }
}
