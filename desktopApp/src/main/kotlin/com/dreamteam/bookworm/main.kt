package com.dreamteam.bookworm

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dreamteam.bookworm.teacher.TeacherApp
import com.dreamteam.bookworm.teacher.platform.DesktopTeacherPlatform
import java.awt.Dimension

/** The teacher desktop app. Students use the Android and iOS apps. */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BookWorm",
        state = rememberWindowState(size = DpSize(1280.dp, 860.dp)),
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(1024, 680) }
        val platform = remember(window) { DesktopTeacherPlatform(window) }
        TeacherApp(platform = platform)
    }
}
