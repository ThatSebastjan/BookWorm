package com.dreamteam.bookworm.teacher.screens.quizsetup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.QuizSetup
import com.dreamteam.bookworm.teacher.components.CalmRing
import com.dreamteam.bookworm.teacher.components.SectionCard
import com.dreamteam.bookworm.teacher.components.TeacherColors
import com.dreamteam.bookworm.teacher.components.TeacherHeader
import com.dreamteam.bookworm.teacher.components.TeacherPage
import com.dreamteam.bookworm.teacher.components.TwoPane
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.common_error_body
import com.dreamteam.bookworm.teacher.resources.quiz_approved_cards
import com.dreamteam.bookworm.teacher.resources.quiz_calm_mode
import com.dreamteam.bookworm.teacher.resources.quiz_calm_mode_body
import com.dreamteam.bookworm.teacher.resources.quiz_chapter_option
import com.dreamteam.bookworm.teacher.resources.quiz_chime
import com.dreamteam.bookworm.teacher.resources.quiz_minutes_option
import com.dreamteam.bookworm.teacher.resources.quiz_no_cards
import com.dreamteam.bookworm.teacher.resources.quiz_ready
import com.dreamteam.bookworm.teacher.resources.quiz_ring_note
import com.dreamteam.bookworm.teacher.resources.quiz_start
import com.dreamteam.bookworm.teacher.resources.quiz_step_cards
import com.dreamteam.bookworm.teacher.resources.quiz_step_cards_hint
import com.dreamteam.bookworm.teacher.resources.quiz_step_count
import com.dreamteam.bookworm.teacher.resources.quiz_step_time
import com.dreamteam.bookworm.teacher.resources.quiz_step_time_hint
import com.dreamteam.bookworm.teacher.resources.quiz_subtitle
import com.dreamteam.bookworm.teacher.resources.quiz_summary_cards
import com.dreamteam.bookworm.teacher.resources.quiz_summary_per_question
import com.dreamteam.bookworm.teacher.resources.quiz_summary_per_question_value
import com.dreamteam.bookworm.teacher.resources.quiz_summary_questions
import com.dreamteam.bookworm.teacher.resources.quiz_summary_time
import com.dreamteam.bookworm.teacher.resources.quiz_summary_time_value
import com.dreamteam.bookworm.teacher.resources.quiz_title
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.LabeledCheckbox
import com.dreamteam.bookworm.ui_components.LedgeSurface
import com.dreamteam.bookworm.ui_components.StateContent
import com.dreamteam.bookworm.ui_components.ValueToggle
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun QuizSetupScreen(classId: String, onStarted: (quizId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { QuizSetupViewModel(classId, container.popQuizRepository, onStarted) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    StateContent(state.setup, onRetry = viewModel::load) { setup ->
        TeacherPage {
            TeacherHeader(
                title = stringResource(Res.string.quiz_title),
                subtitle = stringResource(Res.string.quiz_subtitle, setup.className, setup.studentsInClass),
            )
            TwoPane(
                startWeight = 1.4f,
                start = { Steps(setup, state, viewModel) },
                end = { Summary(state, onStart = viewModel::start) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Steps(setup: QuizSetup, state: QuizSetupUiState, viewModel: QuizSetupViewModel) {
    SectionCard(stringResource(Res.string.quiz_step_cards)) {
        Text(stringResource(Res.string.quiz_step_cards_hint), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        if (setup.chapters.isEmpty()) {
            Text(stringResource(Res.string.quiz_no_cards), style = MaterialTheme.typography.bodyLarge, color = TeacherColors.SunInk)
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            setup.chapters.forEach { chapter ->
                ChapterOption(
                    chapter,
                    subjectName = setup.subjects.firstOrNull { it.id == chapter.subjectId }?.name.orEmpty(),
                    selected = chapter.id == state.chapterId, onClick = { viewModel.selectChapter(chapter.id) })
            }
        }
    }
    SectionCard(stringResource(Res.string.quiz_step_count)) {
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            setup.questionCounts.forEach { count ->
                ValueToggle("$count", selected = count == state.questionCount, onClick = { viewModel.selectCount(count) })
            }
        }
    }
    SectionCard(stringResource(Res.string.quiz_step_time)) {
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            setup.minuteOptions.forEach { minutes ->
                ValueToggle(
                    stringResource(Res.string.quiz_minutes_option, minutes),
                    selected = minutes == state.minutes,
                    onClick = { viewModel.selectMinutes(minutes) },
                )
            }
        }
        Text(
            stringResource(Res.string.quiz_step_time_hint),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = BookWormColors.InkMuted,
        )
    }
    SectionCard(stringResource(Res.string.quiz_calm_mode)) {
        LabeledCheckbox(stringResource(Res.string.quiz_calm_mode_body), state.calmMode, viewModel::setCalmMode)
        LabeledCheckbox(stringResource(Res.string.quiz_chime), state.chime, viewModel::setChime)
    }
}

@Composable
private fun ChapterOption(chapter: ChapterSummary, subjectName: String, selected: Boolean, onClick: () -> Unit) {
    val edge = if (selected) BookWormColors.Primary else BookWormColors.Border
    LedgeSurface(
        color = if (selected) BookWormColors.PrimaryTint else BookWormColors.Surface,
        ledgeColor = edge,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).semantics { this.selected = selected },
        onClick = onClick,
        role = Role.RadioButton,
        borderColor = edge,
        borderWidth = 3.dp,
        ledge = 4.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(BookWormColors.Surface)
                    .border(3.dp, if (selected) BookWormColors.Primary else BookWormColors.Outline, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(if (selected) BookWormColors.Primary else BookWormColors.Surface),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.quiz_chapter_option, chapter.number, chapter.title), style = MaterialTheme.typography.labelMedium.copy(fontSize = 17.sp))
                Text(subjectName, style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
            }
            Text(
                pluralStringResource(Res.plurals.quiz_approved_cards, chapter.flashcardCount, chapter.flashcardCount),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = BookWormColors.InkMuted,
            )
        }
    }
}

@Composable
private fun Summary(state: QuizSetupUiState, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(BookWormColors.PrimarySoft).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(Res.string.quiz_ready), style = MaterialTheme.typography.headlineSmall, color = BookWormColors.PrimaryDeep)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val chapter = state.chapter
            SummaryRow(
                stringResource(Res.string.quiz_summary_cards),
                chapter?.let { stringResource(Res.string.quiz_chapter_option, it.number, it.title) } ?: "–",
            )
            SummaryRow(stringResource(Res.string.quiz_summary_questions), "${state.questionCount}")
            SummaryRow(stringResource(Res.string.quiz_summary_time), pluralStringResource(Res.plurals.quiz_summary_time_value, state.minutes, state.minutes))
            SummaryRow(stringResource(Res.string.quiz_summary_per_question), stringResource(Res.string.quiz_summary_per_question_value, state.secondsPerQuestion))
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BookWormColors.Surface).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CalmRing(progress = 0.3f, modifier = Modifier.size(56.dp), strokeWidth = 6.dp)
            Text(stringResource(Res.string.quiz_ring_note), style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
        }
        BookWormButton(
            stringResource(Res.string.quiz_start),
            onStart,
            Modifier.fillMaxWidth(),
            variant = ButtonVariant.Success,
            enabled = state.canStart,
            leadingIcon = Icons.Rounded.PlayArrow,
        )
        if (state.startFailed) {
            Text(stringResource(Res.string.common_error_body), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.Danger)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp), color = BookWormColors.PrimaryDeep, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = BookWormColors.PrimaryDeep, modifier = Modifier.weight(0.6f))
    }
}
