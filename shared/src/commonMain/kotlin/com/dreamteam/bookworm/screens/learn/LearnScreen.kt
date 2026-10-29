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
import bookworm.shared.generated.resources.join_class_title
import bookworm.shared.generated.resources.learn_all_chapters
import bookworm.shared.generated.resources.learn_change_subject
import bookworm.shared.generated.resources.learn_chapter_number
import bookworm.shared.generated.resources.learn_chapter_progress
import bookworm.shared.generated.resources.learn_empty_body
import bookworm.shared.generated.resources.learn_lesson_done
import bookworm.shared.generated.resources.learn_lesson_locked
import bookworm.shared.generated.resources.learn_lesson_stars
import bookworm.shared.generated.resources.learn_lesson_start
import bookworm.shared.generated.resources.learn_no_path_body
import bookworm.shared.generated.resources.learn_no_path_title
import bookworm.shared.generated.resources.learn_notes
import bookworm.shared.generated.resources.learn_open_library
import bookworm.shared.generated.resources.learn_path_done
import bookworm.shared.generated.resources.learn_start
import bookworm.shared.generated.resources.library_empty_title
import bookworm.shared.generated.resources.stars_count
import bookworm.shared.generated.resources.streak_days
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.LearnChapter
import com.dreamteam.bookworm.domain.model.LessonNode
import com.dreamteam.bookworm.domain.model.LessonStatus
import com.dreamteam.bookworm.domain.model.Subject
import com.dreamteam.bookworm.ui_components.BadgeTile
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormProgressBar
import com.dreamteam.bookworm.ui_components.Eyebrow
import com.dreamteam.bookworm.ui_components.LedgeSurface
import com.dreamteam.bookworm.ui_components.LockedCard
import com.dreamteam.bookworm.ui_components.MascotIcon
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StatPill
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LearnScreen(
    onLessonClick: (lessonId: String) -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    onJoinClass: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LearnViewModel(container.learnRepository, container.libraryRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    StateContent(state, onRetry = viewModel::retry) { content ->
        Column(Modifier.fillMaxSize().background(BookWormColors.Background)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    content.subject?.let { SubjectPicker(content.subjects, it, onSelect = viewModel::selectSubject) }
                }
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

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val path = content.path
                val chapter = content.chapter
                when {
                    content.subject == null -> EmptyCard(
                        title = stringResource(Res.string.library_empty_title),
                        body = stringResource(Res.string.learn_empty_body),
                        action = stringResource(Res.string.join_class_title),
                        onAction = onJoinClass,
                    )
                    path == null || chapter == null -> EmptyCard(
                        title = stringResource(Res.string.learn_no_path_title),
                        body = stringResource(Res.string.learn_no_path_body, content.subject.name),
                        action = stringResource(Res.string.learn_open_library),
                        onAction = onOpenLibrary,
                    )
                    else -> {
                        if (path.chapters.all { c -> c.lessonsDone == c.lessons.size }) PathDone()
                        BookWormCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Eyebrow(stringResource(Res.string.learn_chapter_progress, chapter.chapter.number, chapter.lessonsDone, chapter.lessons.size))
                                    Text(chapter.chapter.title, style = MaterialTheme.typography.headlineMedium)
                                }
                                SmallButton(
                                    stringResource(Res.string.learn_notes),
                                    onClick = { onChapterClick(chapter.chapter.id) },
                                    leadingIcon = Icons.AutoMirrored.Outlined.MenuBook,
                                )
                            }
                        }

                        LessonPath(chapter.lessons, onLessonClick)

                        if (path.chapters.size > 1) {
                            ChapterList(path.chapters, selected = chapter, onSelect = viewModel::selectChapter)
                        }
                    }
                }
            }
        }
    }
}

/** No class yet, or no path for this class: a short explanation and where to go instead. */
@Composable
private fun EmptyCard(title: String, body: String, action: String, onAction: () -> Unit) {
    BookWormCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MascotIcon(Modifier.size(72.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = BookWormColors.Ink, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted, textAlign = TextAlign.Center)
            BookWormButton(action, onClick = onAction, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PathDone() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BookWormColors.SunSoft).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = BookWormColors.SunDark, modifier = Modifier.size(32.dp))
        Text(stringResource(Res.string.learn_path_done), style = MaterialTheme.typography.titleSmall, color = BookWormColors.Ink)
    }
}

/** Every chapter of the path. Open ones can be picked to see their lessons; locked ones wait their turn. */
@Composable
private fun ChapterList(chapters: List<LearnChapter>, selected: LearnChapter, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(Res.string.learn_all_chapters), style = MaterialTheme.typography.titleMedium, color = BookWormColors.Ink)
        chapters.forEach { item ->
            val eyebrow = stringResource(Res.string.learn_chapter_number, item.chapter.number)
            if (!item.isOpen) {
                LockedCard(eyebrow, item.chapter.title, Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Lock, contentDescription = stringResource(Res.string.common_locked), tint = BookWormColors.InkMuted)
                }
                return@forEach
            }
            val isSelected = item == selected
            BookWormCard(
                Modifier.fillMaxWidth().then(
                    if (isSelected) Modifier.border(2.dp, BookWormColors.Primary, RoundedCornerShape(20.dp)) else Modifier,
                ),
                onClick = { onSelect(item.chapter.id) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val allDone = item.lessonsDone == item.lessons.size
                    BadgeTile(
                        "${item.chapter.number}",
                        background = if (allDone) BookWormColors.Primary else BookWormColors.PrimarySoft,
                        contentColor = if (allDone) Color.White else BookWormColors.PrimaryDark,
                        size = 44.dp,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.chapter.title, style = MaterialTheme.typography.titleSmall, color = BookWormColors.Ink)
                        BookWormProgressBar(
                            if (item.lessons.isEmpty()) 0f else item.lessonsDone.toFloat() / item.lessons.size,
                            Modifier.semantics {
                                contentDescription = "$eyebrow, ${item.lessonsDone} / ${item.lessons.size}"
                            },
                            height = 8.dp,
                        )
                    }
                    if (allDone) Icon(Icons.Rounded.Check, contentDescription = null, tint = BookWormColors.Primary)
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
                // The stars still to win; done lessons have given theirs.
                if (lesson.status != LessonStatus.Done && lesson.stars > 0) {
                    val starsLabel = pluralStringResource(Res.plurals.learn_lesson_stars, lesson.stars, lesson.stars)
                    Row(
                        Modifier.semantics(mergeDescendants = true) { contentDescription = starsLabel },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = BookWormColors.Sun, modifier = Modifier.size(16.dp))
                        Text("+${lesson.stars}", style = MaterialTheme.typography.labelSmall, color = BookWormColors.InkMuted)
                    }
                }
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
