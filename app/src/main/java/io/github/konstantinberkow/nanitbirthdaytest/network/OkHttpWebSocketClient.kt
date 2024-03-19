package io.github.konstantinberkow.nanitbirthdaytest.network

import android.util.Log
import io.github.konstantinberkow.nanitbirthdaytest.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

private const val TAG = "OkHttpWebSocketClient"

class OkHttpWebSocketClient(
    private val okHttpClientProvider: () -> OkHttpClient
) : WebSocketClient {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun subscribe(
        input: Flow<WebSocketClient.Command>,
        textHandler: suspend (String) -> Unit,
        bytesHandler: suspend (ByteString) -> Unit,
    ) {
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

        webSocketEvents.collect { event ->
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
