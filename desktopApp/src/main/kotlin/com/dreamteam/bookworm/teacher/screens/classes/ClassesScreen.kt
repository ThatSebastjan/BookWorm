package com.dreamteam.bookworm.teacher.screens.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.GRADES
import com.dreamteam.bookworm.domain.model.TeacherClass
import com.dreamteam.bookworm.navigation.ScopedViewModelStore
import com.dreamteam.bookworm.teacher.components.BookWormDropdown
import com.dreamteam.bookworm.teacher.components.BookWormFormDialog
import com.dreamteam.bookworm.teacher.components.Callout
import com.dreamteam.bookworm.teacher.components.SectionCard
import com.dreamteam.bookworm.teacher.components.TeacherHeader
import com.dreamteam.bookworm.teacher.components.TeacherPage
import com.dreamteam.bookworm.teacher.components.TwoPane
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.classes_action_failed
import com.dreamteam.bookworm.teacher.resources.classes_add_chapter
import com.dreamteam.bookworm.teacher.resources.classes_card_count
import com.dreamteam.bookworm.teacher.resources.classes_chapters
import com.dreamteam.bookworm.teacher.resources.classes_delete
import com.dreamteam.bookworm.teacher.resources.classes_delete_body
import com.dreamteam.bookworm.teacher.resources.classes_delete_chapter
import com.dreamteam.bookworm.teacher.resources.classes_delete_chapter_body
import com.dreamteam.bookworm.teacher.resources.classes_delete_chapter_title
import com.dreamteam.bookworm.teacher.resources.classes_delete_title
import com.dreamteam.bookworm.teacher.resources.classes_edit
import com.dreamteam.bookworm.teacher.resources.classes_edit_title
import com.dreamteam.bookworm.teacher.resources.classes_empty_body
import com.dreamteam.bookworm.teacher.resources.classes_empty_title
import com.dreamteam.bookworm.teacher.resources.classes_move_down
import com.dreamteam.bookworm.teacher.resources.classes_move_up
import com.dreamteam.bookworm.teacher.resources.classes_name
import com.dreamteam.bookworm.teacher.resources.classes_name_hint
import com.dreamteam.bookworm.teacher.resources.classes_new
import com.dreamteam.bookworm.teacher.resources.classes_new_chapter_hint
import com.dreamteam.bookworm.teacher.resources.classes_new_title
import com.dreamteam.bookworm.teacher.resources.classes_no_chapters
import com.dreamteam.bookworm.teacher.resources.classes_not_used
import com.dreamteam.bookworm.teacher.resources.classes_note_count
import com.dreamteam.bookworm.teacher.resources.classes_open_chapter
import com.dreamteam.bookworm.teacher.resources.classes_subtitle
import com.dreamteam.bookworm.teacher.resources.classes_title
import com.dreamteam.bookworm.teacher.resources.classes_used_by
import com.dreamteam.bookworm.teacher.resources.profile_grade
import com.dreamteam.bookworm.teacher.resources.teacher_cancel
import com.dreamteam.bookworm.teacher.resources.teacher_class_grade
import com.dreamteam.bookworm.teacher.resources.teacher_create
import com.dreamteam.bookworm.teacher.resources.teacher_save
import com.dreamteam.bookworm.ui_components.BadgeTile
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormDialog
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.LedgeSurface
import com.dreamteam.bookworm.ui_components.MascotIcon
import com.dreamteam.bookworm.ui_components.RoundIconButton
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Classes: the teacher's teaching content. Pick a class on the left, manage its numbered
 * chapters on the right, and open a chapter to write its notes and flashcards.
 */
@Composable
fun ClassesScreen() {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ClassesViewModel(container.classesRepository, container.teacherRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingChapterId by remember { mutableStateOf<String?>(null) }

    // The chapter editor takes over the page; coming back reloads the counts.
    editingChapterId?.let { chapterId ->
        ScopedViewModelStore(chapterId) {
            ChapterEditorScreen(chapterId, onBack = {
                editingChapterId = null
                viewModel.refresh()
            })
        }
        return
    }

    StateContent(state.classes, onRetry = viewModel::load) { classes ->
        TeacherPage {
            TeacherHeader(
                title = stringResource(Res.string.classes_title),
                subtitle = stringResource(Res.string.classes_subtitle),
                actions = {
                    BookWormButton(
                        stringResource(Res.string.classes_new),
                        onClick = { viewModel.showDialog(ClassDialog.New) },
                        leadingIcon = Icons.Rounded.Add,
                        enabled = !state.busy,
                    )
                },
            )
            if (state.actionFailed) {
                Callout(
                    text = stringResource(Res.string.classes_action_failed),
                    icon = Icons.Outlined.ErrorOutline,
                    background = BookWormColors.DangerSoft,
                    contentColor = BookWormColors.DangerDark,
                )
            }
            val selected = state.selected
            if (selected == null) {
                EmptyClasses(onNew = { viewModel.showDialog(ClassDialog.New) })
            } else {
                TwoPane(
                    // Side by side already at the default window size (the list is narrow).
                    minWidthForRow = 760.dp,
                    startWeight = 0.8f,
                    endWeight = 1.6f,
                    start = {
                        classes.forEach { schoolClass ->
                            ClassCard(schoolClass, selected = schoolClass.id == selected.id, state.classroomNames) {
                                viewModel.select(schoolClass.id)
                            }
                        }
                    },
                    end = {
                        ClassDetail(
                            schoolClass = selected,
                            state = state,
                            viewModel = viewModel,
                            onOpenChapter = { editingChapterId = it },
                        )
                    },
                )
            }
        }
    }

    state.dialog?.let { dialog ->
        ClassFormDialog(dialog, onSave = viewModel::saveClass, onDismiss = { viewModel.showDialog(null) })
    }
    state.deletingClass?.let { schoolClass ->
        BookWormDialog(
            title = stringResource(Res.string.classes_delete_title, schoolClass.name),
            message = stringResource(Res.string.classes_delete_body),
            onDismissRequest = { viewModel.askDeleteClass(null) },
            confirmText = stringResource(Res.string.classes_delete),
            onConfirm = viewModel::deleteClass,
            dismissText = stringResource(Res.string.teacher_cancel),
            confirmVariant = ButtonVariant.Danger,
        )
    }
    state.deletingChapter?.let { chapter ->
        BookWormDialog(
            title = stringResource(Res.string.classes_delete_chapter_title, chapter.number, chapter.title),
            message = stringResource(Res.string.classes_delete_chapter_body),
            onDismissRequest = { viewModel.askDeleteChapter(null) },
            confirmText = stringResource(Res.string.classes_delete_chapter),
            onConfirm = viewModel::deleteChapter,
            dismissText = stringResource(Res.string.teacher_cancel),
            confirmVariant = ButtonVariant.Danger,
        )
    }
}

@Composable
private fun EmptyClasses(onNew: () -> Unit) {
    BookWormCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(32.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MascotIcon(Modifier.padding(4.dp))
            Text(stringResource(Res.string.classes_empty_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.classes_empty_body), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
            BookWormButton(stringResource(Res.string.classes_new), onNew, leadingIcon = Icons.Rounded.Add)
        }
    }
}

@Composable
private fun ClassCard(schoolClass: TeacherClass, selected: Boolean, classroomNames: Map<String, String>, onClick: () -> Unit) {
    LedgeSurface(
        color = if (selected) BookWormColors.PrimaryTint else BookWormColors.Surface,
        ledgeColor = if (selected) BookWormColors.Primary else BookWormColors.Border,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = if (selected) BookWormColors.Primary else BookWormColors.Border,
        ledge = if (selected) 4.dp else 3.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(schoolClass.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp))
            Text(usedBy(schoolClass, classroomNames), style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
        }
    }
}

@Composable
private fun usedBy(schoolClass: TeacherClass, classroomNames: Map<String, String>): String {
    val names = schoolClass.classroomIds.mapNotNull { classroomNames[it] }
    return if (names.isEmpty()) stringResource(Res.string.classes_not_used) else stringResource(Res.string.classes_used_by, names.joinToString(", "))
}

@Composable
private fun ClassDetail(
    schoolClass: TeacherClass,
    state: ClassesUiState,
    viewModel: ClassesViewModel,
    onOpenChapter: (String) -> Unit,
) {
    SectionCard(schoolClass.name) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                listOfNotNull(schoolClass.grade?.let { stringResource(Res.string.profile_grade, it) }, usedBy(schoolClass, state.classroomNames))
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = BookWormColors.InkMuted,
                modifier = Modifier.weight(1f),
            )
            SmallButton(
                stringResource(Res.string.classes_edit),
                { viewModel.showDialog(ClassDialog.Edit(schoolClass)) },
                variant = ButtonVariant.Secondary,
                leadingIcon = Icons.Outlined.Edit,
                enabled = !state.busy,
            )
            SmallButton(
                stringResource(Res.string.classes_delete),
                { viewModel.askDeleteClass(schoolClass) },
                variant = ButtonVariant.Danger,
                leadingIcon = Icons.Outlined.Delete,
                enabled = !state.busy,
            )
        }
    }
    SectionCard(stringResource(Res.string.classes_chapters)) {
        if (schoolClass.chapters.isEmpty()) {
            Text(stringResource(Res.string.classes_no_chapters), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        }
        schoolClass.chapters.forEachIndexed { index, chapter ->
            ChapterRow(
                chapter = chapter,
                isFirst = index == 0,
                isLast = index == schoolClass.chapters.lastIndex,
                enabled = !state.busy,
                onOpen = { onOpenChapter(chapter.id) },
                onMove = { by -> viewModel.moveChapter(index, by) },
                onDelete = { viewModel.askDeleteChapter(chapter) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BookWormTextField(
                value = state.newChapterTitle,
                onValueChange = viewModel::setNewChapterTitle,
                placeholder = stringResource(Res.string.classes_new_chapter_hint),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.addChapter() }),
            )
            BookWormButton(
                stringResource(Res.string.classes_add_chapter),
                viewModel::addChapter,
                leadingIcon = Icons.Rounded.Add,
                enabled = state.newChapterTitle.isNotBlank() && !state.busy,
            )
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterSummary,
    isFirst: Boolean,
    isLast: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    BookWormCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BadgeTile("${chapter.number}", size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(chapter.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(Res.plurals.classes_note_count, chapter.noteCount, chapter.noteCount) + " · " +
                        pluralStringResource(Res.plurals.classes_card_count, chapter.flashcardCount, chapter.flashcardCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = BookWormColors.InkMuted,
                )
            }
            RoundIconButton(Icons.Rounded.KeyboardArrowUp, stringResource(Res.string.classes_move_up, chapter.title), { onMove(-1) }, enabled = enabled && !isFirst)
            RoundIconButton(Icons.Rounded.KeyboardArrowDown, stringResource(Res.string.classes_move_down, chapter.title), { onMove(1) }, enabled = enabled && !isLast)
            RoundIconButton(Icons.Outlined.Delete, stringResource(Res.string.classes_delete_chapter_title, chapter.number, chapter.title), onDelete, enabled = enabled)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(Res.string.classes_open_chapter, chapter.title), tint = BookWormColors.InkMuted)
        }
    }
}

@Composable
private fun ClassFormDialog(dialog: ClassDialog, onSave: (name: String, grade: Int) -> Unit, onDismiss: () -> Unit) {
    val editing = (dialog as? ClassDialog.Edit)?.schoolClass
    var name by remember(dialog) { mutableStateOf(editing?.name.orEmpty()) }
    var grade by remember(dialog) { mutableStateOf(editing?.grade?.coerceIn(GRADES) ?: GRADES.first) }
    BookWormFormDialog(
        title = stringResource(if (editing == null) Res.string.classes_new_title else Res.string.classes_edit_title),
        onDismissRequest = onDismiss,
        confirmText = stringResource(if (editing == null) Res.string.teacher_create else Res.string.teacher_save),
        onConfirm = { onSave(name, grade) },
        dismissText = stringResource(Res.string.teacher_cancel),
        confirmEnabled = name.isNotBlank(),
    ) {
        BookWormTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(Res.string.classes_name),
            placeholder = stringResource(Res.string.classes_name_hint),
        )
        BookWormDropdown(
            label = stringResource(Res.string.teacher_class_grade),
            options = GRADES.toList(),
            selected = grade,
            onSelect = { grade = it },
            optionLabel = { stringResource(Res.string.profile_grade, it) },
        )
    }
}
