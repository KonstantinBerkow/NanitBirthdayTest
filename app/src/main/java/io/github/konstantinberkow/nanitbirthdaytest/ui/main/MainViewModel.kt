package io.github.konstantinberkow.nanitbirthdaytest.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.konstantinberkow.nanitbirthdaytest.NanitClientApp
import io.github.konstantinberkow.nanitbirthdaytest.network.WebSocketClient

class MainViewModel(
    private val webSocketClientFactory: () -> WebSocketClient
) : ViewModel() {
    // TODO: Implement the ViewModel

    object Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            val app =
                extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NanitClientApp
            val socketFactory = app.dependencies.webSocketOkHttpClientFactory
            return MainViewModel(socketFactory) as T
        }
    }
}