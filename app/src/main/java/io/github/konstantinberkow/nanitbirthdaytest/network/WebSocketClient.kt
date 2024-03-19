package io.github.konstantinberkow.nanitbirthdaytest.network

import kotlinx.coroutines.flow.Flow
import okhttp3.WebSocket
import okio.ByteString

interface WebSocketClient {

    suspend fun subscribe(
        input: Flow<Command>,
        textHandler: suspend (String) -> Unit = Empty,
        bytesHandler: suspend (ByteString) -> Unit = Empty,
    )

    private object Empty : suspend (Any) -> Unit {

        override suspend fun invoke(ignored: Any) {
        }
    }

    sealed interface Command {

        data class Connect(
            val webSocketUrl: String
        ) : Command

        data class SendText(
            val text: String
        ) : Command

        data class SendBytes(
            val bytes: ByteString
        ) : Command

        data class Disconnect(
            val code: Int,
            val reason: String?
        ) : Command
    }

    sealed interface Event {

        sealed interface State : Event {

            val webSocket: WebSocket

            data class Connected(
                override val webSocket: WebSocket
            ) : State

            data class Closing(
                override val webSocket: WebSocket,
                val code: Int,
                val reason: String
            ) : State

            data class Closed(
                override val webSocket: WebSocket,
                val code: Int,
                val reason: String
            ) : State

            data class Failed(
                override val webSocket: WebSocket,
                val throwable: Throwable
            ) : State
        }

        sealed interface Data : Event {

            data class TextMessage(val text: String) : Data

            data class BytesMessage(val bytes: ByteString) : Data
        }
    }
}
