package com.dreamteam.bookworm.screens.chapter

import org.jetbrains.compose.resources.pluralStringResource
import com.dreamteam.bookworm.ui_components.MascotIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.chapter_card_meaning
import bookworm.shared.generated.resources.library_status_known
import bookworm.shared.generated.resources.chapter_start_over_failed
import bookworm.shared.generated.resources.chapter_start_over
import bookworm.shared.generated.resources.chapter_mastered_title
import bookworm.shared.generated.resources.chapter_mastered_body
import bookworm.shared.generated.resources.chapter_card_word
import bookworm.shared.generated.resources.chapter_eyebrow
import bookworm.shared.generated.resources.chapter_got_it
import bookworm.shared.generated.resources.chapter_no_flashcards
import bookworm.shared.generated.resources.chapter_notes_by
import bookworm.shared.generated.resources.chapter_practise
import bookworm.shared.generated.resources.chapter_remember
import bookworm.shared.generated.resources.chapter_still_learning
import bookworm.shared.generated.resources.chapter_tab_flashcards
import bookworm.shared.generated.resources.chapter_tab_notes
import bookworm.shared.generated.resources.chapter_tap_to_flip
import bookworm.shared.generated.resources.common_back
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.Chapter
import com.dreamteam.bookworm.domain.model.Flashcard
import com.dreamteam.bookworm.domain.model.NoteSection
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormProgressBar
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.Eyebrow
import com.dreamteam.bookworm.ui_components.LedgeSurface
import com.dreamteam.bookworm.ui_components.RoundIconButton
import com.dreamteam.bookworm.ui_components.SegmentedControl
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChapterScreen(chapterId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ChapterViewModel(chapterId, container.libraryRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    StateContent(state.chapter, onRetry = viewModel::load) { chapter ->
        Column(Modifier.fillMaxSize().background(BookWormColors.Background)) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundIconButton(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        stringResource(Res.string.common_back),
                        onClick = onBack,
                        tint = BookWormColors.Ink,
                        background = BookWormColors.Surface,
                        modifier = Modifier.border(2.dp, BookWormColors.Border, CircleShape),
                    )
                    Column {
                        Eyebrow(stringResource(Res.string.chapter_eyebrow, chapter.subjectName, chapter.number))
                        Text(chapter.title, style = MaterialTheme.typography.headlineMedium)
                    }
                }
                SegmentedControl(
                    options = listOf(stringResource(Res.string.chapter_tab_notes), stringResource(Res.string.chapter_tab_flashcards)),
                    selectedIndex = state.tab.ordinal,
                    onSelect = { viewModel.selectTab(ChapterTab.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when (state.tab) {
                ChapterTab.Notes -> NotesTab(chapter, onPractise = { viewModel.selectTab(ChapterTab.Flashcards) })
                ChapterTab.Flashcards -> FlashcardsTab(
                    state = state,
                    onFlip = viewModel::flip,
                    onAnswer = viewModel::answerCard,
                    onStartOver = viewModel::startOver,
                )
            }
        }
    }
}

@Composable
private fun NotesTab(chapter: Chapter, onPractise: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = BookWormColors.InkMuted, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.chapter_notes_by, chapter.teacherName), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = BookWormColors.InkMuted)
        }
        chapter.notes.forEach { NoteSectionView(it) }
        if (chapter.flashcards.isNotEmpty()) {
            BookWormButton(stringResource(Res.string.chapter_practise), onPractise, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NoteSectionView(section: NoteSection) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(section.heading, style = MaterialTheme.typography.headlineSmall)
        Text(section.body, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp))
        section.remember?.let { remember ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BookWormColors.SunSoft).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Eyebrow(stringResource(Res.string.chapter_remember), color = Color(0xFF7A4B00))
                Text(remember, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp))
            }
        }
    }
}

/** Practice with the cards the student doesn't know yet; once they know them all, they can start over. */
@Composable
private fun FlashcardsTab(
    state: ChapterUiState,
    onFlip: () -> Unit,
    onAnswer: (known: Boolean) -> Unit,
    onStartOver: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        val card = state.current
        when {
            state.total == 0 -> BookWormCard(Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.chapter_no_flashcards), style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted)
            }
            state.mastered || card == null -> Mastered(state, onStartOver)
            else -> FlashcardView(card, state.knownCount, state.total, state.flipped, onFlip, onAnswer)
        }
    }
}

@Composable
private fun KnownProgress(known: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BookWormProgressBar(known.toFloat() / total, Modifier.weight(1f), height = 12.dp)
        Text(stringResource(Res.string.library_status_known, known, total), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = BookWormColors.InkMuted)
    }
}

@Composable
private fun ColumnScope.Mastered(state: ChapterUiState, onStartOver: () -> Unit) {
    KnownProgress(state.total, state.total)
    BookWormCard(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(24.dp)) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            MascotIcon(Modifier.size(96.dp))
            Text(
                pluralStringResource(Res.plurals.chapter_mastered_title, state.total, state.total),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.chapter_mastered_body),
                style = MaterialTheme.typography.bodyLarge,
                color = BookWormColors.InkMuted,
                textAlign = TextAlign.Center,
            )
            if (state.startOverFailed) {
                Text(
                    stringResource(Res.string.chapter_start_over_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BookWormColors.Danger,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    BookWormButton(
        stringResource(Res.string.chapter_start_over),
        onStartOver,
        Modifier.fillMaxWidth(),
        enabled = !state.startingOver,
        leadingIcon = Icons.Rounded.Autorenew,
    )
}

@Composable
private fun ColumnScope.FlashcardView(
    card: Flashcard,
    known: Int,
    total: Int,
    flipped: Boolean,
    onFlip: () -> Unit,
    onAnswer: (known: Boolean) -> Unit,
) {
    KnownProgress(known, total)

    val face = if (flipped) BookWormColors.Primary else BookWormColors.Surface
    val edge = if (flipped) BookWormColors.PrimaryDark else BookWormColors.Border
    val ink = if (flipped) Color.White else BookWormColors.Ink
    LedgeSurface(
        color = face,
        ledgeColor = edge,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 320.dp),
        onClick = onFlip,
        borderColor = if (flipped) BookWormColors.Primary else BookWormColors.Border,
        borderWidth = 3.dp,
        ledge = 6.dp,
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Eyebrow(stringResource(if (flipped) Res.string.chapter_card_meaning else Res.string.chapter_card_word), color = ink)
            Text(
                if (flipped) card.definition else card.term,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = if (flipped) 26.sp else 44.sp, lineHeight = if (flipped) 32.sp else 50.sp),
                color = ink,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Autorenew, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
                Text(stringResource(Res.string.chapter_tap_to_flip), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = ink)
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BookWormButton(stringResource(Res.string.chapter_still_learning), { onAnswer(false) }, Modifier.weight(1f), variant = ButtonVariant.Secondary)
        BookWormButton(stringResource(Res.string.chapter_got_it), { onAnswer(true) }, Modifier.weight(1f), variant = ButtonVariant.Success)
    }
}
