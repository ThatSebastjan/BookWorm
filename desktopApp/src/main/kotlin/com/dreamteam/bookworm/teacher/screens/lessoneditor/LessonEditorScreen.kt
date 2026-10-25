package com.dreamteam.bookworm.teacher.screens.lessoneditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.AuthoredQuestion
import com.dreamteam.bookworm.domain.model.LessonContent
import com.dreamteam.bookworm.domain.model.Question
import com.dreamteam.bookworm.teacher.components.BookWormDropdown
import com.dreamteam.bookworm.teacher.components.Callout
import com.dreamteam.bookworm.teacher.components.SectionCard
import com.dreamteam.bookworm.teacher.components.TeacherColors
import com.dreamteam.bookworm.teacher.components.TeacherHeader
import com.dreamteam.bookworm.teacher.components.TeacherPage
import com.dreamteam.bookworm.teacher.components.dashedBorder
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.lesson_type_blank
import com.dreamteam.bookworm.teacher.resources.lesson_type_choice
import com.dreamteam.bookworm.teacher.resources.lesson_type_match
import com.dreamteam.bookworm.teacher.resources.lesson_type_multi
import com.dreamteam.bookworm.teacher.resources.lesson_type_order
import com.dreamteam.bookworm.teacher.resources.lesson_type_typed
import com.dreamteam.bookworm.teacher.resources.lessonedit_action_failed
import com.dreamteam.bookworm.teacher.resources.lessonedit_add
import com.dreamteam.bookworm.teacher.resources.lessonedit_add_item
import com.dreamteam.bookworm.teacher.resources.lessonedit_add_option
import com.dreamteam.bookworm.teacher.resources.lessonedit_add_pair
import com.dreamteam.bookworm.teacher.resources.lessonedit_add_question
import com.dreamteam.bookworm.teacher.resources.lessonedit_add_word
import com.dreamteam.bookworm.teacher.resources.lessonedit_answers_multi
import com.dreamteam.bookworm.teacher.resources.lessonedit_answers_single
import com.dreamteam.bookworm.teacher.resources.lessonedit_back
import com.dreamteam.bookworm.teacher.resources.lessonedit_correct
import com.dreamteam.bookworm.teacher.resources.lessonedit_empty_body
import com.dreamteam.bookworm.teacher.resources.lessonedit_empty_title
import com.dreamteam.bookworm.teacher.resources.lessonedit_explanation
import com.dreamteam.bookworm.teacher.resources.lessonedit_explanation_hint
import com.dreamteam.bookworm.teacher.resources.lessonedit_eyebrow
import com.dreamteam.bookworm.teacher.resources.lessonedit_feedback
import com.dreamteam.bookworm.teacher.resources.lessonedit_fraction_input
import com.dreamteam.bookworm.teacher.resources.lessonedit_hint
import com.dreamteam.bookworm.teacher.resources.lessonedit_hint_hint
import com.dreamteam.bookworm.teacher.resources.lessonedit_incomplete
import com.dreamteam.bookworm.teacher.resources.lessonedit_item
import com.dreamteam.bookworm.teacher.resources.lessonedit_items
import com.dreamteam.bookworm.teacher.resources.lessonedit_leave_body
import com.dreamteam.bookworm.teacher.resources.lessonedit_leave_confirm
import com.dreamteam.bookworm.teacher.resources.lessonedit_leave_title
import com.dreamteam.bookworm.teacher.resources.lessonedit_needs_work
import com.dreamteam.bookworm.teacher.resources.lessonedit_no_prompt
import com.dreamteam.bookworm.teacher.resources.lessonedit_no_suggestions
import com.dreamteam.bookworm.teacher.resources.lessonedit_option
import com.dreamteam.bookworm.teacher.resources.lessonedit_pair_left
import com.dreamteam.bookworm.teacher.resources.lessonedit_pair_right
import com.dreamteam.bookworm.teacher.resources.lessonedit_pairs
import com.dreamteam.bookworm.teacher.resources.lessonedit_preview_gap
import com.dreamteam.bookworm.teacher.resources.lessonedit_prompt
import com.dreamteam.bookworm.teacher.resources.lessonedit_question_count
import com.dreamteam.bookworm.teacher.resources.lessonedit_question_number
import com.dreamteam.bookworm.teacher.resources.lessonedit_remove
import com.dreamteam.bookworm.teacher.resources.lessonedit_remove_question
import com.dreamteam.bookworm.teacher.resources.lessonedit_retry
import com.dreamteam.bookworm.teacher.resources.lessonedit_retry_hint
import com.dreamteam.bookworm.teacher.resources.lessonedit_save
import com.dreamteam.bookworm.teacher.resources.lessonedit_saved
import com.dreamteam.bookworm.teacher.resources.lessonedit_suggest
import com.dreamteam.bookworm.teacher.resources.lessonedit_text_after
import com.dreamteam.bookworm.teacher.resources.lessonedit_text_before
import com.dreamteam.bookworm.teacher.resources.lessonedit_typed_answer
import com.dreamteam.bookworm.teacher.resources.lessonedit_typed_answer_hint
import com.dreamteam.bookworm.teacher.resources.lessonedit_typed_note
import com.dreamteam.bookworm.teacher.resources.lessonedit_word
import com.dreamteam.bookworm.teacher.resources.lessonedit_words
import com.dreamteam.bookworm.teacher.resources.paths_move_down
import com.dreamteam.bookworm.teacher.resources.paths_move_up
import com.dreamteam.bookworm.teacher.resources.teacher_cancel
import com.dreamteam.bookworm.ui.PlatformBackHandler
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormDialog
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.Eyebrow
import com.dreamteam.bookworm.ui_components.LabeledCheckbox
import com.dreamteam.bookworm.ui_components.LedgeSurface
import com.dreamteam.bookworm.ui_components.RoundIconButton
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Writes the questions of one lesson on a learning path, with their answer keys. */
@Composable
fun LessonEditorScreen(lessonId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LessonEditorViewModel(lessonId, container.curriculumRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val leave = { if (viewModel.requestLeave()) onBack() }

    PlatformBackHandler { leave() }

    StateContent(state.content, onRetry = viewModel::load) { content ->
        TeacherPage {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.lessonedit_back), leave, tint = BookWormColors.PrimaryDark)
                Text(stringResource(Res.string.lessonedit_back), style = MaterialTheme.typography.labelMedium, color = BookWormColors.PrimaryDark)
            }
            TeacherHeader(
                eyebrow = stringResource(Res.string.lessonedit_eyebrow, content.subjectName, content.chapter.number, content.chapter.title),
                title = content.title,
                subtitle = state.questions.size.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.lessonedit_question_count, it, it) },
                actions = {
                    BookWormButton(
                        stringResource(Res.string.lessonedit_suggest),
                        viewModel::suggest,
                        variant = ButtonVariant.Secondary,
                        enabled = !state.suggesting && content.chapter.flashcardCount >= 2,
                        leadingIcon = Icons.Outlined.AutoAwesome,
                    )
                    BookWormButton(stringResource(Res.string.lessonedit_save), viewModel::save, enabled = state.canSave)
                },
            )
            Messages(state, content)
            if (state.questions.isEmpty()) {
                EmptyLesson(content, onAdd = viewModel::add)
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 900.dp
                    val list = @Composable { modifier: Modifier -> QuestionList(state, viewModel, modifier) }
                    val editor = @Composable { modifier: Modifier ->
                        state.current?.let { QuestionEditor(it, state.selected, state.questions.size, viewModel, modifier) }
                    }
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            list(Modifier.width(320.dp))
                            editor(Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            list(Modifier.fillMaxWidth())
                            editor(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

    if (state.confirmLeave) {
        BookWormDialog(
            title = stringResource(Res.string.lessonedit_leave_title),
            message = stringResource(Res.string.lessonedit_leave_body),
            onDismissRequest = viewModel::stay,
            confirmText = stringResource(Res.string.teacher_cancel),
            onConfirm = viewModel::stay,
            dismissText = stringResource(Res.string.lessonedit_leave_confirm),
            onDismiss = {
                viewModel.stay()
                onBack()
            },
        )
    }
}

@Composable
private fun Messages(state: LessonEditorUiState, content: LessonContent) {
    if (state.justSaved) {
        Callout(stringResource(Res.string.lessonedit_saved), Icons.Outlined.CheckCircle, BookWormColors.SuccessSoft, BookWormColors.SuccessDark)
    }
    if (state.actionFailed) {
        Callout(stringResource(Res.string.lessonedit_action_failed), Icons.Outlined.ErrorOutline, BookWormColors.DangerSoft, BookWormColors.DangerDark)
    }
    if (state.noSuggestions) {
        Callout(stringResource(Res.string.lessonedit_no_suggestions, content.chapter.title), Icons.Outlined.Info, BookWormColors.PrimarySoft, BookWormColors.PrimaryDark)
    }
    if (state.dirty && state.incomplete > 0) {
        Callout(
            pluralStringResource(Res.plurals.lessonedit_incomplete, state.incomplete, state.incomplete),
            Icons.Rounded.WarningAmber,
            BookWormColors.SunSoft,
            TeacherColors.SunInkDark,
        )
    }
}

private fun QuestionKind.label(): StringResource = when (this) {
    QuestionKind.SingleChoice -> Res.string.lesson_type_choice
    QuestionKind.MultiChoice -> Res.string.lesson_type_multi
    QuestionKind.TypedAnswer -> Res.string.lesson_type_typed
    QuestionKind.FillBlank -> Res.string.lesson_type_blank
    QuestionKind.Order -> Res.string.lesson_type_order
    QuestionKind.Match -> Res.string.lesson_type_match
}

@Composable
private fun AddQuestion(onAdd: (QuestionKind) -> Unit, modifier: Modifier = Modifier) {
    var kind by remember { mutableStateOf(QuestionKind.SingleChoice) }
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BookWormDropdown(
            label = stringResource(Res.string.lessonedit_add_question),
            options = QuestionKind.entries,
            selected = kind,
            onSelect = { kind = it },
            optionLabel = { stringResource(it.label()) },
            modifier = Modifier.weight(1f),
        )
        SmallButton(stringResource(Res.string.lessonedit_add), { onAdd(kind) }, leadingIcon = Icons.Rounded.Add)
    }
}

/** No questions written: say what students get meanwhile, and offer the first question. */
@Composable
private fun EmptyLesson(content: LessonContent, onAdd: (QuestionKind) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(BookWormColors.Surface)
            .dashedBorder(BookWormColors.Outline)
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Outlined.Quiz, contentDescription = null, tint = BookWormColors.PrimaryDark, modifier = Modifier.size(36.dp))
        Text(stringResource(Res.string.lessonedit_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            pluralStringResource(Res.plurals.lessonedit_empty_body, content.chapter.flashcardCount, content.chapter.flashcardCount),
            style = MaterialTheme.typography.bodyLarge,
            color = BookWormColors.InkMuted,
            textAlign = TextAlign.Center,
        )
        AddQuestion(onAdd, Modifier.width(420.dp))
    }
}

// Question list

@Composable
private fun QuestionList(state: LessonEditorUiState, viewModel: LessonEditorViewModel, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.questions.forEachIndexed { index, question ->
            QuestionListItem(index, question, selected = index == state.selected, onClick = { viewModel.select(index) })
        }
        AddQuestion(viewModel::add, Modifier.padding(top = 6.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionListItem(index: Int, question: AuthoredQuestion, selected: Boolean, onClick: () -> Unit) {
    LedgeSurface(
        color = if (selected) BookWormColors.PrimaryTint else BookWormColors.Surface,
        ledgeColor = if (selected) BookWormColors.Primary else BookWormColors.Border,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = if (selected) BookWormColors.Primary else BookWormColors.Border,
        ledge = if (selected) 4.dp else 3.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Eyebrow(stringResource(Res.string.lessonedit_question_number, index + 1, stringResource(question.kind.label())), color = BookWormColors.InkMuted)
            Text(
                question.question.prompt.ifBlank { stringResource(Res.string.lessonedit_no_prompt) },
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, lineHeight = 21.sp),
                color = if (question.question.prompt.isBlank()) BookWormColors.InkMuted else BookWormColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!question.isComplete()) {
                Text(
                    stringResource(Res.string.lessonedit_needs_work),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = TeacherColors.SunInk,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(BookWormColors.SunSoft).padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
        }
    }
}

// Question editor

@Composable
private fun QuestionEditor(
    question: AuthoredQuestion,
    index: Int,
    count: Int,
    viewModel: LessonEditorViewModel,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.lessonedit_question_number, index + 1, stringResource(question.kind.label()))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionCard(label) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RoundIconButton(Icons.Rounded.ArrowUpward, stringResource(Res.string.paths_move_up, label), { viewModel.move(index, -1) }, enabled = index > 0)
                RoundIconButton(Icons.Rounded.ArrowDownward, stringResource(Res.string.paths_move_down, label), { viewModel.move(index, 1) }, enabled = index < count - 1)
                RoundIconButton(Icons.Outlined.Delete, stringResource(Res.string.lessonedit_remove_question), { viewModel.remove(index) })
            }
            BookWormTextField(
                value = question.question.prompt,
                onValueChange = { text -> viewModel.edit { withPrompt(text) } },
                label = stringResource(Res.string.lessonedit_prompt),
                singleLine = false,
                minLines = 2,
            )
            when (question.question) {
                is Question.SingleChoice, is Question.MultiChoice -> ChoiceEditor(question, viewModel)
                is Question.TypedAnswer -> TypedEditor(question, viewModel)
                is Question.FillBlank -> BlankEditor(question, viewModel)
                is Question.Order -> OrderEditor(question, viewModel)
                is Question.Match -> MatchEditor(question, viewModel)
            }
        }
        SectionCard(stringResource(Res.string.lessonedit_feedback)) {
            BookWormTextField(
                value = question.explanation,
                onValueChange = { text -> viewModel.edit { copy(explanation = text) } },
                label = stringResource(Res.string.lessonedit_explanation),
                placeholder = stringResource(Res.string.lessonedit_explanation_hint),
                singleLine = false,
                minLines = 2,
            )
            BookWormTextField(
                value = question.retryHint,
                onValueChange = { text -> viewModel.edit { copy(retryHint = text) } },
                label = stringResource(Res.string.lessonedit_retry),
                placeholder = stringResource(Res.string.lessonedit_retry_hint),
                singleLine = false,
                minLines = 2,
            )
            BookWormTextField(
                value = question.question.hint.orEmpty(),
                onValueChange = { text -> viewModel.edit { withHint(text) } },
                label = stringResource(Res.string.lessonedit_hint),
                placeholder = stringResource(Res.string.lessonedit_hint_hint),
            )
        }
    }
}

@Composable
private fun FieldTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = BookWormColors.Ink)
}

/** A text field row with a leading control and a remove button. */
@Composable
private fun EditableRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onRemove: (() -> Unit)?,
    removeLabel: String,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        leading()
        BookWormTextField(value = value, onValueChange = onValueChange, placeholder = placeholder, modifier = Modifier.weight(1f))
        trailing()
        RoundIconButton(Icons.Rounded.Close, removeLabel, { onRemove?.invoke() }, enabled = onRemove != null)
    }
}

@Composable
private fun ChoiceEditor(question: AuthoredQuestion, viewModel: LessonEditorViewModel) {
    val multi = question.question is Question.MultiChoice
    FieldTitle(stringResource(if (multi) Res.string.lessonedit_answers_multi else Res.string.lessonedit_answers_single))
    val options = question.choiceOptions
    options.forEachIndexed { i, option ->
        val placeholder = stringResource(Res.string.lessonedit_option, i + 1)
        val correctLabel = stringResource(Res.string.lessonedit_correct, option.text.ifBlank { placeholder })
        EditableRow(
            value = option.text,
            onValueChange = { text -> viewModel.edit { withOptionText(i, text) } },
            placeholder = placeholder,
            onRemove = if (options.size > 2) ({ viewModel.edit { withoutOption(i) } }) else null,
            removeLabel = stringResource(Res.string.lessonedit_remove, option.text.ifBlank { placeholder }),
            leading = {
                val correct = question.isCorrectOption(option.id)
                val toggle = { viewModel.edit { withCorrectToggled(option.id) } }
                if (multi) {
                    Checkbox(
                        checked = correct,
                        onCheckedChange = { toggle() },
                        colors = CheckboxDefaults.colors(checkedColor = BookWormColors.Success, uncheckedColor = BookWormColors.Outline),
                        modifier = Modifier.semantics { contentDescription = correctLabel },
                    )
                } else {
                    RadioButton(
                        selected = correct,
                        onClick = toggle,
                        colors = RadioButtonDefaults.colors(selectedColor = BookWormColors.Success, unselectedColor = BookWormColors.Outline),
                        modifier = Modifier.semantics { contentDescription = correctLabel },
                    )
                }
            },
        )
    }
    if (options.size < 6) {
        SmallButton(stringResource(Res.string.lessonedit_add_option), { viewModel.edit { withNewOption() } }, variant = ButtonVariant.Secondary, leadingIcon = Icons.Rounded.Add)
    }
}

@Composable
private fun TypedEditor(question: AuthoredQuestion, viewModel: LessonEditorViewModel) {
    val typed = question.question as Question.TypedAnswer
    LabeledCheckbox(
        stringResource(Res.string.lessonedit_fraction_input),
        typed.fractionInput,
        onCheckedChange = { on -> viewModel.edit { withFractionInput(on) } },
    )
    BookWormTextField(
        value = question.textAnswer,
        onValueChange = { text -> viewModel.edit { withTextAnswer(text) } },
        label = stringResource(Res.string.lessonedit_typed_answer),
        placeholder = stringResource(Res.string.lessonedit_typed_answer_hint),
    )
    Text(stringResource(Res.string.lessonedit_typed_note), style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
}

@Composable
private fun BlankEditor(question: AuthoredQuestion, viewModel: LessonEditorViewModel) {
    val blank = question.question as Question.FillBlank
    BookWormTextField(
        value = blank.textBefore,
        onValueChange = { text -> viewModel.edit { withBlankText(before = text) } },
        label = stringResource(Res.string.lessonedit_text_before),
    )
    BookWormTextField(
        value = blank.textAfter,
        onValueChange = { text -> viewModel.edit { withBlankText(after = text) } },
        label = stringResource(Res.string.lessonedit_text_after),
    )
    // How the sentence reads, with the gap.
    Text(
        listOf(blank.textBefore, stringResource(Res.string.lessonedit_preview_gap), blank.textAfter).filter { it.isNotBlank() }.joinToString(" "),
        style = MaterialTheme.typography.bodyLarge,
        color = BookWormColors.PrimaryDark,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BookWormColors.PrimaryTint).padding(14.dp),
    )
    FieldTitle(stringResource(Res.string.lessonedit_words))
    blank.words.forEachIndexed { i, word ->
        val placeholder = stringResource(Res.string.lessonedit_word, i + 1)
        val correctLabel = stringResource(Res.string.lessonedit_correct, word.ifBlank { placeholder })
        EditableRow(
            value = word,
            onValueChange = { text -> viewModel.edit { withWord(i, text) } },
            placeholder = placeholder,
            onRemove = if (blank.words.size > 2) ({ viewModel.edit { withoutWord(i) } }) else null,
            removeLabel = stringResource(Res.string.lessonedit_remove, word.ifBlank { placeholder }),
            leading = {
                RadioButton(
                    selected = word.isNotEmpty() && word == question.textAnswer,
                    onClick = { viewModel.edit { withCorrectWord(i) } },
                    colors = RadioButtonDefaults.colors(selectedColor = BookWormColors.Success, unselectedColor = BookWormColors.Outline),
                    modifier = Modifier.semantics { contentDescription = correctLabel },
                )
            },
        )
    }
    if (blank.words.size < 6) {
        SmallButton(stringResource(Res.string.lessonedit_add_word), { viewModel.edit { withNewWord() } }, variant = ButtonVariant.Secondary, leadingIcon = Icons.Rounded.Add)
    }
}

@Composable
private fun OrderEditor(question: AuthoredQuestion, viewModel: LessonEditorViewModel) {
    FieldTitle(stringResource(Res.string.lessonedit_items))
    val items = question.orderItems
    items.forEachIndexed { i, item ->
        val placeholder = stringResource(Res.string.lessonedit_item, i + 1)
        val name = item.text.ifBlank { placeholder }
        EditableRow(
            value = item.text,
            onValueChange = { text -> viewModel.edit { withItemText(i, text) } },
            placeholder = placeholder,
            onRemove = if (items.size > 2) ({ viewModel.edit { withoutItem(i) } }) else null,
            removeLabel = stringResource(Res.string.lessonedit_remove, name),
            leading = { NumberBadge(i + 1) },
            trailing = {
                RoundIconButton(Icons.Rounded.ArrowUpward, stringResource(Res.string.paths_move_up, name), { viewModel.edit { withItemMoved(i, -1) } }, enabled = i > 0)
                RoundIconButton(Icons.Rounded.ArrowDownward, stringResource(Res.string.paths_move_down, name), { viewModel.edit { withItemMoved(i, 1) } }, enabled = i < items.lastIndex)
            },
        )
    }
    if (items.size < 6) {
        SmallButton(stringResource(Res.string.lessonedit_add_item), { viewModel.edit { withNewItem() } }, variant = ButtonVariant.Secondary, leadingIcon = Icons.Rounded.Add)
    }
}

@Composable
private fun MatchEditor(question: AuthoredQuestion, viewModel: LessonEditorViewModel) {
    FieldTitle(stringResource(Res.string.lessonedit_pairs))
    val pairs = question.matchPairs
    pairs.forEachIndexed { i, (left, right) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BookWormTextField(
                value = left.text,
                onValueChange = { text -> viewModel.edit { withPairText(i, left = text) } },
                placeholder = stringResource(Res.string.lessonedit_pair_left, i + 1),
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.SyncAlt, contentDescription = null, tint = BookWormColors.InkMuted)
            BookWormTextField(
                value = right.text,
                onValueChange = { text -> viewModel.edit { withPairText(i, right = text) } },
                placeholder = stringResource(Res.string.lessonedit_pair_right, i + 1),
                modifier = Modifier.weight(1f),
            )
            RoundIconButton(
                Icons.Rounded.Close,
                stringResource(Res.string.lessonedit_remove, left.text.ifBlank { stringResource(Res.string.lessonedit_pair_left, i + 1) }),
                { viewModel.edit { withoutPair(i) } },
                enabled = pairs.size > 2,
            )
        }
    }
    if (pairs.size < 6) {
        SmallButton(stringResource(Res.string.lessonedit_add_pair), { viewModel.edit { withNewPair() } }, variant = ButtonVariant.Secondary, leadingIcon = Icons.Rounded.Add)
    }
}

@Composable
private fun NumberBadge(number: Int, background: Color = BookWormColors.PrimarySoft) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Text("$number", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), color = BookWormColors.PrimaryDark)
    }
}
