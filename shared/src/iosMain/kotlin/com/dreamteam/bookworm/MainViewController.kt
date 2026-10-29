package com.dreamteam.bookworm

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.dreamteam.bookworm.data.remote.KeychainSessionStore
import com.dreamteam.bookworm.di.AppContainer

fun MainViewController() = ComposeUIViewController {
    App(remember { AppContainer.create(sessionStore = KeychainSessionStore()) })
}
