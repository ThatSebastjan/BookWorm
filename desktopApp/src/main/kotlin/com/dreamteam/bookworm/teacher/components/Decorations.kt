package com.dreamteam.bookworm.teacher.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dreamteam.bookworm.domain.model.Fraction
import com.dreamteam.bookworm.ui_components.BookWormColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/* Drawing helpers for the teacher app. */

/** A circle cut into equal slices with some shaded, picturing [fraction]. */
@Composable
fun FractionPie(
    fraction: Fraction,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fill: Color = BookWormColors.Sun,
) {
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val stroke = size.minDimension * 0.03f
        val radius = size.minDimension / 2f - stroke
        val parts = fraction.denominator.coerceAtLeast(1)
        val sweep = 360f / parts
        val topLeft = Offset(center.x - radius, center.y - radius)
        val box = Size(radius * 2, radius * 2)

        drawCircle(Color.White, radius, center)
        repeat(fraction.numerator.coerceIn(0, parts)) { i ->
            drawArc(fill, startAngle = -90f + i * sweep, sweepAngle = sweep, useCenter = true, topLeft = topLeft, size = box)
        }
        if (parts > 1) {
            repeat(parts) { i ->
                val angle = (-90.0 + i * sweep) * PI / 180.0
                val end = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
                drawLine(BookWormColors.Ink, center, end, strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
        drawCircle(BookWormColors.Ink, radius, center, style = Stroke(stroke))
    }
}

/** Dashed rounded outline, as on the canvas drop zone and placeholders. */
fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 3.dp,
    cornerRadius: Dp = 28.dp,
    dash: Dp = 10.dp,
    gap: Dp = 8.dp,
) = drawBehind {
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))),
    )
}
