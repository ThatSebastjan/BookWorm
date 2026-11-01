package com.dreamteam.bookworm.teacher.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.dreamteam.bookworm.domain.model.PickedFile

/**
 * What the teacher app needs from the computer it runs on. The desktop app
 * passes a real implementation to TeacherApp; the defaults do nothing.
 */
interface TeacherPlatform {
    /** Opens the system file chooser. Returns nothing when the teacher cancels. */
    suspend fun pickFiles(): List<PickedFile> = emptyList()

    /** Lets files be dropped onto the element this modifier is applied to. */
    @Composable
    fun fileDropTarget(onDrop: (List<PickedFile>) -> Unit): Modifier = Modifier

    /** Puts [text] on the clipboard. Returns false when the platform can't. */
    fun copyToClipboard(text: String): Boolean = false

    /** Sends a page with the join code to the printer. */
    fun printJoinCode(className: String, code: String, link: String) = Unit
}

object NoTeacherPlatform : TeacherPlatform

val LocalTeacherPlatform = staticCompositionLocalOf<TeacherPlatform> { NoTeacherPlatform }
