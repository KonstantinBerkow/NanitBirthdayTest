package io.github.konstantinberkow.nanitbirthdaytest.dependencies

import android.util.Log
import io.github.konstantinberkow.nanitbirthdaytest.BuildConfig
import io.github.konstantinberkow.nanitbirthdaytest.network.OkHttpWebSocketClient
import io.github.konstantinberkow.nanitbirthdaytest.network.WebSocketClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class AppDependencies {

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
