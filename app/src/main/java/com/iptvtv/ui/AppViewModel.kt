package com.iptvtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iptvtv.AppContainer
import com.iptvtv.IptvApp

/** Creates a ViewModel with access to the [AppContainer] and the navigation arguments. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (app: IptvApp, container: AppContainer, handle: SavedStateHandle) -> VM,
): VM = viewModel(
    factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as IptvApp
            create(app, app.container, createSavedStateHandle())
        }
    },
)

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as IptvApp).container
