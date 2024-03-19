package io.github.konstantinberkow.nanitbirthdaytest.dependencies

import android.util.Log
import io.github.konstantinberkow.nanitbirthdaytest.BuildConfig
import io.github.konstantinberkow.nanitbirthdaytest.network.OkHttpWebSocketClient
import io.github.konstantinberkow.nanitbirthdaytest.network.WebSocketClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

private const val ADDRESS_SCHEMA = "http://"
private const val ADDRESS_PATH = "/nanit"

class AppDependencies {

    val transformAddressFromInput: (String) -> String = { rawAddress ->
        val shouldAddProtocol = !rawAddress.startsWith(ADDRESS_SCHEMA)
        val shouldAddPath = !rawAddress.endsWith(ADDRESS_PATH)

        buildString {
            if (shouldAddProtocol) {
                append(ADDRESS_SCHEMA)
            }
            append(rawAddress)
            if (shouldAddPath) {
                append(ADDRESS_PATH)
            }
        }
    }

    val okHttpClientFactory = { tag: String ->
        val builder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            val logger = HttpLoggingInterceptor.Logger {
                Log.d(tag, it)
            }
            val loggingInterceptor = HttpLoggingInterceptor(logger).apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    val webSocketOkHttpClientFactory: () -> WebSocketClient = {
        val wrappedFactory = { okHttpClientFactory("WebSocketOkHttpClient") }
        OkHttpWebSocketClient(
            okHttpClientProvider = wrappedFactory
        )
    }
}
