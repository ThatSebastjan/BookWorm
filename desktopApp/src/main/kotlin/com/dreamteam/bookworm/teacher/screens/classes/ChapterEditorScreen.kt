package com.dreamteam.bookworm.teacher.screens.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.teacher.components.Callout
import com.dreamteam.bookworm.teacher.components.SectionCard
import com.dreamteam.bookworm.teacher.components.TeacherHeader
import com.dreamteam.bookworm.teacher.components.TeacherPage
import com.dreamteam.bookworm.teacher.components.TwoPane
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.chapter_add_card
import com.dreamteam.bookworm.teacher.resources.chapter_add_note
import com.dreamteam.bookworm.teacher.resources.chapter_back
import com.dreamteam.bookworm.teacher.resources.chapter_card
import com.dreamteam.bookworm.teacher.resources.chapter_card_definition
import com.dreamteam.bookworm.teacher.resources.chapter_card_term
import com.dreamteam.bookworm.teacher.resources.chapter_cards
import com.dreamteam.bookworm.teacher.resources.chapter_cards_hint
import com.dreamteam.bookworm.teacher.resources.chapter_eyebrow
import com.dreamteam.bookworm.teacher.resources.chapter_fix_fields
import com.dreamteam.bookworm.teacher.resources.chapter_leave_body
import com.dreamteam.bookworm.teacher.resources.chapter_leave_confirm
import com.dreamteam.bookworm.teacher.resources.chapter_leave_title
import com.dreamteam.bookworm.teacher.resources.chapter_move_down
import com.dreamteam.bookworm.teacher.resources.chapter_move_up
import com.dreamteam.bookworm.teacher.resources.chapter_no_cards
import com.dreamteam.bookworm.teacher.resources.chapter_no_notes
import com.dreamteam.bookworm.teacher.resources.chapter_note_body
import com.dreamteam.bookworm.teacher.resources.chapter_note_heading
import com.dreamteam.bookworm.teacher.resources.chapter_note_remember
import com.dreamteam.bookworm.teacher.resources.chapter_notes
import com.dreamteam.bookworm.teacher.resources.chapter_notes_hint
import com.dreamteam.bookworm.teacher.resources.chapter_remove
import com.dreamteam.bookworm.teacher.resources.chapter_required
import com.dreamteam.bookworm.teacher.resources.chapter_save
import com.dreamteam.bookworm.teacher.resources.chapter_save_failed
import com.dreamteam.bookworm.teacher.resources.chapter_saved
import com.dreamteam.bookworm.teacher.resources.chapter_section
import com.dreamteam.bookworm.teacher.resources.chapter_title
import com.dreamteam.bookworm.teacher.resources.teacher_cancel
import com.dreamteam.bookworm.ui.PlatformBackHandler
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormDialog
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.RoundIconButton
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.stringResource

/** Writes one chapter by hand: its title, note sections and flashcards. */
@Composable
fun ChapterEditorScreen(chapterId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ChapterEditorViewModel(chapterId, container.classesRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val leave = { if (viewModel.requestLeave()) onBack() }

    PlatformBackHandler { leave() }

    StateContent(state.loaded, onRetry = viewModel::load) { _ ->
        val draft = state.draft ?: return@StateContent
        val required = stringResource(Res.string.chapter_required)
        fun missing(value: String?) = if (state.showErrors && value.isNullOrBlank()) required else null

        TeacherPage {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.chapter_back), leave, tint = BookWormColors.PrimaryDark)
                Text(stringResource(Res.string.chapter_back), style = MaterialTheme.typography.labelMedium, color = BookWormColors.PrimaryDark)
            }
            TeacherHeader(
                eyebrow = stringResource(Res.string.chapter_eyebrow, state.className, draft.number),
                title = draft.title.ifBlank { " " },
                actions = { BookWormButton(stringResource(Res.string.chapter_save), viewModel::save, enabled = state.canSave) },
            )
            when {
                state.showErrors -> Message(stringResource(Res.string.chapter_fix_fields), error = true)
                state.saveFailed -> Message(stringResource(Res.string.chapter_save_failed), error = true)
                state.justSaved -> Message(stringResource(Res.string.chapter_saved), error = false)
            }
            BookWormTextField(
                value = draft.title,
                onValueChange = viewModel::setTitle,
                label = stringResource(Res.string.chapter_title),
                modifier = Modifier.fillMaxWidth(),
                errorText = missing(draft.title),
            )
            TwoPane(
                startWeight = 1.3f,
                start = {
                    SectionCard(stringResource(Res.string.chapter_notes)) {
                        Text(stringResource(Res.string.chapter_notes_hint), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                        if (draft.notes.isEmpty()) {
                            Text(stringResource(Res.string.chapter_no_notes), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                        }
                        draft.notes.forEachIndexed { index, note ->
                            val label = stringResource(Res.string.chapter_section, index + 1)
                            ItemBox(label, index, draft.notes.lastIndex, onMove = { viewModel.moveNote(index, it) }, onRemove = { viewModel.removeNote(index) }) {
                                BookWormTextField(
                                    note.heading, { viewModel.updateNote(index, note.copy(heading = it)) }, Modifier.fillMaxWidth(),
                                    label = stringResource(Res.string.chapter_note_heading),
                                    errorText = missing(note.heading),
                                )
                                BookWormTextField(
                                    note.body, { viewModel.updateNote(index, note.copy(body = it)) }, Modifier.fillMaxWidth(),
                                    label = stringResource(Res.string.chapter_note_body),
                                    singleLine = false,
                                    minLines = 4,
                                    errorText = missing(note.body),
                                )
                                BookWormTextField(
                                    note.remember.orEmpty(), { viewModel.updateNote(index, note.copy(remember = it)) }, Modifier.fillMaxWidth(),
                                    label = stringResource(Res.string.chapter_note_remember),
                                    singleLine = false,
                                    minLines = 2,
                                )
                            }
                        }
                        SmallButton(stringResource(Res.string.chapter_add_note), viewModel::addNote, variant = ButtonVariant.Secondary, leadingIcon = Icons.Rounded.Add)
                    }
                },
                end = {
                    SectionCard(stringResource(Res.string.chapter_cards)) {
                        Text(stringResource(Res.string.chapter_cards_hint), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                        if (draft.flashcards.isEmpty()) {
                            Text(stringResource(Res.string.chapter_no_cards), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                        }
                        draft.flashcards.forEachIndexed { index, card ->
                            val label = stringResource(Res.string.chapter_card, index + 1)
                            ItemBox(label, index, draft.flashcards.lastIndex, onMove = { viewModel.moveCard(index, it) }, onRemove = { viewModel.removeCard(index) }) {
                                BookWormTextField(
                                    card.term, { viewModel.updateCard(index, card.copy(term = it)) }, Modifier.fillMaxWidth(),
                                    label = stringResource(Res.string.chapter_card_term),
                                    errorText = missing(card.term),
                                )
                                BookWormTextField(
                                    card.definition, { viewModel.updateCard(index, card.copy(definition = it)) }, Modifier.fillMaxWidth(),
                                    label = stringResource(Res.string.chapter_card_definition),
                                    singleLine = false,
                                    minLines = 2,
                                    errorText = missing(card.definition),
                                )
                            }
                        }
                        SmallButton(stringResource(Res.string.chapter_add_card), viewModel::addCard, variant = ButtonVariant.Secondary, leadingIcon = Icons.Rounded.Add)
                    }
                },
            )
        }
    }

    if (state.confirmLeave) {
        BookWormDialog(
            title = stringResource(Res.string.chapter_leave_title),
            message = stringResource(Res.string.chapter_leave_body),
            onDismissRequest = viewModel::stay,
            confirmText = stringResource(Res.string.teacher_cancel),
            onConfirm = viewModel::stay,
            dismissText = stringResource(Res.string.chapter_leave_confirm),
            onDismiss = {
                viewModel.stay()
                onBack()
            },
        )
    }
}

/** One note section or flashcard: a numbered label with move and remove buttons above its fields. */
@Composable
private fun ItemBox(
    label: String,
    index: Int,
    lastIndex: Int,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    fields: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BookWormColors.Background).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
            RoundIconButton(Icons.Rounded.KeyboardArrowUp, stringResource(Res.string.chapter_move_up, label), { onMove(-1) }, enabled = index > 0)
            RoundIconButton(Icons.Rounded.KeyboardArrowDown, stringResource(Res.string.chapter_move_down, label), { onMove(1) }, enabled = index < lastIndex)
            RoundIconButton(Icons.Outlined.Delete, stringResource(Res.string.chapter_remove, label), onRemove)
        }
        fields()
    }
}

@Composable
private fun Message(text: String, error: Boolean) {
    Callout(
        text = text,
        icon = if (error) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
        background = if (error) BookWormColors.DangerSoft else BookWormColors.SuccessSoft,
        contentColor = if (error) BookWormColors.DangerDark else BookWormColors.SuccessDark,
    )
}
