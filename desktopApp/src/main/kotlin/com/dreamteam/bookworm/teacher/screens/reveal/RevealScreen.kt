package com.dreamteam.bookworm.teacher.screens.reveal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.Option
import com.dreamteam.bookworm.domain.model.QuestionResult
import com.dreamteam.bookworm.teacher.components.FractionPie
import com.dreamteam.bookworm.teacher.components.TeacherColors
import com.dreamteam.bookworm.teacher.components.TwoPane
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.live_end
import com.dreamteam.bookworm.teacher.resources.live_header
import com.dreamteam.bookworm.teacher.resources.live_title
import com.dreamteam.bookworm.teacher.resources.reveal_denominator
import com.dreamteam.bookworm.teacher.resources.reveal_denominator_hint
import com.dreamteam.bookworm.teacher.resources.reveal_finish
import com.dreamteam.bookworm.teacher.resources.reveal_finish_here
import com.dreamteam.bookworm.teacher.resources.reveal_header
import com.dreamteam.bookworm.teacher.resources.reveal_insight_good
import com.dreamteam.bookworm.teacher.resources.reveal_insight_talk
import com.dreamteam.bookworm.teacher.resources.reveal_next
import com.dreamteam.bookworm.teacher.resources.reveal_numerator
import com.dreamteam.bookworm.teacher.resources.reveal_numerator_hint
import com.dreamteam.bookworm.teacher.resources.reveal_option_description
import com.dreamteam.bookworm.teacher.resources.reveal_pie
import com.dreamteam.bookworm.teacher.resources.reveal_question
import com.dreamteam.bookworm.teacher.resources.reveal_right_answer
import com.dreamteam.bookworm.teacher.resources.reveal_students
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.Eyebrow
import com.dreamteam.bookworm.ui_components.FractionText
import com.dreamteam.bookworm.ui_components.MascotIcon
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RevealScreen(quizId: String, onEnd: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { RevealViewModel(quizId, container.popQuizRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(BookWormColors.Background)) {
        StateContent(state.result, onRetry = viewModel::load) { result ->
            RevealHeader(result, onEnd)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 32.dp),
            ) {
                TwoPane(
                    minWidthForRow = 1000.dp,
                    gap = 32.dp,
                    start = { Tallies(result) },
                    end = {
                        Explanation(
                            result = result,
                            isLast = state.isLast,
                            onNext = { if (state.isLast) onEnd() else viewModel.next() },
                            onFinish = onEnd,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun RevealHeader(result: QuestionResult, onEnd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BookWormColors.Surface)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRect(BookWormColors.Divider, topLeft = Offset(0f, size.height - stroke), size = size.copy(height = stroke))
            }
            .padding(horizontal = 40.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MascotIcon(Modifier.size(36.dp))
            Column {
                Text(stringResource(Res.string.live_title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
                Text(
                    stringResource(Res.string.live_header, result.className, result.chapter.number, result.chapter.title),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = BookWormColors.InkMuted,
                )
            }
        }
        Text(
            stringResource(Res.string.reveal_header, result.number, result.questionCount),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 20.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            SmallButton(stringResource(Res.string.live_end), onEnd, variant = ButtonVariant.Secondary)
        }
    }
}

@Composable
private fun Tallies(result: QuestionResult) {
    BookWormCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(28.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow(stringResource(Res.string.reveal_question, result.number))
                Text(
                    result.question.prompt,
                    style = MaterialTheme.typography.displaySmall.copy(lineHeight = 41.sp),
                    modifier = Modifier.semantics { heading() },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                result.question.options.forEachIndexed { index, option ->
                    OptionBar(
                        option = option,
                        key = ('A' + index).toString(),
                        count = result.answerCounts[option.id] ?: 0,
                        correct = option.id == result.correctOptionId,
                        total = result.studentCount,
                    )
                }
            }
            val ratio = if (result.studentCount == 0) 0f else result.correctCount.toFloat() / result.studentCount
            Text(
                stringResource(
                    if (ratio >= 0.8f) Res.string.reveal_insight_good else Res.string.reveal_insight_talk,
                    result.correctCount,
                    result.studentCount,
                ),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp),
                color = TeacherColors.SunInkDark,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BookWormColors.SunSoft).padding(horizontal = 18.dp, vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun OptionBar(option: Option, key: String, count: Int, correct: Boolean, total: Int) {
    val share = if (total == 0) 0f else count.toFloat() / total
    val who = pluralStringResource(Res.plurals.reveal_students, count, count)
    val description = stringResource(Res.string.reveal_option_description, key, option.text, who) +
        if (correct) ", " + stringResource(Res.string.reveal_right_answer) else ""
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (correct) BookWormColors.Success else BookWormColors.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Text(key, style = MaterialTheme.typography.labelMedium.copy(fontSize = 18.sp), color = Color.White)
        }
        // Fractions are drawn the school way, as in the student app's answer tiles.
        val fraction = option.fraction
        if (fraction != null) {
            Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) { FractionText(fraction, fontSize = 20) }
        } else {
            Text(
                option.text,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = if (option.text.length <= 4) 30.sp else 20.sp),
                modifier = Modifier.width(if (option.text.length <= 4) 64.dp else 180.dp),
            )
        }
        Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(12.dp)).background(TeacherColors.ResultTrack)) {
            if (share > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(share)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (correct) BookWormColors.Success else TeacherColors.PrimaryMuted),
                )
            }
        }
        Row(Modifier.width(140.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(who, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp))
            if (correct) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = BookWormColors.SuccessDark, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Explanation(
    result: QuestionResult,
    isLast: Boolean,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        result.fraction?.let { fraction ->
            FlowRow(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(BookWormColors.PrimarySoft).padding(28.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FractionPie(
                    fraction,
                    contentDescription = stringResource(Res.string.reveal_pie, fraction.denominator, fraction.numerator),
                    modifier = Modifier.size(140.dp).align(Alignment.CenterVertically),
                )
                Row(Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    FractionText(
                        fraction,
                        fontSize = 64,
                        color = BookWormColors.PrimaryDeep,
                        modifier = Modifier.semantics {
                            contentDescription = "${fraction.numerator}/${fraction.denominator}"
                        },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        FractionPart(stringResource(Res.string.reveal_numerator), stringResource(Res.string.reveal_numerator_hint))
                        FractionPart(stringResource(Res.string.reveal_denominator), stringResource(Res.string.reveal_denominator_hint))
                    }
                }
            }
        }
        Text(result.explanation, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 27.sp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BookWormButton(
                stringResource(if (isLast) Res.string.reveal_finish else Res.string.reveal_next),
                onNext,
                variant = ButtonVariant.Success,
                leadingIcon = if (isLast) null else Icons.AutoMirrored.Rounded.ArrowForward,
            )
            if (!isLast) {
                BookWormButton(stringResource(Res.string.reveal_finish_here), onFinish, variant = ButtonVariant.Secondary)
            }
        }
    }
}

@Composable
private fun FractionPart(name: String, hint: String) {
    Column {
        Text(name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 18.sp), color = BookWormColors.PrimaryDeep)
        Text(hint, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = BookWormColors.PrimaryDeep)
    }
}
