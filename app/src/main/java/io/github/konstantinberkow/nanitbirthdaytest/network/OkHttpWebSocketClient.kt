package io.github.konstantinberkow.nanitbirthdaytest.network

import android.util.Log
import io.github.konstantinberkow.nanitbirthdaytest.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlin.coroutines.CoroutineContext

private const val TAG = "OkHttpWebSocketClient"

class OkHttpWebSocketClient(
    private val okHttpClientProvider: () -> OkHttpClient,
    private val coroutineContext: CoroutineContext
) : WebSocketClient {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun subscribe(
        input: Flow<WebSocketClient.Command>,
        textHandler: suspend (String) -> Unit,
        bytesHandler: suspend (ByteString) -> Unit,
        stateUpdated: suspend (WebSocketClient.Event.State) -> Unit,
        coroutineScope: CoroutineScope
    ) = withContext(coroutineContext) {
        val webSocketEvents = input
            .filterIsInstance<WebSocketClient.Command.Connect>()
            .flatMapLatest { connect ->
                logDebug { "callbackFlow start" }
                callbackFlow {
                    val listener = ProductScopeWebSocketListener(this)
                    val request = Request.Builder()
                        .url(connect.webSocketUrl)
                        .build()
                    val webSocket = okHttpClientProvider().newWebSocket(request, listener)
                    logDebug { "connection initiated via $webSocket" }

                    awaitClose {
                        logDebug { "callbackFlow closing" }
                        // https://www.rfc-editor.org/rfc/rfc6455.html#section-7.4.1
                        webSocket.close(code = 1000, reason = "Client disconnected")
                    }
                }
            }
            .onEach { logDebug { "Emitting next event: $it" } }
            .shareIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed())

        // perform incoming commands
        val actions = webSocketEvents
            .mapNotNull { event ->
                when (event) {
                    is WebSocketClient.Event.Data -> null
                    is WebSocketClient.Event.State -> event
                }
            }
            .flatMapLatest { lastState ->
                logDebug { "Execute 'stateUpdate' for $lastState" }
                stateUpdated(lastState)
                val webSocket = lastState.webSocket
                when (lastState) {
                    is WebSocketClient.Event.State.Connected ->
                        input.mapNotNull { command ->
                            when (command) {
                                is WebSocketClient.Command.Connect -> {
                                    // ignore, already connected, shouldn't be in such state
                                    null
                                }

                                is WebSocketClient.Command.SendText ->
                                    Action.SendText(webSocket, command.text)

                                is WebSocketClient.Command.SendBytes ->
                                    Action.SendBytes(webSocket, command.bytes)

                                is WebSocketClient.Command.Disconnect ->
                                    Action.Disconnect(webSocket, command.code, command.reason)
                            }
                        }

                    is WebSocketClient.Event.State.Closing,
                    is WebSocketClient.Event.State.Closed,
                    is WebSocketClient.Event.State.Failed -> {
                        // commands shouldn't be accepted if not connected
                        emptyFlow()
                    }
                }
            }

        val executeIncomingCommands = async {
            actions.collect {
                it.execute()
            }
        }

        logDebug { "proceed to data collect" }
        val relayOutgoingMessages = async {
            webSocketEvents.collect { event ->
                logDebug { "Collect event $event" }
                when (event) {
                    is WebSocketClient.Event.State -> {
                        // ignore this events
                    }

                    is WebSocketClient.Event.Data.TextMessage ->
                        textHandler(event.text)

                    is WebSocketClient.Event.Data.BytesMessage ->
                        bytesHandler(event.bytes)
                }
            }
        }

        executeIncomingCommands.await()
        relayOutgoingMessages.await()
    }
}

class ProductScopeWebSocketListener(
    private val producerScope: ProducerScope<WebSocketClient.Event>
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        logDebug { "onOpen $webSocket, response: $response" }
        producerScope.trySendBlocking(
            WebSocketClient.Event.State.Connected(webSocket)
        )
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        logDebug { "onMessage $webSocket, text: $text" }
        producerScope.trySendBlocking(
            WebSocketClient.Event.Data.TextMessage(text)
        )
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        logDebug { "onMessage $webSocket, bytes: $bytes" }
        producerScope.trySendBlocking(
            WebSocketClient.Event.Data.BytesMessage(bytes)
        )
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        logDebug { "onClosing $webSocket, code: $code, reason: $reason" }
        producerScope.trySendBlocking(
            WebSocketClient.Event.State.Closing(webSocket, code, reason)
        )
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        logDebug { "onClosed $webSocket, code: $code, reason: $reason" }
        producerScope.trySendBlocking(
            WebSocketClient.Event.State.Closed(webSocket, code, reason)
        )
        producerScope.close()
    }

    override fun onFailure(
        webSocket: WebSocket,
        t: Throwable,
        response: Response?
    ) {
        logError(t) { "onFailure $webSocket, response: $response" }
        producerScope.trySendBlocking(WebSocketClient.Event.State.Failed(webSocket, t))
        producerScope.close(t)
    }
}

private sealed class Action(val webSocket: WebSocket) {

    abstract fun execute()

    class SendText(webSocket: WebSocket, val text: String) : Action(webSocket) {
        override fun execute() {
            logDebug { "send text: $text via $webSocket" }
            webSocket.send(text)
        }
    }

    class SendBytes(webSocket: WebSocket, val bytes: ByteString) : Action(webSocket) {
        override fun execute() {
            logDebug { "send bytes: $bytes via $webSocket" }
            webSocket.send(bytes)
        }
    }

    class Disconnect(webSocket: WebSocket, val code: Int, val reason: String?) :
        Action(webSocket) {
        override fun execute() {
            logDebug { "disconnect code: $code, reason: $reason from $webSocket" }
            webSocket.close(code, reason)
        }
    }
}

private inline fun logDebug(msg: () -> String) {
    if (BuildConfig.DEBUG) {
        val logText = msg() + " thread: ${Thread.currentThread()}"
        Log.d(TAG, logText)
    }
}

private inline fun logError(throwable: Throwable? = null, msg: () -> String) {
    if (BuildConfig.DEBUG) {
        val logText = msg() + " thread: ${Thread.currentThread()}"
        Log.e(TAG, logText, throwable)
    }
}
