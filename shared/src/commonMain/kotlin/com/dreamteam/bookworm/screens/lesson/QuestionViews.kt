package com.dreamteam.bookworm.screens.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.lesson_blank_empty
import bookworm.shared.generated.resources.lesson_blank_filled
import bookworm.shared.generated.resources.lesson_denominator
import bookworm.shared.generated.resources.lesson_match_hint
import bookworm.shared.generated.resources.lesson_numerator
import bookworm.shared.generated.resources.lesson_order_hint
import bookworm.shared.generated.resources.lesson_type_multi_hint
import bookworm.shared.generated.resources.lesson_your_answer
import com.dreamteam.bookworm.domain.model.Option
import com.dreamteam.bookworm.domain.model.Question
import com.dreamteam.bookworm.ui_components.AnswerOption
import com.dreamteam.bookworm.ui_components.AnswerState
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.FractionText
import com.dreamteam.bookworm.ui_components.ValueToggle
import org.jetbrains.compose.resources.stringResource

/** The answer area for one question; every interaction goes through [viewModel]. */
@Composable
internal fun QuestionBody(question: Question, state: LessonUiState, viewModel: LessonViewModel) {
    val result = (state.phase as? Phase.Checked)?.result
    /** Colour for something the student picked: neutral while answering, green or red once checked. */
    val pickedState = when {
        result == null -> AnswerState.Selected
        result.correct -> AnswerState.Correct
        else -> AnswerState.Wrong
    }
    val draft = state.draft
    val enabled = !state.locked

    when (question) {
        is Question.SingleChoice -> OptionGrid(question.options, draft.selected, pickedState, enabled, multi = false, onPick = viewModel::toggleOption)
        is Question.MultiChoice -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Hint(stringResource(Res.string.lesson_type_multi_hint))
            OptionGrid(question.options, draft.selected, pickedState, enabled, multi = true, onPick = viewModel::toggleOption)
        }
        is Question.TypedAnswer -> TypedAnswerInput(question, draft, result?.correct, enabled, viewModel)
        is Question.FillBlank -> FillBlankInput(question, draft, pickedState, enabled, viewModel)
        is Question.Order -> OrderInput(question, draft, pickedState, enabled, viewModel)
        is Question.Match -> MatchInput(question, draft, pickedState, enabled, viewModel)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
}

@Composable
private fun OptionContent(option: Option, fontSize: Int = 42) {
    val fraction = option.fraction
    if (fraction != null) {
        FractionText(fraction, fontSize = fontSize)
    } else {
        Text(option.text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun OptionGrid(
    options: List<Option>,
    selected: Set<String>,
    pickedState: AnswerState,
    enabled: Boolean,
    multi: Boolean,
    onPick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { option ->
                    val isPicked = option.id in selected
                    Box(Modifier.weight(1f)) {
                        AnswerOption(
                            state = if (isPicked) pickedState else AnswerState.Idle,
                            onClick = { onPick(option.id) },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp).semantics { contentDescription = option.text },
                        ) {
                            OptionContent(option)
                        }
                        if (multi) CheckMark(isPicked, Modifier.align(Alignment.TopEnd).padding(10.dp))
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CheckMark(checked: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .size(26.dp)
            .clip(shape)
            .background(if (checked) BookWormColors.Primary else BookWormColors.Surface)
            .border(2.5.dp, if (checked) BookWormColors.Primary else BookWormColors.Outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TypedAnswerInput(
    question: Question.TypedAnswer,
    draft: AnswerDraft,
    correct: Boolean?,
    enabled: Boolean,
    viewModel: LessonViewModel,
) {
    // A normal keyboard, not a number pad: iOS number pads have no return key, so they couldn't be dismissed.
    // The view model drops anything that isn't a digit.
    val focusManager = LocalFocusManager.current
    val next = KeyboardOptions(imeAction = ImeAction.Next)
    val done = KeyboardOptions(imeAction = ImeAction.Done)
    val dismiss = KeyboardActions(onDone = { focusManager.clearFocus() })
    if (question.fractionInput) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val numeratorLabel = stringResource(Res.string.lesson_numerator)
            val denominatorLabel = stringResource(Res.string.lesson_denominator)
            BookWormTextField(
                draft.numerator, viewModel::setNumerator,
                Modifier.width(96.dp).semantics { contentDescription = numeratorLabel },
                placeholder = "?", isError = correct == false, enabled = enabled,
                keyboardOptions = next, keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            Box(Modifier.width(112.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(BookWormColors.Ink))
            BookWormTextField(
                draft.denominator, viewModel::setDenominator,
                Modifier.width(96.dp).semantics { contentDescription = denominatorLabel },
                placeholder = "?", isError = correct == false, enabled = enabled,
                keyboardOptions = done, keyboardActions = dismiss,
            )
        }
    } else {
        BookWormTextField(
            draft.text, viewModel::setText, Modifier.fillMaxWidth(),
            label = stringResource(Res.string.lesson_your_answer), isError = correct == false, enabled = enabled,
            keyboardOptions = done, keyboardActions = dismiss,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FillBlankInput(
    question: Question.FillBlank,
    draft: AnswerDraft,
    pickedState: AnswerState,
    enabled: Boolean,
    viewModel: LessonViewModel,
) {
    val sentenceStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp, lineHeight = 30.sp)
    val word = draft.word
    val (blankFill, blankEdge) = when {
        word == null -> BookWormColors.Background to BookWormColors.Outline
        pickedState == AnswerState.Correct -> BookWormColors.SuccessSoft to BookWormColors.Success
        pickedState == AnswerState.Wrong -> BookWormColors.DangerSoft to BookWormColors.Danger
        else -> BookWormColors.PrimarySoft to BookWormColors.Primary
    }
    val blankLabel = if (word == null) stringResource(Res.string.lesson_blank_empty) else stringResource(Res.string.lesson_blank_filled, word)

    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(BookWormColors.Surface)
                .border(2.dp, BookWormColors.Border, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            question.textBefore.split(" ").forEach { Text(it, style = sentenceStyle) }
            Box(
                Modifier
                    .widthIn(min = 150.dp)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(blankFill)
                    .border(3.dp, blankEdge, RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled && word != null, role = Role.Button, onClick = viewModel::clearWord)
                    .semantics { contentDescription = blankLabel }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(word.orEmpty(), style = MaterialTheme.typography.labelMedium.copy(fontSize = 20.sp))
            }
            question.textAfter.split(" ").forEach { Text(it, style = sentenceStyle) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.words.forEach { option ->
                ValueToggle(
                    option,
                    selected = option == word,
                    onClick = { if (enabled) viewModel.pickWord(option) },
                )
            }
        }
    }
}

@Composable
private fun OrderInput(
    question: Question.Order,
    draft: AnswerDraft,
    pickedState: AnswerState,
    enabled: Boolean,
    viewModel: LessonViewModel,
) {
    val byId = question.items.associateBy { it.id }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Hint(stringResource(Res.string.lesson_order_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            question.items.indices.forEach { slot ->
                val placed = draft.order.getOrNull(slot)?.let(byId::get)
                AnswerOption(
                    state = if (placed != null) pickedState else AnswerState.Idle,
                    onClick = { viewModel.removeItemAt(slot) },
                    enabled = enabled && placed != null,
                    modifier = Modifier.weight(1f).heightIn(min = 120.dp),
                ) {
                    if (placed != null) {
                        OptionContent(placed, fontSize = 30)
                    } else {
                        Text("${slot + 1}", style = MaterialTheme.typography.headlineSmall, color = BookWormColors.TrackShadow)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            question.items.forEach { item ->
                val used = item.id in draft.order
                AnswerOption(
                    state = AnswerState.Idle,
                    onClick = { viewModel.placeItem(item.id) },
                    enabled = enabled && !used,
                    modifier = Modifier.weight(1f).heightIn(min = 120.dp).alpha(if (used) 0.35f else 1f),
                ) {
                    OptionContent(item, fontSize = 30)
                }
            }
        }
    }
}

private val pairTints = listOf(
    BookWormColors.Primary,
    Color(0xFF8F5A00),
    BookWormColors.Success,
    BookWormColors.Flame,
)

@Composable
private fun MatchInput(
    question: Question.Match,
    draft: AnswerDraft,
    pickedState: AnswerState,
    enabled: Boolean,
    viewModel: LessonViewModel,
) {
    val pairOrder = draft.pairs.keys.toList()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Hint(stringResource(Res.string.lesson_match_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                question.left.forEach { item ->
                    val pairNo = pairOrder.indexOf(item.id)
                    val state = when {
                        pairNo >= 0 -> pickedState
                        draft.pendingLeft == item.id -> AnswerState.Selected
                        else -> AnswerState.Idle
                    }
                    MatchTile(item, state, pairNo, enabled, onClick = { viewModel.pickLeft(item.id) })
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                question.right.forEach { item ->
                    val left = draft.pairs.entries.firstOrNull { it.value == item.id }?.key
                    val pairNo = left?.let(pairOrder::indexOf) ?: -1
                    MatchTile(item, if (pairNo >= 0) pickedState else AnswerState.Idle, pairNo, enabled, onClick = { viewModel.pickRight(item.id) })
                }
            }
        }
    }
}

@Composable
private fun MatchTile(item: Option, state: AnswerState, pairNo: Int, enabled: Boolean, onClick: () -> Unit) {
    Box {
        AnswerOption(
            state = state,
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp).semantics { contentDescription = item.text },
        ) {
            OptionContent(item, fontSize = 30)
        }
        if (pairNo >= 0) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(pairTints[pairNo % pairTints.size]),
                contentAlignment = Alignment.Center,
            ) {
                Text("${pairNo + 1}", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}
