package com.dreamteam.bookworm.teacher.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.Eyebrow

/** Scrolling page body next to the sidebar, with the canvas' 32/40 padding. */
@Composable
fun TeacherPage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BookWormColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        content = content,
    )
}

/** Page title with optional eyebrow, subtitle and actions on the right. */
@Composable
fun TeacherHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (eyebrow != null) Eyebrow(eyebrow)
            Text(title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted, modifier = Modifier.widthIn(max = 640.dp))
            }
        }
        if (actions != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/** White card holding one section of a page ("Where should the cards go?"). */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BookWormCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp)) {
        SectionTitle(title)
        content()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.semantics { heading() })
}

/** Coloured note with a leading icon (tips, warnings, success messages). */
@Composable
fun Callout(
    text: String,
    icon: ImageVector,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    fontSize: Int = 15,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp, lineHeight = (fontSize * 1.45f).sp), color = contentColor)
    }
}

/**
 * Two columns side by side on a wide window, stacked when narrow, like the
 * canvas' auto-fit grids. [start] gets [startWeight] of the width.
 */
@Composable
fun TwoPane(
    modifier: Modifier = Modifier,
    minWidthForRow: Dp = 960.dp,
    startWeight: Float = 1f,
    endWeight: Float = 1f,
    gap: Dp = 28.dp,
    start: @Composable ColumnScope.() -> Unit,
    end: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth >= minWidthForRow) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(startWeight), verticalArrangement = Arrangement.spacedBy(20.dp), content = start)
                Column(Modifier.weight(endWeight), verticalArrangement = Arrangement.spacedBy(20.dp), content = end)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp), content = start)
                Column(verticalArrangement = Arrangement.spacedBy(20.dp), content = end)
            }
        }
    }
}

/** The calm countdown ring: a soft arc that fills clockwise as time passes. */
@Composable
fun CalmRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(900))
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val radius = size.minDimension / 2f - stroke / 2f
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)
        drawCircle(Color.White, radius, center)
        drawCircle(TeacherColors.CalmTrack, radius, center, style = Stroke(stroke))
        if (animated > 0f) {
            drawArc(
                TeacherColors.Calm,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}
