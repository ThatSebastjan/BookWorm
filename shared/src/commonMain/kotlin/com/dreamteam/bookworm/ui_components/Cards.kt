package com.dreamteam.bookworm.ui_components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * White rounded card with the cream border. Pass [onClick] to make it a
 * tappable list card with the ledge shadow (chapter and class rows).
 */
@Composable
fun BookWormCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    LedgeSurface(
        color = BookWormColors.Surface,
        ledgeColor = BookWormColors.Border,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier,
        onClick = onClick,
        borderColor = BookWormColors.Border,
        ledge = if (onClick != null) 4.dp else 0.dp,
        contentPadding = contentPadding,
        contentAlignment = Alignment.TopStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Dashed placeholder card, e.g. the locked "Next chapter" teaser. */
@Composable
fun LockedCard(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, BookWormColors.Outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Eyebrow(eyebrow, color = BookWormColors.InkMuted)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = BookWormColors.InkMuted)
        }
        trailing?.invoke()
    }
}

/** Small uppercase label above a heading ("CHAPTER 3 · 2 OF 6 DONE"). */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BookWormColors.PrimaryDark,
) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** Rounded square tile holding a number or initial (chapter number, class initial). */
@Composable
fun BadgeTile(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = BookWormColors.PrimarySoft,
    contentColor: Color = BookWormColors.PrimaryDark,
    size: Dp = 52.dp,
) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(16.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.headlineSmall, color = contentColor)
    }
}

/** Header pill showing a count with an icon (streak, stars). */
@Composable
fun StatPill(
    icon: ImageVector,
    value: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconTint: Color = BookWormColors.Sun,
    background: Color = BookWormColors.SunSoft,
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .padding(horizontal = 12.dp)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp), color = BookWormColors.Ink)
    }
}

/** Stat block for profile totals ("6 Day streak"). */
@Composable
fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: Color = BookWormColors.Sun,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BookWormColors.Surface)
            .border(2.dp, BookWormColors.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 6.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 26.sp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
    }
}

/** Rounded progress bar (lesson progress, level progress, chapter mastery). */
@Composable
fun BookWormProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = BookWormColors.Success,
    trackColor: Color = BookWormColors.Track,
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(color),
        )
    }
}
