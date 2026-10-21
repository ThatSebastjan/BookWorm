package com.dreamteam.bookworm.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ButtonVariant { Primary, Success, Danger, Secondary }

private data class ButtonColors(val face: Color, val ledge: Color, val content: Color, val border: Color?)

private fun ButtonVariant.colors(enabled: Boolean): ButtonColors = when {
    !enabled -> ButtonColors(BookWormColors.Track, BookWormColors.Track, BookWormColors.InkMuted, null)
    this == ButtonVariant.Primary -> ButtonColors(BookWormColors.Primary, BookWormColors.PrimaryDark, Color.White, null)
    this == ButtonVariant.Success -> ButtonColors(BookWormColors.Success, BookWormColors.SuccessDark, Color.White, null)
    this == ButtonVariant.Danger -> ButtonColors(BookWormColors.Danger, BookWormColors.DangerDark, Color.White, null)
    else -> ButtonColors(BookWormColors.Surface, BookWormColors.Border, BookWormColors.Ink, BookWormColors.Border)
}

/** Big call-to-action button ("Check", "Continue", "Try again", "Start the quiz"). */
@Composable
fun BookWormButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = variant.colors(enabled)
    LedgeSurface(
        color = colors.face,
        ledgeColor = colors.ledge,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.heightIn(min = 61.dp),
        onClick = onClick,
        enabled = enabled,
        borderColor = colors.border,
        ledge = if (enabled) 5.dp else 0.dp,
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = colors.content, modifier = Modifier.size(24.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = colors.content)
        }
    }
}

/** Compact button, e.g. the "Notes" button on the chapter header. */
@Composable
fun SmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = variant.colors(enabled)
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.face)
            .then(if (colors.border != null) Modifier.border(2.dp, colors.border, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = colors.content, modifier = Modifier.size(20.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = colors.content)
    }
}

/** Round 44dp icon-only button (close, back, etc.). */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = BookWormColors.InkMuted,
    background: Color = Color.Transparent,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(26.dp))
    }
}

/** Pill toggle used for subject filters ("Maths", "Nature", "Language"). */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) BookWormColors.Primary else BookWormColors.Surface)
            .border(2.dp, if (selected) BookWormColors.Primary else BookWormColors.Border, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else BookWormColors.Ink,
        )
    }
}

/** Segmented-style value picker ("5", "8", "10", "3 min"). */
@Composable
fun ValueToggle(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .widthIn(min = 88.dp)
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (selected) BookWormColors.PrimarySoft else BookWormColors.Surface)
            .border(3.dp, if (selected) BookWormColors.Primary else BookWormColors.Border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = BookWormColors.Ink)
    }
}

enum class AnswerState { Idle, Selected, Correct, Wrong }

/** Answer tile for quiz questions; colours follow selection and checked result. */
@Composable
fun AnswerOption(
    state: AnswerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val (face, edge) = when (state) {
        AnswerState.Idle -> BookWormColors.Surface to BookWormColors.Border
        AnswerState.Selected -> BookWormColors.PrimarySoft to BookWormColors.Primary
        AnswerState.Correct -> BookWormColors.SuccessSoft to BookWormColors.Success
        AnswerState.Wrong -> BookWormColors.DangerSoft to BookWormColors.Danger
    }
    LedgeSurface(
        color = face,
        ledgeColor = edge,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .heightIn(min = 64.dp)
            .semantics { selected = state != AnswerState.Idle },
        onClick = onClick,
        enabled = enabled,
        role = Role.RadioButton,
        borderColor = edge,
        borderWidth = 3.dp,
        ledge = 4.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        content()
    }
}
