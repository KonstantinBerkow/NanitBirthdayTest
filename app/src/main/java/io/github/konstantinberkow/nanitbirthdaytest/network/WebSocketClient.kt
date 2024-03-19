package io.github.konstantinberkow.nanitbirthdaytest.network

import kotlinx.coroutines.flow.Flow
import okio.ByteString

interface WebSocketClient {

    suspend fun subscribe(input: Flow<Command>)

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

        data object Connected : Event

        data class Closing(
            val code: Int,
            val reason: String
        ) : Event

        data class Closed(
            val code: Int,
            val reason: String
        ) : Event

        data class Failed(
            val throwable: Throwable
        ) : Event

        data class TextMessage(val text: String) : Event

        data class BytesMessage(val bytes: ByteString) : Event
    }
}
