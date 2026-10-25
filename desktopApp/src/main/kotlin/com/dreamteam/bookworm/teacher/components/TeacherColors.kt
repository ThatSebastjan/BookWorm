package com.dreamteam.bookworm.teacher.components

import androidx.compose.ui.graphics.Color

/** Colours only the teacher app uses, on top of [com.dreamteam.bookworm.ui_components.BookWormColors]. */
object TeacherColors {
    /** Dark text on the sun-coloured callouts and chips. */
    val SunInk = Color(0xFF7A4B00)
    val SunInkDark = Color(0xFF5C3A00)

    /** Muted bar for wrong answers on the pop quiz results screen. */
    val PrimaryMuted = Color(0xFF9C95D6)
    val ResultTrack = Color(0xFFF1EBDA)

    /** The cool, quiet palette of the live pop quiz screen. */
    val CalmBackground = Color(0xFFF3F8FB)
    val CalmDivider = Color(0xFFE1ECF2)
    val CalmBorder = Color(0xFFC5D5DF)
    val CalmTrack = Color(0xFFDCEAF1)
    val Calm = Color(0xFF3E9BB5)
    val CalmDark = Color(0xFF17566A)
    val CalmInk = Color(0xFF2F3B5C)
}
