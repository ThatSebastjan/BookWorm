package com.dreamteam.bookworm.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.flashcards_count
import bookworm.shared.generated.resources.join_class_title
import bookworm.shared.generated.resources.library_empty_body
import bookworm.shared.generated.resources.library_empty_title
import bookworm.shared.generated.resources.library_status_known
import bookworm.shared.generated.resources.library_status_mastered
import bookworm.shared.generated.resources.library_status_new
import bookworm.shared.generated.resources.library_subtitle
import bookworm.shared.generated.resources.library_title
import bookworm.shared.generated.resources.notes_count
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.Subject
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui_components.BadgeTile
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormProgressBar
import com.dreamteam.bookworm.ui_components.ChoiceChip
import com.dreamteam.bookworm.ui_components.ErrorView
import com.dreamteam.bookworm.ui_components.LoadingView
import com.dreamteam.bookworm.ui_components.MascotIcon
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryScreen(onChapterClick: (chapterId: String) -> Unit, onJoinClass: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LibraryViewModel(container.libraryRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    StateContent(state.subjects, onRetry = viewModel::load) { subjects ->
        if (subjects.isEmpty()) {
            NoClasses(onJoinClass)
            return@StateContent
        }
        LibraryContent(
            subjects = subjects,
            selectedSubjectId = state.selectedSubjectId,
            chapters = state.chapters,
            onSelectSubject = viewModel::selectSubject,
            onRetryChapters = viewModel::retryChapters,
            onChapterClick = onChapterClick,
        )
    }
}

@Composable
private fun LibraryContent(
    subjects: List<Subject>,
    selectedSubjectId: String?,
    chapters: UiState<List<ChapterSummary>>,
    onSelectSubject: (String) -> Unit,
    onRetryChapters: () -> Unit,
    onChapterClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BookWormColors.Background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(Res.string.library_title), style = MaterialTheme.typography.headlineLarge, color = BookWormColors.Ink)
                    Text(stringResource(Res.string.library_subtitle), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjects, key = { it.id }) { subject ->
                        ChoiceChip(subject.name, selected = subject.id == selectedSubjectId, onClick = { onSelectSubject(subject.id) })
                    }
                }
            }
        }
        when (chapters) {
            UiState.Loading -> item { LoadingView(Modifier.height(240.dp)) }
            is UiState.Error -> item { ErrorView(onRetryChapters, Modifier.height(360.dp)) }
            is UiState.Success -> items(chapters.data, key = { it.id }) { chapter ->
                ChapterCard(chapter, onClick = { onChapterClick(chapter.id) })
            }
        }
    }
}

/** Before the student is in any class there is nothing to list. */
@Composable
private fun NoClasses(onJoinClass: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(BookWormColors.Background).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.library_title), style = MaterialTheme.typography.headlineLarge, color = BookWormColors.Ink)
            Text(stringResource(Res.string.library_subtitle), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        }
        BookWormCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MascotIcon(Modifier.size(72.dp))
                Text(stringResource(Res.string.library_empty_title), style = MaterialTheme.typography.titleMedium, color = BookWormColors.Ink)
                Text(
                    stringResource(Res.string.library_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BookWormColors.InkMuted,
                    textAlign = TextAlign.Center,
                )
                BookWormButton(stringResource(Res.string.join_class_title), onClick = onJoinClass, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ChapterCard(chapter: ChapterSummary, onClick: () -> Unit) {
    val status = when (chapter.knownFlashcards) {
        chapter.flashcardCount -> stringResource(Res.string.library_status_mastered)
        0 -> stringResource(Res.string.library_status_new)
        else -> stringResource(Res.string.library_status_known, chapter.knownFlashcards, chapter.flashcardCount)
    }
    BookWormCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeTile("${chapter.number}")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(chapter.title, style = MaterialTheme.typography.titleMedium, color = BookWormColors.Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Meta(Icons.AutoMirrored.Outlined.MenuBook, pluralStringResource(Res.plurals.notes_count, chapter.noteCount, chapter.noteCount))
                    Meta(Icons.Outlined.Style, pluralStringResource(Res.plurals.flashcards_count, chapter.flashcardCount, chapter.flashcardCount))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BookWormProgressBar(chapter.mastery, Modifier.weight(1f))
                    Text(status, style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = BookWormColors.Ink)
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = BookWormColors.InkMuted)
        }
    }
}

@Composable
private fun Meta(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = BookWormColors.InkMuted, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
    }
}
