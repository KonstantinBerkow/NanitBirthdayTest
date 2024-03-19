package io.github.konstantinberkow.nanitbirthdaytest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.konstantinberkow.nanitbirthdaytest.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }
}