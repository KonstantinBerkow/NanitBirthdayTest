package io.github.konstantinberkow.nanitbirthdaytest.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.github.konstantinberkow.nanitbirthdaytest.NanitClientApp
import io.github.konstantinberkow.nanitbirthdaytest.network.BirthdayMessage
import io.github.konstantinberkow.nanitbirthdaytest.network.WebSocketClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart

private const val TAG = "MainViewModel"

class MainViewModel(
    private val webSocketClientFactory: () -> WebSocketClient,
    private val transformAddressFromInput: (String) -> String,
    private val gson: Gson
) : ViewModel() {

    // only existing action - connect
    private val actionsFlow = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val stateLiveData: LiveData<State> =
        actionsFlow
            .filter { it.isNotBlank() } // prevent default
            .flatMapLatest { address ->
                val decoratedAddress = transformAddressFromInput(address)
                Log.d(TAG, "raw address: $address")
                Log.d(TAG, "target address: $decoratedAddress")
                channelFlow {
                    send(State.connecting(address))
                    val client = webSocketClientFactory()
                    client.subscribe(
                        input = flowOf(WebSocketClient.Command.Connect(decoratedAddress)),
                        stateUpdated = { socketState ->
                            Log.d(
                                TAG,
                                "Latest socket state: $socketState, thread: ${Thread.currentThread()}"
                            )

                            val newState = when (socketState) {
                                is WebSocketClient.Event.State.Closed,
                                is WebSocketClient.Event.State.Closing ->
                                    State.closed(address)

                                is WebSocketClient.Event.State.Connected -> {
                                    socketState.webSocket.send("HappyBirthday")
                                    State.connected(address)
                                }

                                is WebSocketClient.Event.State.Failed ->
                                    State.failed(address, socketState.throwable)
                            }
                            send(newState)
                        },
                        textHandler = { newText ->
                            try {
                                val message = gson.fromJson(newText, BirthdayMessage::class.java)
                                send(State.info(address, message))
                            } catch (je: JsonSyntaxException) {
                                Log.e(TAG, "Failed to parse", je)
                            }
                        },
                        coroutineScope = viewModelScope
                    )
                }
            }
            .onStart {
                emit(
                    State()
                )
            }
            .catch { Log.e(TAG, "Unhandled exception", it) }
            .asLiveData(context = viewModelScope.coroutineContext)

    fun setWebSocketAddress(address: String) {
        Log.d(TAG, "setWebSocketAddress: $address")
        actionsFlow.tryEmit(address)
    }

    data class State(
        val rawAddress: String = "",
        val connecting: Boolean = false,
        val connected: Boolean = false,
        val failure: Throwable? = null,
        val info: BirthdayMessage? = null
    ) {

        companion object {

            fun connecting(address: String) =
                State(
                    rawAddress = address,
                    connecting = true,
                )

            fun connected(address: String) =
                State(
                    rawAddress = address,
                    connecting = false,
                    connected = true,
                )

            fun closed(address: String) =
                State(
                    rawAddress = address,
                )

            fun failed(address: String, cause: Throwable) =
                State(
                    rawAddress = address,
                    failure = cause
                )

            fun info(address: String, birthdayMessage: BirthdayMessage) =
                State(
                    rawAddress = address,
                    connected = true,
                    info = birthdayMessage
                )
        }
    }

    object Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            val app =
                extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NanitClientApp
            val dependencies = app.dependencies
            val socketFactory = dependencies.webSocketOkHttpClientFactory
            val transformAddressFromInput = dependencies.transformAddressFromInput
            return MainViewModel(
                webSocketClientFactory = socketFactory,
                transformAddressFromInput = transformAddressFromInput,
                gson = dependencies.gson
            ) as T
        }
    }
}