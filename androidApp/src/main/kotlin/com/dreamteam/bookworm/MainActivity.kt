package com.dreamteam.bookworm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.dreamteam.bookworm.data.remote.AndroidSessionStore
import com.dreamteam.bookworm.di.AppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(remember { AppContainer.create(sessionStore = AndroidSessionStore(applicationContext)) })
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}