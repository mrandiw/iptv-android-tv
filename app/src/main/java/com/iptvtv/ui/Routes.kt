package com.iptvtv.ui

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object SearchRoute
@Serializable object CountriesRoute
@Serializable object CategoriesRoute
@Serializable object SettingsRoute
@Serializable object CreatedByRoute

/** "See all" grid. [source] is an encoded [com.iptvtv.model.ChannelSource]. */
@Serializable data class GridRoute(val source: String, val title: String)

/** Full-screen player. [source] lets the user switch channels with up/down. */
@Serializable data class PlayerRoute(val channelId: String, val source: String? = null)
