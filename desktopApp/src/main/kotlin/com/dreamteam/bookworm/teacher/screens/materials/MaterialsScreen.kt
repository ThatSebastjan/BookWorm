package com.dreamteam.bookworm.teacher.screens.materials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.CardStyle
import com.dreamteam.bookworm.domain.model.FileKind
import com.dreamteam.bookworm.domain.model.MaterialsOverview
import com.dreamteam.bookworm.domain.model.PickedFile
import com.dreamteam.bookworm.domain.model.Upload
import com.dreamteam.bookworm.domain.model.UploadStatus
import com.dreamteam.bookworm.teacher.components.BookWormDropdown
import com.dreamteam.bookworm.teacher.components.Callout
import com.dreamteam.bookworm.teacher.components.SectionCard
import com.dreamteam.bookworm.teacher.components.SectionTitle
import com.dreamteam.bookworm.teacher.components.TeacherColors
import com.dreamteam.bookworm.teacher.components.TeacherHeader
import com.dreamteam.bookworm.teacher.components.TeacherPage
import com.dreamteam.bookworm.teacher.components.TwoPane
import com.dreamteam.bookworm.teacher.components.dashedBorder
import com.dreamteam.bookworm.teacher.platform.LocalTeacherPlatform
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.materials_browse
import com.dreamteam.bookworm.teacher.resources.materials_cards_ready
import com.dreamteam.bookworm.teacher.resources.materials_chapter
import com.dreamteam.bookworm.teacher.resources.materials_chapter_option
import com.dreamteam.bookworm.teacher.resources.materials_drop_hint
import com.dreamteam.bookworm.teacher.resources.materials_drop_title
import com.dreamteam.bookworm.teacher.resources.materials_make_flashcards
import com.dreamteam.bookworm.teacher.resources.materials_no_uploads
import com.dreamteam.bookworm.teacher.resources.materials_privacy
import com.dreamteam.bookworm.teacher.resources.materials_reading_level
import com.dreamteam.bookworm.teacher.resources.materials_remove_file
import com.dreamteam.bookworm.teacher.resources.materials_replace
import com.dreamteam.bookworm.teacher.resources.materials_review
import com.dreamteam.bookworm.teacher.resources.materials_style
import com.dreamteam.bookworm.teacher.resources.materials_style_mix
import com.dreamteam.bookworm.teacher.resources.materials_style_qa
import com.dreamteam.bookworm.teacher.resources.materials_style_terms
import com.dreamteam.bookworm.teacher.resources.materials_subject
import com.dreamteam.bookworm.teacher.resources.materials_subtitle
import com.dreamteam.bookworm.teacher.resources.materials_target_title
import com.dreamteam.bookworm.teacher.resources.materials_title
import com.dreamteam.bookworm.teacher.resources.materials_unreadable
import com.dreamteam.bookworm.teacher.resources.materials_upload_failed
import com.dreamteam.bookworm.teacher.resources.materials_uploads
import com.dreamteam.bookworm.teacher.resources.materials_writing
import com.dreamteam.bookworm.teacher.resources.materials_writing_description
import com.dreamteam.bookworm.teacher.resources.profile_grade
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormProgressBar
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.RoundIconButton
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StateContent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MaterialsScreen(onReview: (uploadId: String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { MaterialsViewModel(container.materialsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val platform = LocalTeacherPlatform.current
    val scope = rememberCoroutineScope()

    StateContent(state.overview, onRetry = viewModel::load) { overview ->
        TeacherPage {
            TeacherHeader(
                title = stringResource(Res.string.materials_title),
                subtitle = stringResource(Res.string.materials_subtitle),
            )
            TwoPane(
                start = {
                    DropZone(
                        staged = state.staged,
                        onBrowse = { scope.launch { viewModel.stage(platform.pickFiles()) } },
                        onDrop = viewModel::stage,
                        onRemove = viewModel::unstage,
                    )
                    CardTarget(
                        overview = overview,
                        state = state,
                        onSubject = viewModel::selectSubject,
                        onChapter = viewModel::selectChapter,
                        onStyle = viewModel::selectStyle,
                        onGrade = viewModel::selectGrade,
                        onMakeCards = viewModel::makeFlashcards,
                    )
                },
                end = {
                    SectionTitle(stringResource(Res.string.materials_uploads))
                    if (overview.uploads.isEmpty()) {
                        Text(stringResource(Res.string.materials_no_uploads), style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted)
                    }
                    overview.uploads.forEach { upload ->
                        UploadRow(
                            upload = upload,
                            onReview = { onReview(upload.id) },
                            onReplace = {
                                scope.launch {
                                    platform.pickFiles().firstOrNull()?.let { viewModel.replace(upload.id, it) }
                                }
                            },
                        )
                    }
                    Callout(
                        text = stringResource(Res.string.materials_privacy),
                        icon = Icons.Outlined.Info,
                        background = BookWormColors.PrimarySoft,
                        contentColor = BookWormColors.PrimaryDark,
                    )
                },
            )
        }
    }
}

@Composable
private fun DropZone(
    staged: List<PickedFile>,
    onBrowse: () -> Unit,
    onDrop: (List<PickedFile>) -> Unit,
    onRemove: (PickedFile) -> Unit,
) {
    val platform = LocalTeacherPlatform.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(platform.fileDropTarget(onDrop))
            .clip(RoundedCornerShape(28.dp))
            .background(BookWormColors.Surface)
            .dashedBorder(BookWormColors.Outline)
            .padding(horizontal = 24.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(BookWormColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.FileUpload, contentDescription = null, tint = BookWormColors.PrimaryDark, modifier = Modifier.size(36.dp))
        }
        Text(stringResource(Res.string.materials_drop_title), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp), textAlign = TextAlign.Center)
        Text(stringResource(Res.string.materials_drop_hint), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted, textAlign = TextAlign.Center)
        BookWormButton(stringResource(Res.string.materials_browse), onBrowse)

        if (staged.isNotEmpty()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                staged.forEach { file ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BookWormColors.PrimaryTint)
                            .padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = BookWormColors.PrimaryDark, modifier = Modifier.size(20.dp))
                        Text(
                            file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        RoundIconButton(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(Res.string.materials_remove_file, file.name),
                            onClick = { onRemove(file) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTarget(
    overview: MaterialsOverview,
    state: MaterialsUiState,
    onSubject: (String) -> Unit,
    onChapter: (String) -> Unit,
    onStyle: (CardStyle) -> Unit,
    onGrade: (Int) -> Unit,
    onMakeCards: () -> Unit,
) {
    val chapters = overview.chapters.filter { it.subjectId == state.subjectId }
    SectionCard(stringResource(Res.string.materials_target_title)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BookWormDropdown(
                label = stringResource(Res.string.materials_subject),
                options = overview.subjects,
                selected = overview.subjects.firstOrNull { it.id == state.subjectId },
                onSelect = { onSubject(it.id) },
                optionLabel = { it.name },
                modifier = Modifier.weight(1f),
            )
            BookWormDropdown(
                label = stringResource(Res.string.materials_chapter),
                options = chapters,
                selected = chapters.firstOrNull { it.id == state.chapterId },
                onSelect = { onChapter(it.id) },
                optionLabel = { stringResource(Res.string.materials_chapter_option, it.number, it.title) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BookWormDropdown(
                label = stringResource(Res.string.materials_style),
                options = CardStyle.entries,
                selected = state.style,
                onSelect = onStyle,
                optionLabel = { stringResource(it.label()) },
                modifier = Modifier.weight(1f),
            )
            BookWormDropdown(
                label = stringResource(Res.string.materials_reading_level),
                options = overview.readingGrades,
                selected = state.readingGrade,
                onSelect = onGrade,
                optionLabel = { stringResource(Res.string.profile_grade, it) },
                modifier = Modifier.weight(1f),
            )
        }
        BookWormButton(
            text = stringResource(Res.string.materials_make_flashcards),
            onClick = onMakeCards,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            variant = ButtonVariant.Success,
            enabled = state.canMakeCards,
            leadingIcon = Icons.Outlined.AutoAwesome,
        )
        if (state.uploadFailed) {
            Text(stringResource(Res.string.materials_upload_failed), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.Danger)
        }
    }
}

private fun CardStyle.label() = when (this) {
    CardStyle.Terms -> Res.string.materials_style_terms
    CardStyle.QuestionsAndAnswers -> Res.string.materials_style_qa
    CardStyle.Mix -> Res.string.materials_style_mix
}

@Composable
private fun UploadRow(upload: Upload, onReview: () -> Unit, onReplace: () -> Unit) {
    BookWormCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FileBadge(upload)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    upload.fileName,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when (val status = upload.status) {
                    is UploadStatus.Ready -> StatusLine(
                        Icons.Rounded.Check,
                        pluralStringResource(Res.plurals.materials_cards_ready, status.cardCount, status.cardCount),
                        BookWormColors.SuccessDark,
                    )
                    is UploadStatus.Processing -> {
                        val percent = (status.progress * 100).toInt()
                        val description = stringResource(Res.string.materials_writing_description, percent)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BookWormProgressBar(
                                status.progress,
                                Modifier.weight(1f).semantics { contentDescription = description },
                                color = BookWormColors.Primary,
                            )
                            Text(
                                stringResource(Res.string.materials_writing, percent),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                color = BookWormColors.InkMuted,
                            )
                        }
                    }
                    UploadStatus.Unreadable -> StatusLine(
                        Icons.Rounded.WarningAmber,
                        stringResource(Res.string.materials_unreadable),
                        TeacherColors.SunInk,
                    )
                }
            }
            when (upload.status) {
                is UploadStatus.Ready -> SmallButton(stringResource(Res.string.materials_review), onReview)
                UploadStatus.Unreadable -> SmallButton(stringResource(Res.string.materials_replace), onReplace, variant = ButtonVariant.Secondary)
                is UploadStatus.Processing -> Unit
            }
        }
    }
}

@Composable
private fun StatusLine(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = color)
    }
}

@Composable
private fun FileBadge(upload: Upload) {
    val (background, content) = when (upload.kind) {
        FileKind.Pdf -> BookWormColors.DangerSoft to BookWormColors.DangerDark
        FileKind.Slides -> BookWormColors.SunSoft to TeacherColors.SunInk
        FileKind.Photo -> BookWormColors.SuccessSoft to BookWormColors.SuccessDark
        FileKind.Document -> BookWormColors.PrimarySoft to BookWormColors.PrimaryDark
        FileKind.Text -> BookWormColors.Track to BookWormColors.InkMuted
    }
    // The file extension is a format name, not a word to translate.
    val extension = upload.fileName.substringAfterLast('.', "").take(4).uppercase()
    Box(
        Modifier.size(width = 48.dp, height = 56.dp).clip(RoundedCornerShape(10.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(extension, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), color = content)
    }
}
