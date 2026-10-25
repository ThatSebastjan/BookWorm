package com.dreamteam.bookworm.screens.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.common_back
import bookworm.shared.generated.resources.common_check
import bookworm.shared.generated.resources.common_continue
import bookworm.shared.generated.resources.common_error_title
import bookworm.shared.generated.resources.common_try_again
import bookworm.shared.generated.resources.lesson_close
import bookworm.shared.generated.resources.lesson_complete_score
import bookworm.shared.generated.resources.lesson_complete_stars
import bookworm.shared.generated.resources.lesson_complete_title
import bookworm.shared.generated.resources.lesson_correct_title
import bookworm.shared.generated.resources.lesson_finish
import bookworm.shared.generated.resources.lesson_leave_body
import bookworm.shared.generated.resources.lesson_leave_confirm
import bookworm.shared.generated.resources.lesson_leave_stay
import bookworm.shared.generated.resources.lesson_leave_title
import bookworm.shared.generated.resources.lesson_not_ready_body
import bookworm.shared.generated.resources.lesson_not_ready_title
import bookworm.shared.generated.resources.lesson_question_of
import bookworm.shared.generated.resources.lesson_type_blank
import bookworm.shared.generated.resources.lesson_type_choice
import bookworm.shared.generated.resources.lesson_type_match
import bookworm.shared.generated.resources.lesson_type_multi
import bookworm.shared.generated.resources.lesson_type_order
import bookworm.shared.generated.resources.lesson_type_typed
import bookworm.shared.generated.resources.lesson_wrong_title
import bookworm.shared.generated.resources.stars_count
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.LessonSummary
import com.dreamteam.bookworm.domain.model.Question
import com.dreamteam.bookworm.ui.PlatformBackHandler
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui_components.ActionFooter
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormDialog
import com.dreamteam.bookworm.ui_components.BookWormProgressBar
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.Eyebrow
import com.dreamteam.bookworm.ui_components.FeedbackPanel
import com.dreamteam.bookworm.ui_components.FeedbackTone
import com.dreamteam.bookworm.ui_components.RoundIconButton
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LessonScreen(lessonId: String, onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LessonViewModel(lessonId, container.lessonRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Nothing to lose when the lesson is over or has no questions yet.
    val canLeaveFreely = state.summary != null || (state.lesson is UiState.Success && state.questionCount == 0)
    PlatformBackHandler { if (canLeaveFreely) onClose() else confirmLeave = true }

    StateContent(state.lesson, onRetry = viewModel::load) {
        Column(Modifier.fillMaxSize().background(BookWormColors.Background)) {
            val summary = state.summary
            if (state.questionCount == 0) {
                LessonNotReady(onClose)
            } else if (summary != null) {
                LessonComplete(summary, onDone = onClose)
            } else {
                LessonHeader(
                    index = state.index,
                    total = state.questionCount,
                    stars = state.starsEarned,
                    onClose = { confirmLeave = true },
                )
                state.question?.let { question ->
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Eyebrow(stringResource(question.typeLabel()))
                            Text(question.prompt, style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 34.sp))
                        }
                        QuestionBody(question, state, viewModel)
                    }
                    LessonFooter(
                        state,
                        onCheck = {
                            focusManager.clearFocus()
                            viewModel.check()
                        },
                        onRetry = viewModel::retry,
                        onNext = viewModel::next,
                    )
                }
            }
        }
    }

    if (confirmLeave) {
        BookWormDialog(
            title = stringResource(Res.string.lesson_leave_title),
            message = stringResource(Res.string.lesson_leave_body),
            onDismissRequest = { confirmLeave = false },
            confirmText = stringResource(Res.string.lesson_leave_stay),
            onConfirm = { confirmLeave = false },
            dismissText = stringResource(Res.string.lesson_leave_confirm),
            onDismiss = {
                confirmLeave = false
                onClose()
            },
        )
    }
}

/** The teacher hasn't written questions yet and the chapter has no flashcards to practise with. */
@Composable
private fun LessonNotReady(onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.lesson_not_ready_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(Res.string.lesson_not_ready_body),
            style = MaterialTheme.typography.bodyLarge,
            color = BookWormColors.InkMuted,
            textAlign = TextAlign.Center,
        )
        BookWormButton(stringResource(Res.string.common_back), onClose, Modifier.fillMaxWidth())
    }
}

private fun Question.typeLabel() = when (this) {
    is Question.SingleChoice -> Res.string.lesson_type_choice
    is Question.MultiChoice -> Res.string.lesson_type_multi
    is Question.TypedAnswer -> Res.string.lesson_type_typed
    is Question.FillBlank -> Res.string.lesson_type_blank
    is Question.Order -> Res.string.lesson_type_order
    is Question.Match -> Res.string.lesson_type_match
}

@Composable
private fun LessonHeader(index: Int, total: Int, stars: Int, onClose: () -> Unit) {
    val progressLabel = stringResource(Res.string.lesson_question_of, index + 1, total)
    val starsLabel = pluralStringResource(Res.plurals.stars_count, stars, stars)
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoundIconButton(Icons.Outlined.Close, stringResource(Res.string.lesson_close), onClick = onClose)
        BookWormProgressBar(
            progress = if (total == 0) 0f else (index + 1f) / total,
            modifier = Modifier.weight(1f).semantics { contentDescription = progressLabel },
            height = 16.dp,
        )
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = starsLabel },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = BookWormColors.Sun, modifier = Modifier.size(22.dp))
            Text("$stars", style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp))
        }
    }
}

@Composable
private fun LessonFooter(state: LessonUiState, onCheck: () -> Unit, onRetry: () -> Unit, onNext: () -> Unit) {
    val phase = state.phase
    if (phase is Phase.Checked) {
        val result = phase.result
        Column {
            if (state.submitFailed) ErrorLine()
            FeedbackPanel(
                tone = if (result.correct) FeedbackTone.Correct else FeedbackTone.Wrong,
                title = if (result.correct) {
                    pluralStringResource(Res.plurals.lesson_correct_title, result.starsEarned, result.starsEarned)
                } else {
                    stringResource(Res.string.lesson_wrong_title)
                },
                message = result.explanation,
                actionText = stringResource(if (result.correct) Res.string.common_continue else Res.string.common_try_again),
                onAction = if (result.correct) onNext else onRetry,
            )
        }
    } else {
        ActionFooter {
            if (state.submitFailed) ErrorLine()
            BookWormButton(
                stringResource(Res.string.common_check),
                onClick = onCheck,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canCheck,
            )
        }
    }
}

@Composable
private fun ErrorLine() {
    Text(
        stringResource(Res.string.common_error_title),
        style = MaterialTheme.typography.bodyMedium,
        color = BookWormColors.Danger,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LessonComplete(summary: LessonSummary, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(132.dp).clip(CircleShape).background(BookWormColors.SunSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = BookWormColors.Sun, modifier = Modifier.size(88.dp))
        }
        Spacer(Modifier.size(24.dp))
        Text(stringResource(Res.string.lesson_complete_title), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.size(8.dp))
        Text(
            pluralStringResource(Res.plurals.lesson_complete_stars, summary.starsEarned, summary.starsEarned),
            style = MaterialTheme.typography.titleLarge,
            color = BookWormColors.PrimaryDark,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.lesson_complete_score, summary.correctAnswers, summary.totalQuestions),
            style = MaterialTheme.typography.bodyLarge,
            color = BookWormColors.InkMuted,
        )
        Spacer(Modifier.weight(1f))
        BookWormButton(stringResource(Res.string.lesson_finish), onDone, Modifier.fillMaxWidth(), variant = ButtonVariant.Success)
    }
}
