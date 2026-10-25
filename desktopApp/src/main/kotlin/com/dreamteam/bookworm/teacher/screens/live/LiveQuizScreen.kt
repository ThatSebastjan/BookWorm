package com.dreamteam.bookworm.teacher.screens.live

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.LiveQuiz
import com.dreamteam.bookworm.teacher.components.CalmRing
import com.dreamteam.bookworm.teacher.components.TeacherColors
import com.dreamteam.bookworm.teacher.platform.LocalTeacherPlatform
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.common_error_body
import com.dreamteam.bookworm.teacher.resources.live_about_a_minute
import com.dreamteam.bookworm.teacher.resources.live_about_minutes
import com.dreamteam.bookworm.teacher.resources.live_add_minute
import com.dreamteam.bookworm.teacher.resources.live_breathe
import com.dreamteam.bookworm.teacher.resources.live_clock
import com.dreamteam.bookworm.teacher.resources.live_clock_left
import com.dreamteam.bookworm.teacher.resources.live_end
import com.dreamteam.bookworm.teacher.resources.live_finished
import com.dreamteam.bookworm.teacher.resources.live_header
import com.dreamteam.bookworm.teacher.resources.live_little_longer
import com.dreamteam.bookworm.teacher.resources.live_nearly_there
import com.dreamteam.bookworm.teacher.resources.live_no_rush
import com.dreamteam.bookworm.teacher.resources.live_pause
import com.dreamteam.bookworm.teacher.resources.live_paused
import com.dreamteam.bookworm.teacher.resources.live_paused_hint
import com.dreamteam.bookworm.teacher.resources.live_questions
import com.dreamteam.bookworm.teacher.resources.live_resume
import com.dreamteam.bookworm.teacher.resources.live_show_results
import com.dreamteam.bookworm.teacher.resources.live_thank_you
import com.dreamteam.bookworm.teacher.resources.live_time_up
import com.dreamteam.bookworm.teacher.resources.live_title
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LiveQuizScreen(
    quizId: String,
    onShowResults: () -> Unit,
    onEnd: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LiveQuizViewModel(quizId, container.popQuizRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val platform = LocalTeacherPlatform.current

    Column(Modifier.fillMaxSize().background(TeacherColors.CalmBackground)) {
        StateContent(state.quiz, onRetry = viewModel::load) { quiz ->
            // Play the chime once, when the countdown reaches zero.
            var chimed by remember { mutableStateOf(false) }
            LaunchedEffect(quiz.secondsLeft == 0) {
                if (quiz.secondsLeft == 0 && quiz.chime && !chimed) {
                    chimed = true
                    platform.playChime()
                }
            }

            LiveHeader(quiz, onEnd = { viewModel.finish(onEnd) }, enabled = !state.leaving)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
            ) {
                Text(
                    stringResource(Res.string.live_breathe),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
                    color = TeacherColors.CalmInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 720.dp),
                )
                Countdown(quiz)
                FinishedDots(quiz)
                Controls(
                    quiz = quiz,
                    enabled = !state.leaving,
                    onAddMinute = viewModel::addMinute,
                    onTogglePause = viewModel::togglePause,
                    onShowResults = { viewModel.finish(onShowResults) },
                )
                if (state.actionFailed) {
                    Text(stringResource(Res.string.common_error_body), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.Danger)
                }
            }
        }
    }
}

@Composable
private fun LiveHeader(quiz: LiveQuiz, onEnd: () -> Unit, enabled: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BookWormColors.Surface)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRect(TeacherColors.CalmDivider, topLeft = Offset(0f, size.height - stroke), size = size.copy(height = stroke))
            }
            .padding(horizontal = 40.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.live_title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
            Text(
                stringResource(Res.string.live_header, quiz.className, quiz.chapter.number, quiz.chapter.title),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = BookWormColors.InkMuted,
            )
        }
        Text(
            pluralStringResource(Res.plurals.live_questions, quiz.questionCount, quiz.questionCount),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 20.sp),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            CalmOutlineButton(stringResource(Res.string.live_end), onEnd, enabled = enabled, height = 44, textColor = BookWormColors.Ink)
        }
    }
}

@Composable
private fun Countdown(quiz: LiveQuiz) {
    val left = quiz.secondsLeft
    val headline: String
    val subline: String
    when {
        quiz.paused -> {
            headline = stringResource(Res.string.live_paused)
            subline = stringResource(Res.string.live_paused_hint)
        }
        left == 0 -> {
            headline = stringResource(Res.string.live_time_up)
            subline = stringResource(Res.string.live_thank_you)
        }
        !quiz.calmMode -> {
            headline = stringResource(Res.string.live_clock, left / 60, (left % 60).toString().padStart(2, '0'))
            subline = stringResource(Res.string.live_clock_left)
        }
        left <= 30 -> {
            headline = stringResource(Res.string.live_little_longer)
            subline = stringResource(Res.string.live_nearly_there)
        }
        left < 90 -> {
            headline = stringResource(Res.string.live_about_a_minute)
            subline = stringResource(Res.string.live_no_rush)
        }
        else -> {
            val minutes = (left + 59) / 60
            headline = pluralStringResource(Res.plurals.live_about_minutes, minutes, minutes)
            subline = stringResource(Res.string.live_no_rush)
        }
    }

    // A slow "breathing" pulse, as on the canvas. It pauses with the quiz.
    val breath = rememberInfiniteTransition()
    val pulse by breath.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(3_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    )
    Box(
        Modifier
            .size(380.dp)
            .scale(if (quiz.paused || left == 0) 1f else pulse)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        CalmRing(quiz.progress, Modifier.fillMaxSize().padding(13.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(headline, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Text(subline, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = TeacherColors.CalmInk)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinishedDots(quiz: LiveQuiz) {
    val label = stringResource(Res.string.live_finished, quiz.studentsFinished, quiz.studentCount)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // One dot per student reads well for a class; past 30 the sentence says enough.
        if (quiz.studentCount in 1..30) {
            FlowRow(
                Modifier.widthIn(max = 520.dp).clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(quiz.studentCount) { i ->
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (i < quiz.studentsFinished) TeacherColors.Calm else BookWormColors.Surface)
                            .border(2.dp, TeacherColors.Calm, CircleShape),
                    )
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp), color = TeacherColors.CalmInk)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Controls(
    quiz: LiveQuiz,
    enabled: Boolean,
    onAddMinute: () -> Unit,
    onTogglePause: () -> Unit,
    onShowResults: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CalmOutlineButton(
            stringResource(Res.string.live_add_minute),
            onAddMinute,
            enabled = enabled,
            icon = Icons.Rounded.MoreTime,
            borderColor = TeacherColors.Calm,
            textColor = TeacherColors.CalmDark,
        )
        CalmOutlineButton(
            stringResource(if (quiz.paused) Res.string.live_resume else Res.string.live_pause),
            onTogglePause,
            enabled = enabled && quiz.secondsLeft > 0,
            textColor = BookWormColors.Ink,
        )
        BookWormButton(
            stringResource(Res.string.live_show_results),
            onShowResults,
            variant = ButtonVariant.Success,
            enabled = enabled,
            leadingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
        )
    }
}

/** White button with a thin cool outline, used on the quiet live screen. */
@Composable
private fun CalmOutlineButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    textColor: Color,
    icon: ImageVector? = null,
    borderColor: Color = TeacherColors.CalmBorder,
    height: Int = 56,
) {
    val shape = RoundedCornerShape(if (height < 50) 14.dp else 18.dp)
    Row(
        Modifier
            .heightIn(min = height.dp)
            .clip(shape)
            .background(BookWormColors.Surface)
            .border(2.dp, borderColor, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
        Text(
            text,
            style = if (height < 50) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall.copy(fontSize = 18.sp),
            color = if (enabled) textColor else BookWormColors.InkMuted,
        )
    }
}
