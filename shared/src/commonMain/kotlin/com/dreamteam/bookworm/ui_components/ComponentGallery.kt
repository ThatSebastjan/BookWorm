package com.dreamteam.bookworm.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Every shared component on one scrollable screen, for previews and quick checks. */
@Composable
@Preview
fun ComponentGallery() {
    BookWormTheme {
        var subject by remember { mutableStateOf("Maths") }
        var count by remember { mutableStateOf(8) }
        var answer by remember { mutableStateOf<String?>(null) }
        var text by remember { mutableStateOf("") }
        var calm by remember { mutableStateOf(true) }
        var showDialog by remember { mutableStateOf(false) }

        Column(
            Modifier
                .fillMaxSize()
                .background(BookWormColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Components", style = MaterialTheme.typography.headlineLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(Icons.Rounded.LocalFireDepartment, "6", "6 day streak", iconTint = BookWormColors.Flame)
                StatPill(Icons.Rounded.Star, "428", "428 stars")
                RoundIconButton(Icons.Outlined.Close, "Close", onClick = {})
            }

            BookWormButton("Check", onClick = { showDialog = true }, Modifier.fillMaxWidth())
            BookWormButton("Check", onClick = {}, Modifier.fillMaxWidth(), enabled = false)
            BookWormButton("Start the quiz", onClick = {}, Modifier.fillMaxWidth(), variant = ButtonVariant.Success, leadingIcon = Icons.Rounded.PlayArrow)
            BookWormButton("Maybe later", onClick = {}, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
            SmallButton("Notes", onClick = {}, leadingIcon = Icons.AutoMirrored.Outlined.MenuBook)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Maths", "Nature", "Language").forEach {
                    ChoiceChip(it, selected = subject == it, onClick = { subject = it })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(5, 8, 10).forEach {
                    ValueToggle("$it", selected = count == it, onClick = { count = it })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("1/2" to AnswerState.Correct, "1/4" to AnswerState.Wrong, "1/8" to null).forEach { (label, checked) ->
                    val state = checked ?: if (answer == label) AnswerState.Selected else AnswerState.Idle
                    AnswerOption(state, onClick = { answer = label }, Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            BookWormCard(Modifier.fillMaxWidth(), onClick = {}) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BadgeTile("3")
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Fractions", style = MaterialTheme.typography.titleMedium)
                        BookWormProgressBar(5 / 12f)
                        Text("5 of 12 known", style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
                    }
                }
            }
            LockedCard("Next · Chapter 4", "Telling the time", Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Lock, contentDescription = "Locked", tint = BookWormColors.InkMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Rounded.LocalFireDepartment, "6", "Day streak", Modifier.weight(1f), iconTint = BookWormColors.Flame)
                StatTile(Icons.Rounded.Star, "428", "Stars earned", Modifier.weight(1f))
                StatTile(Icons.Rounded.Check, "41", "Lessons done", Modifier.weight(1f), iconTint = BookWormColors.Success)
            }

            BookWormTextField(text, { text = it }, Modifier.fillMaxWidth(), label = "Your answer", placeholder = "Type here")
            LabeledCheckbox("Soft chime when time is up", calm, { calm = it })

            FeedbackPanel(FeedbackTone.Correct, "Nice one! +10 stars", "Eighths are the smallest here.", "Continue", onAction = {})
            FeedbackPanel(FeedbackTone.Wrong, "Not quite", "Which bottom number is biggest?", "Try again", onAction = {})
        }

        if (showDialog) {
            BookWormDialog(
                title = "Leave the lesson?",
                message = "You'll lose the stars from this lesson.",
                onDismissRequest = { showDialog = false },
                confirmText = "Keep learning",
                onConfirm = { showDialog = false },
                dismissText = "Leave",
            )
        }
    }
}
