package com.dreamteam.bookworm.ui_components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** The BookWorm mascot from the canvas logo, drawn on a 64-unit grid. Decorative. */
@Composable
fun MascotIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 64f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)

        val leaf = Path().apply {
            moveTo(32 * u, 12 * u)
            cubicTo(32 * u, 6 * u, 37 * u, 3 * u, 41 * u, 4 * u)
            cubicTo(40 * u, 9 * u, 37 * u, 12 * u, 32 * u, 12 * u)
            close()
        }
        drawPath(leaf, BookWormColors.Success)
        drawOval(BookWormColors.Sun, topLeft = p(6f, 15f), size = Size(52 * u, 46 * u))
        drawCircle(BookWormColors.Ink, radius = 3.2f * u, center = p(23f, 36f))
        drawCircle(BookWormColors.Ink, radius = 3.2f * u, center = p(41f, 36f))
        drawCircle(Color(0xFFF28B6E), radius = 3.5f * u, center = p(16f, 43f), alpha = 0.6f)
        drawCircle(Color(0xFFF28B6E), radius = 3.5f * u, center = p(48f, 43f), alpha = 0.6f)
        val smile = Path().apply {
            moveTo(26 * u, 44 * u)
            quadraticTo(32 * u, 50 * u, 38 * u, 44 * u)
        }
        drawPath(smile, BookWormColors.Ink, style = Stroke(width = 2.5f * u, cap = StrokeCap.Round))
    }
}
