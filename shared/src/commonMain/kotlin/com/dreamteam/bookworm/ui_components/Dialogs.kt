package com.dreamteam.bookworm.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Centered popup in the BookWorm style: optional illustration on top,
 * title, message and a stacked confirm / dismiss pair.
 */
@Composable
fun BookWormDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
    confirmVariant: ButtonVariant = ButtonVariant.Primary,
    icon: @Composable (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(BookWormColors.Background)
                .border(2.dp, BookWormColors.Border, RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            icon?.invoke()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = BookWormColors.Ink, textAlign = TextAlign.Center)
                if (message != null) {
                    Text(message, style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted, textAlign = TextAlign.Center)
                }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BookWormButton(confirmText, onConfirm, Modifier.fillMaxWidth(), variant = confirmVariant)
                if (dismissText != null) {
                    BookWormButton(dismissText, onDismiss, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
                }
            }
        }
    }
}

enum class FeedbackTone { Correct, Wrong }

/**
 * The answer-check panel that replaces the footer after "Check":
 * green "Nice one!" or red "Not quite", with an explanation and the next action.
 */
@Composable
fun FeedbackPanel(
    tone: FeedbackTone,
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val correct = tone == FeedbackTone.Correct
    val background = if (correct) BookWormColors.SuccessSoft else BookWormColors.DangerSoft
    val accent = if (correct) BookWormColors.Success else BookWormColors.Danger
    val text = if (correct) BookWormColors.SuccessDark else BookWormColors.DangerDark

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .topBorder(3.dp, accent)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (correct) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                contentDescription = null,
                tint = text,
                modifier = Modifier.size(40.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = text)
                Text(message, style = MaterialTheme.typography.bodyLarge, color = text)
            }
        }
        BookWormButton(
            actionText,
            onAction,
            Modifier.fillMaxWidth(),
            variant = if (correct) ButtonVariant.Success else ButtonVariant.Danger,
        )
    }
}

/** White footer bar holding the main action, with the cream divider on top. */
@Composable
fun ActionFooter(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BookWormColors.Surface)
            .topBorder(2.dp, BookWormColors.Divider)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
    ) {
        content()
    }
}

private fun Modifier.topBorder(width: Dp, color: Color) = drawBehind {
    drawRect(color, topLeft = Offset.Zero, size = size.copy(height = width.toPx()))
}
