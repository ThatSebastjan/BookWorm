package com.dreamteam.bookworm.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.common_locked
import bookworm.shared.generated.resources.learn_change_subject
import bookworm.shared.generated.resources.learn_chapter_progress
import bookworm.shared.generated.resources.learn_lesson_done
import bookworm.shared.generated.resources.learn_lesson_locked
import bookworm.shared.generated.resources.learn_lesson_start
import bookworm.shared.generated.resources.learn_next_chapter
import bookworm.shared.generated.resources.learn_notes
import bookworm.shared.generated.resources.learn_start
import bookworm.shared.generated.resources.stars_count
import bookworm.shared.generated.resources.streak_days
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.LessonNode
import com.dreamteam.bookworm.domain.model.LessonStatus
import com.dreamteam.bookworm.domain.model.Subject
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.Eyebrow
import com.dreamteam.bookworm.ui_components.LedgeSurface
import com.dreamteam.bookworm.ui_components.LockedCard
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StatPill
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LearnScreen(
    onLessonClick: (lessonId: String) -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LearnViewModel(container.learnRepository, container.libraryRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    StateContent(state, onRetry = viewModel::retry) { content ->
        Column(Modifier.fillMaxSize().background(BookWormColors.Background)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubjectPicker(content.subjects, content.path.subject, onSelect = viewModel::selectSubject)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(
                        Icons.Rounded.LocalFireDepartment, "${content.stats.streakDays}",
                        pluralStringResource(Res.plurals.streak_days, content.stats.streakDays, content.stats.streakDays),
                        iconTint = BookWormColors.Flame,
                    )
                    StatPill(
                        Icons.Rounded.Star, "${content.stats.stars}",
                        pluralStringResource(Res.plurals.stars_count, content.stats.stars, content.stats.stars),
                    )
                }
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val chapter = content.path.chapter
                val done = content.path.lessons.count { it.status == LessonStatus.Done }
                BookWormCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Eyebrow(stringResource(Res.string.learn_chapter_progress, chapter.number, done, content.path.lessons.size))
                            Text(chapter.title, style = MaterialTheme.typography.headlineMedium)
                        }
                        SmallButton(
                            stringResource(Res.string.learn_notes),
                            onClick = { onChapterClick(chapter.id) },
                            leadingIcon = Icons.AutoMirrored.Outlined.MenuBook,
                        )
                    }
                }

                LessonPath(content.path.lessons, onLessonClick)

                content.path.nextChapter?.let { next ->
                    LockedCard(stringResource(Res.string.learn_next_chapter, next.number), next.title, Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Lock, contentDescription = stringResource(Res.string.common_locked), tint = BookWormColors.InkMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectPicker(subjects: List<Subject>, current: Subject, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    Box {
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(shape)
                .background(BookWormColors.Surface)
                .border(2.dp, BookWormColors.Border, shape)
                .clickable(role = Role.Button, onClick = { open = true })
                .padding(horizontal = 14.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val label = stringResource(Res.string.learn_change_subject, current.name)
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(BookWormColors.Primary))
            Text(current.name, style = MaterialTheme.typography.labelMedium, modifier = Modifier.semantics { contentDescription = label })
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = BookWormColors.Ink, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = BookWormColors.Surface) {
            subjects.forEach { subject ->
                DropdownMenuItem(
                    text = { Text(subject.name, style = MaterialTheme.typography.labelMedium) },
                    onClick = {
                        open = false
                        onSelect(subject.id)
                    },
                )
            }
        }
    }
}

/** Lessons as a winding path of round buttons, like the canvas. */
@Composable
private fun LessonPath(lessons: List<LessonNode>, onLessonClick: (String) -> Unit) {
    val offsets = listOf(0, 0, 0, -80, 80, 0)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        lessons.forEachIndexed { index, lesson ->
            Column(
                Modifier.offset(x = offsets[index % offsets.size].dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LessonButton(lesson, onClick = { onLessonClick(lesson.lessonId) })
                Text(
                    lesson.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (lesson.status == LessonStatus.Locked) BookWormColors.InkMuted else BookWormColors.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(140.dp),
                )
            }
        }
    }
}

@Composable
private fun LessonButton(lesson: LessonNode, onClick: () -> Unit) {
    val (face, ledge, iconTint) = when (lesson.status) {
        LessonStatus.Done -> Triple(BookWormColors.Primary, BookWormColors.PrimaryDark, Color.White)
        LessonStatus.Current -> Triple(BookWormColors.Sun, BookWormColors.SunDark, BookWormColors.Ink)
        LessonStatus.Locked -> Triple(BookWormColors.Track, BookWormColors.TrackShadow, BookWormColors.LockedIcon)
    }
    val size = when {
        lesson.status == LessonStatus.Current -> 88.dp
        lesson.isChapterQuiz -> 80.dp
        else -> 64.dp
    }
    val icon = when {
        lesson.status == LessonStatus.Done -> Icons.Rounded.Check
        lesson.status == LessonStatus.Current -> Icons.Rounded.Star
        lesson.isChapterQuiz -> Icons.Rounded.EmojiEvents
        else -> Icons.Outlined.Lock
    }
    val description = when (lesson.status) {
        LessonStatus.Done -> stringResource(Res.string.learn_lesson_done, lesson.title)
        LessonStatus.Current -> stringResource(Res.string.learn_lesson_start, lesson.title)
        LessonStatus.Locked -> stringResource(Res.string.learn_lesson_locked, lesson.title)
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LedgeSurface(
            color = face,
            ledgeColor = ledge,
            shape = CircleShape,
            modifier = Modifier.size(size + 6.dp).semantics { contentDescription = description },
            onClick = if (lesson.status == LessonStatus.Locked) null else onClick,
            ledge = 6.dp,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.45f))
        }
        if (lesson.status == LessonStatus.Current) {
            Text(
                stringResource(Res.string.learn_start).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BookWormColors.Ink)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}
