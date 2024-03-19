package io.github.konstantinberkow.nanitbirthdaytest

import android.app.Application
import io.github.konstantinberkow.nanitbirthdaytest.dependencies.AppDependencies

class NanitClientApp : Application() {

    val dependencies by lazy { AppDependencies() }
}
