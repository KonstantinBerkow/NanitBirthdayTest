package io.github.konstantinberkow.nanitbirthdaytest.network

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import java.net.InetSocketAddress

class OkHttpWebSocketClient(
    private val okHttpClientProvider: () -> OkHttpClient
) : WebSocketClient {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun subscribe(
        webSocketUrl: String,
        input: Flow<WebSocketClient.Command>
    ) {
        val webSocketEvents = input
            .filter { it is WebSocketClient.Command.Connect }
            .flatMapLatest { _ ->
                callbackFlow<WebSocketClient.Event> {
                    val listener = object : WebSocketListener() {}
                    val request = Request.Builder()
                        .url(webSocketUrl)
                        .build()
                    val webSocket = okHttpClientProvider().newWebSocket(request, listener)

                    awaitClose {
                        // TODO: close socket
                    }
                }
            }
    }
}
