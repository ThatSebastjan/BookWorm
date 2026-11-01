package com.dreamteam.bookworm.teacher.screens.curriculum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.Curriculum
import com.dreamteam.bookworm.domain.model.CurriculumLesson
import com.dreamteam.bookworm.domain.model.CurriculumLibrary
import com.dreamteam.bookworm.domain.model.CurriculumSummary
import com.dreamteam.bookworm.domain.model.CurriculumUnit
import com.dreamteam.bookworm.domain.model.PathProgress
import com.dreamteam.bookworm.domain.model.StudentProgress
import com.dreamteam.bookworm.domain.model.UnlockMode
import com.dreamteam.bookworm.navigation.ScopedViewModelStore
import com.dreamteam.bookworm.teacher.components.BookWormDropdown
import com.dreamteam.bookworm.teacher.components.BookWormFormDialog
import com.dreamteam.bookworm.teacher.components.Callout
import com.dreamteam.bookworm.teacher.components.SectionCard
import com.dreamteam.bookworm.teacher.components.TeacherColors
import com.dreamteam.bookworm.teacher.components.TeacherHeader
import com.dreamteam.bookworm.teacher.components.TeacherPage
import com.dreamteam.bookworm.teacher.components.dashedBorder
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.paths_action_failed
import com.dreamteam.bookworm.teacher.resources.paths_add_chapter
import com.dreamteam.bookworm.teacher.resources.paths_add_lesson
import com.dreamteam.bookworm.teacher.resources.paths_chapter_cards
import com.dreamteam.bookworm.teacher.resources.paths_chapter_count
import com.dreamteam.bookworm.teacher.resources.paths_chapter_option
import com.dreamteam.bookworm.teacher.resources.paths_chapter_pick
import com.dreamteam.bookworm.teacher.resources.paths_chapters
import com.dreamteam.bookworm.teacher.resources.paths_chapters_hint
import com.dreamteam.bookworm.teacher.resources.paths_class
import com.dreamteam.bookworm.teacher.resources.paths_create_chapter
import com.dreamteam.bookworm.teacher.resources.paths_default_lesson
import com.dreamteam.bookworm.teacher.resources.paths_default_practise
import com.dreamteam.bookworm.teacher.resources.paths_default_quiz
import com.dreamteam.bookworm.teacher.resources.paths_delete
import com.dreamteam.bookworm.teacher.resources.paths_delete_body
import com.dreamteam.bookworm.teacher.resources.paths_delete_title
import com.dreamteam.bookworm.teacher.resources.paths_discard
import com.dreamteam.bookworm.teacher.resources.paths_edit_questions
import com.dreamteam.bookworm.teacher.resources.paths_empty_body
import com.dreamteam.bookworm.teacher.resources.paths_empty_title
import com.dreamteam.bookworm.teacher.resources.paths_followers
import com.dreamteam.bookworm.teacher.resources.paths_followers_hint
import com.dreamteam.bookworm.teacher.resources.paths_from_flashcards
import com.dreamteam.bookworm.teacher.resources.paths_keep_editing
import com.dreamteam.bookworm.teacher.resources.paths_lesson_count
import com.dreamteam.bookworm.teacher.resources.paths_lesson_stars
import com.dreamteam.bookworm.teacher.resources.paths_lesson_title
import com.dreamteam.bookworm.teacher.resources.paths_mode
import com.dreamteam.bookworm.teacher.resources.paths_mode_any
import com.dreamteam.bookworm.teacher.resources.paths_mode_any_hint
import com.dreamteam.bookworm.teacher.resources.paths_mode_in_order
import com.dreamteam.bookworm.teacher.resources.paths_mode_in_order_hint
import com.dreamteam.bookworm.teacher.resources.paths_move_down
import com.dreamteam.bookworm.teacher.resources.paths_move_up
import com.dreamteam.bookworm.teacher.resources.paths_name
import com.dreamteam.bookworm.teacher.resources.paths_new
import com.dreamteam.bookworm.teacher.resources.paths_new_chapter
import com.dreamteam.bookworm.teacher.resources.paths_new_chapter_hint
import com.dreamteam.bookworm.teacher.resources.paths_new_title
import com.dreamteam.bookworm.teacher.resources.paths_no_chapters
import com.dreamteam.bookworm.teacher.resources.paths_no_followers
import com.dreamteam.bookworm.teacher.resources.paths_now_follows
import com.dreamteam.bookworm.teacher.resources.paths_progress_lessons
import com.dreamteam.bookworm.teacher.resources.paths_progress_no_classrooms
import com.dreamteam.bookworm.teacher.resources.paths_progress_no_students
import com.dreamteam.bookworm.teacher.resources.paths_progress_stars
import com.dreamteam.bookworm.teacher.resources.paths_progress_unsaved
import com.dreamteam.bookworm.teacher.resources.paths_question_count
import com.dreamteam.bookworm.teacher.resources.paths_quiz_off
import com.dreamteam.bookworm.teacher.resources.paths_quiz_on
import com.dreamteam.bookworm.teacher.resources.paths_remove
import com.dreamteam.bookworm.teacher.resources.paths_save
import com.dreamteam.bookworm.teacher.resources.paths_save_first
import com.dreamteam.bookworm.teacher.resources.paths_saved
import com.dreamteam.bookworm.teacher.resources.paths_subject_grade
import com.dreamteam.bookworm.teacher.resources.paths_subtitle
import com.dreamteam.bookworm.teacher.resources.paths_tab_edit
import com.dreamteam.bookworm.teacher.resources.paths_tab_progress
import com.dreamteam.bookworm.teacher.resources.paths_title
import com.dreamteam.bookworm.teacher.resources.paths_unsaved_body
import com.dreamteam.bookworm.teacher.resources.paths_unsaved_title
import com.dreamteam.bookworm.teacher.resources.teacher_cancel
import com.dreamteam.bookworm.teacher.resources.teacher_create
import com.dreamteam.bookworm.teacher.screens.lessoneditor.LessonEditorScreen
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui_components.BadgeTile
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormDialog
import com.dreamteam.bookworm.ui_components.BookWormProgressBar
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.LabeledCheckbox
import com.dreamteam.bookworm.ui_components.LedgeSurface
import com.dreamteam.bookworm.ui_components.LoadingView
import com.dreamteam.bookworm.ui_components.RoundIconButton
import com.dreamteam.bookworm.ui_components.SegmentedControl
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Learning paths: what students learn on their own in a class, in which order, and which classrooms follow it. */
@Composable
fun CurriculumScreen() {
    val container = LocalAppContainer.current
    val viewModel = viewModel { CurriculumViewModel(container.curriculumRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingLessonId by remember { mutableStateOf<String?>(null) }

    // The lesson editor takes over the page; the path and its unsaved changes wait here.
    val lessonId = editingLessonId
    if (lessonId != null) {
        ScopedViewModelStore(lessonId) {
            LessonEditorScreen(lessonId, onBack = {
                editingLessonId = null
                viewModel.refreshQuestionCounts()
            })
        }
        return
    }

    StateContent(state.library, onRetry = viewModel::load) { library ->
        TeacherPage {
            TeacherHeader(
                title = stringResource(Res.string.paths_title),
                subtitle = stringResource(Res.string.paths_subtitle),
                actions = {
                    BookWormButton(stringResource(Res.string.paths_new), { viewModel.showNewDialog(true) }, leadingIcon = Icons.Rounded.Add)
                },
            )
            if (state.actionFailed) {
                Callout(
                    text = stringResource(Res.string.paths_action_failed),
                    icon = Icons.Outlined.ErrorOutline,
                    background = BookWormColors.DangerSoft,
                    contentColor = BookWormColors.DangerDark,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 980.dp
                val list = @Composable { modifier: Modifier ->
                    PathList(library, selectedId = state.draft?.id, onSelect = viewModel::select, modifier = modifier)
                }
                val editor = @Composable { modifier: Modifier ->
                    Box(modifier) {
                        val draft = state.draft
                        when {
                            state.loadingDraft -> LoadingView(Modifier.padding(48.dp))
                            draft != null -> Editor(library, draft, state, viewModel, onEditLesson = { editingLessonId = it })
                            else -> EmptyEditor(onNew = { viewModel.showNewDialog(true) })
                        }
                    }
                }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        list(Modifier.width(320.dp))
                        editor(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        list(Modifier.fillMaxWidth())
                        editor(Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (state.showNewDialog) {
            NewPathDialog(library, onCreate = viewModel::startNew, onDismiss = { viewModel.showNewDialog(false) })
        }
        if (state.confirmDelete) {
            BookWormDialog(
                title = stringResource(Res.string.paths_delete_title, state.draft?.name.orEmpty()),
                message = stringResource(Res.string.paths_delete_body),
                onDismissRequest = { viewModel.askDelete(false) },
                confirmText = stringResource(Res.string.paths_delete),
                onConfirm = viewModel::delete,
                dismissText = stringResource(Res.string.teacher_cancel),
                confirmVariant = ButtonVariant.Danger,
            )
        }
        if (state.pendingSelection != null) {
            BookWormDialog(
                title = stringResource(Res.string.paths_unsaved_title),
                message = stringResource(Res.string.paths_unsaved_body),
                onDismissRequest = viewModel::keepEditing,
                confirmText = stringResource(Res.string.paths_keep_editing),
                onConfirm = viewModel::keepEditing,
                dismissText = stringResource(Res.string.paths_discard),
                onDismiss = viewModel::discardAndOpenPending,
            )
        }
    }
}

// Path list

@Composable
private fun PathList(
    library: CurriculumLibrary,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        library.curricula.forEach { path ->
            PathListItem(library, path, selected = path.id == selectedId, onClick = { onSelect(path.id) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PathListItem(library: CurriculumLibrary, path: CurriculumSummary, selected: Boolean, onClick: () -> Unit) {
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(path.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp))
            Text(
                subjectGrade(library, path.subjectId, path.grade) + " · " +
                    pluralStringResource(Res.plurals.paths_chapter_count, path.chapterCount, path.chapterCount) + " · " +
                    pluralStringResource(Res.plurals.paths_lesson_count, path.lessonCount, path.lessonCount),
                style = MaterialTheme.typography.bodySmall,
                color = BookWormColors.InkMuted,
            )
            if (path.assignedTo.isEmpty()) {
                Chip(stringResource(Res.string.paths_no_followers), BookWormColors.SunSoft, TeacherColors.SunInk)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    path.assignedTo.forEach { Chip(targetLabel(library, it), BookWormColors.PrimarySoft, BookWormColors.PrimaryDark) }
                }
            }
        }
    }
}

@Composable
private fun EmptyEditor(onNew: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(BookWormColors.Surface)
            .dashedBorder(BookWormColors.Outline)
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Outlined.Route, contentDescription = null, tint = BookWormColors.PrimaryDark)
        Text(stringResource(Res.string.paths_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(stringResource(Res.string.paths_empty_body), style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted, textAlign = TextAlign.Center)
        BookWormButton(stringResource(Res.string.paths_new), onNew)
    }
}

// Editor

@Composable
private fun Editor(
    library: CurriculumLibrary,
    draft: Curriculum,
    state: CurriculumUiState,
    viewModel: CurriculumViewModel,
    onEditLesson: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookWormTextField(
                value = draft.name,
                onValueChange = viewModel::rename,
                label = stringResource(Res.string.paths_name),
                modifier = Modifier.weight(1f),
            )
            RoundIconButton(
                Icons.Outlined.Delete,
                contentDescription = stringResource(Res.string.paths_delete),
                onClick = { viewModel.askDelete(true) },
                modifier = Modifier.padding(bottom = 2.dp),
            )
            BookWormButton(stringResource(Res.string.paths_save), viewModel::save, enabled = state.canSave)
        }
        Text(subjectGrade(library, draft.subjectId, draft.grade), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        if (state.justSaved) {
            Callout(
                text = stringResource(Res.string.paths_saved),
                icon = Icons.Outlined.CheckCircle,
                background = BookWormColors.SuccessSoft,
                contentColor = BookWormColors.SuccessDark,
            )
        }

        // Progress needs a saved path to count against.
        if (draft.id.isNotEmpty()) {
            SegmentedControl(
                options = listOf(stringResource(Res.string.paths_tab_edit), stringResource(Res.string.paths_tab_progress)),
                selectedIndex = if (state.showProgress) 1 else 0,
                onSelect = { viewModel.showProgress(it == 1) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.showProgress && draft.id.isNotEmpty()) {
            Progress(state.progress, dirty = state.dirty, onRetry = viewModel::loadProgress)
            return@Column
        }

        Followers(library, draft, onToggle = viewModel::toggleTarget)

        SectionCard(stringResource(Res.string.paths_mode)) {
            val modes = UnlockMode.entries
            SegmentedControl(
                options = listOf(stringResource(Res.string.paths_mode_in_order), stringResource(Res.string.paths_mode_any)),
                selectedIndex = modes.indexOf(draft.unlockMode),
                onSelect = { viewModel.setUnlockMode(modes[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(if (draft.unlockMode == UnlockMode.InOrder) Res.string.paths_mode_in_order_hint else Res.string.paths_mode_any_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = BookWormColors.InkMuted,
            )
        }

        Chapters(library, draft, viewModel, onEditLesson)
    }
}

/** How far each student of the classrooms following the path has got. */
@Composable
private fun Progress(progress: UiState<PathProgress>?, dirty: Boolean, onRetry: () -> Unit) {
    if (dirty) {
        Text(stringResource(Res.string.paths_progress_unsaved), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
    }
    StateContent(progress ?: UiState.Loading, onRetry = onRetry) { data ->
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (data.classrooms.isEmpty()) {
                SectionCard(stringResource(Res.string.paths_tab_progress)) {
                    Text(stringResource(Res.string.paths_progress_no_classrooms), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                }
            }
            data.classrooms.forEach { room ->
                SectionCard(room.classroomName) {
                    if (room.students.isEmpty()) {
                        Text(stringResource(Res.string.paths_progress_no_students), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                    }
                    room.students.forEach { StudentProgressRow(it, data) }
                }
            }
        }
    }
}

@Composable
private fun StudentProgressRow(student: StudentProgress, path: PathProgress) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(student.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp), modifier = Modifier.width(180.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BookWormProgressBar(if (path.lessonCount == 0) 0f else student.lessonsDone.toFloat() / path.lessonCount, height = 8.dp)
            Text(
                stringResource(Res.string.paths_progress_lessons, student.lessonsDone, path.lessonCount, student.currentChapter, path.chapterCount),
                style = MaterialTheme.typography.bodySmall,
                color = BookWormColors.InkMuted,
            )
        }
        Text(
            pluralStringResource(Res.plurals.paths_progress_stars, student.stars, student.stars),
            style = MaterialTheme.typography.bodySmall,
            color = BookWormColors.InkMuted,
            modifier = Modifier.width(90.dp),
        )
    }
}

/** Which classrooms follow the path. */
@Composable
private fun Followers(library: CurriculumLibrary, draft: Curriculum, onToggle: (classroomId: String) -> Unit) {
    val subjectName = library.subjects.firstOrNull { it.id == draft.subjectId }?.name.orEmpty()
    // What each classroom follows now for this class, if it isn't this path.
    val current = library.curricula.filter { it.subjectId == draft.subjectId && it.id != draft.id }
    fun otherPath(classroomId: String) = current.firstOrNull { classroomId in it.assignedTo }?.name

    SectionCard(stringResource(Res.string.paths_followers)) {
        Text(stringResource(Res.string.paths_followers_hint, subjectName), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        library.classes.forEach { classroom ->
            FollowerRow(
                label = classroom.name,
                checked = classroom.id in draft.assignedTo,
                otherPath = otherPath(classroom.id).takeIf { classroom.id !in draft.assignedTo },
                onToggle = { onToggle(classroom.id) },
            )
        }
    }
}

@Composable
private fun FollowerRow(
    label: String,
    checked: Boolean,
    otherPath: String?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledCheckbox(label, checked, onCheckedChange = { onToggle() })
        if (otherPath != null) {
            Text(stringResource(Res.string.paths_now_follows, otherPath), style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
        }
    }
}

@Composable
private fun Chapters(library: CurriculumLibrary, draft: Curriculum, viewModel: CurriculumViewModel, onEditLesson: (String) -> Unit) {
    val practise = stringResource(Res.string.paths_default_practise)
    val quiz = stringResource(Res.string.paths_default_quiz)
    val newLesson = stringResource(Res.string.paths_default_lesson)

    SectionCard(stringResource(Res.string.paths_chapters)) {
        Text(stringResource(Res.string.paths_chapters_hint), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        if (draft.units.isEmpty()) {
            Text(stringResource(Res.string.paths_no_chapters), style = MaterialTheme.typography.bodyLarge, color = TeacherColors.SunInk)
        }
        draft.units.forEachIndexed { unitIndex, unit ->
            UnitCard(
                position = unitIndex + 1,
                unit = unit,
                isFirst = unitIndex == 0,
                isLast = unitIndex == draft.units.lastIndex,
                onMove = { viewModel.moveUnit(unitIndex, it) },
                onRemove = { viewModel.removeUnit(unitIndex) },
                onAddLesson = { viewModel.addLesson(unitIndex, newLesson) },
                onRenameLesson = { lesson, title -> viewModel.renameLesson(unitIndex, lesson, title) },
                onToggleQuiz = { viewModel.toggleQuiz(unitIndex, it) },
                onLessonStars = { lesson, stars -> viewModel.setLessonStars(unitIndex, lesson, stars) },
                onMoveLesson = { lesson, by -> viewModel.moveLesson(unitIndex, lesson, by) },
                onRemoveLesson = { viewModel.removeLesson(unitIndex, it) },
                onEditLesson = onEditLesson,
            )
        }

        // Add an existing shared chapter, or create a new one.
        val available = library.chapters.filter { chapter -> chapter.subjectId == draft.subjectId && draft.units.none { it.chapter.id == chapter.id } }
        var pickedId by remember(available) { mutableStateOf(available.firstOrNull()?.id) }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookWormDropdown(
                label = stringResource(Res.string.paths_chapter_pick),
                options = available,
                selected = available.firstOrNull { it.id == pickedId },
                onSelect = { pickedId = it.id },
                optionLabel = { stringResource(Res.string.paths_chapter_option, it.number, it.title) },
                modifier = Modifier.weight(1f),
                enabled = available.isNotEmpty(),
            )
            SmallButton(
                stringResource(Res.string.paths_add_chapter),
                onClick = { available.firstOrNull { it.id == pickedId }?.let { viewModel.addChapter(it, practise, quiz) } },
                enabled = pickedId != null,
                leadingIcon = Icons.Rounded.Add,
            )
        }
        var newTitle by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookWormTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = stringResource(Res.string.paths_new_chapter),
                placeholder = stringResource(Res.string.paths_new_chapter_hint),
                modifier = Modifier.weight(1f),
            )
            SmallButton(
                stringResource(Res.string.paths_create_chapter),
                onClick = {
                    viewModel.createChapter(newTitle, practise, quiz)
                    newTitle = ""
                },
                variant = ButtonVariant.Secondary,
                enabled = newTitle.isNotBlank(),
            )
        }
    }
}

@Composable
private fun UnitCard(
    position: Int,
    unit: CurriculumUnit,
    isFirst: Boolean,
    isLast: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onAddLesson: () -> Unit,
    onRenameLesson: (Int, String) -> Unit,
    onToggleQuiz: (Int) -> Unit,
    onLessonStars: (Int, String) -> Unit,
    onMoveLesson: (Int, Int) -> Unit,
    onRemoveLesson: (Int) -> Unit,
    onEditLesson: (String) -> Unit,
) {
    val chapterLabel = stringResource(Res.string.paths_chapter_option, unit.chapter.number, unit.chapter.title)
    BookWormCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BadgeTile("$position", size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(chapterLabel, style = MaterialTheme.typography.titleSmall.copy(fontSize = 18.sp))
                Text(
                    pluralStringResource(Res.plurals.paths_chapter_cards, unit.chapter.flashcardCount, unit.chapter.flashcardCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = BookWormColors.InkMuted,
                )
            }
            RoundIconButton(Icons.Rounded.ArrowUpward, stringResource(Res.string.paths_move_up, chapterLabel), { onMove(-1) }, enabled = !isFirst)
            RoundIconButton(Icons.Rounded.ArrowDownward, stringResource(Res.string.paths_move_down, chapterLabel), { onMove(1) }, enabled = !isLast)
            RoundIconButton(Icons.Outlined.Delete, stringResource(Res.string.paths_remove, chapterLabel), onRemove)
        }
        unit.lessons.forEachIndexed { index, lesson ->
            LessonRow(
                lesson = lesson,
                number = index + 1,
                isFirst = index == 0,
                isLast = index == unit.lessons.lastIndex,
                onRename = { onRenameLesson(index, it) },
                onToggleQuiz = { onToggleQuiz(index) },
                onStars = { onLessonStars(index, it) },
                onMove = { onMoveLesson(index, it) },
                onRemove = { onRemoveLesson(index) },
                onEdit = { onEditLesson(lesson.id) },
            )
        }
        SmallButton(stringResource(Res.string.paths_add_lesson), onAddLesson, variant = ButtonVariant.Secondary, leadingIcon = Icons.Rounded.Add)
    }
}

@Composable
private fun LessonRow(
    lesson: CurriculumLesson,
    number: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onRename: (String) -> Unit,
    onToggleQuiz: () -> Unit,
    onStars: (String) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
) {
    val title = lesson.title.ifBlank { stringResource(Res.string.paths_lesson_title) }
    Row(
        Modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Lesson number, or a trophy for the chapter quiz, as on the student's path.
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (lesson.isChapterQuiz) BookWormColors.SunSoft else BookWormColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            if (lesson.isChapterQuiz) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = BookWormColors.SunDark, modifier = Modifier.size(16.dp))
            } else {
                Text("$number", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), color = BookWormColors.PrimaryDark)
            }
        }
        BookWormTextField(
            value = lesson.title,
            onValueChange = onRename,
            placeholder = stringResource(Res.string.paths_lesson_title),
            modifier = Modifier.weight(1f),
        )
        // What students get: the written questions, or practice from the chapter's flashcards.
        Text(
            if (lesson.questionCount > 0) {
                pluralStringResource(Res.plurals.paths_question_count, lesson.questionCount, lesson.questionCount)
            } else {
                stringResource(Res.string.paths_from_flashcards)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (lesson.questionCount > 0) BookWormColors.SuccessDark else BookWormColors.InkMuted,
            modifier = Modifier.width(96.dp),
        )
        // Stars the student gets the first time they finish the lesson.
        val starsLabel = stringResource(Res.string.paths_lesson_stars, title)
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = starsLabel },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = BookWormColors.Sun, modifier = Modifier.size(20.dp))
            BookWormTextField(
                value = "${lesson.stars}",
                onValueChange = onStars,
                modifier = Modifier.width(64.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        // Lessons get an id when the path is saved; only then can their questions be written.
        RoundIconButton(
            Icons.Outlined.EditNote,
            stringResource(if (lesson.id.isEmpty()) Res.string.paths_save_first else Res.string.paths_edit_questions, title),
            onEdit,
            enabled = lesson.id.isNotEmpty(),
            tint = BookWormColors.PrimaryDark,
        )
        RoundIconButton(
            Icons.Rounded.EmojiEvents,
            stringResource(if (lesson.isChapterQuiz) Res.string.paths_quiz_on else Res.string.paths_quiz_off, title),
            onToggleQuiz,
            tint = if (lesson.isChapterQuiz) BookWormColors.SunDark else BookWormColors.TrackShadow,
        )
        RoundIconButton(Icons.Rounded.ArrowUpward, stringResource(Res.string.paths_move_up, title), { onMove(-1) }, enabled = !isFirst)
        RoundIconButton(Icons.Rounded.ArrowDownward, stringResource(Res.string.paths_move_down, title), { onMove(1) }, enabled = !isLast)
        RoundIconButton(Icons.Rounded.Close, stringResource(Res.string.paths_remove, title), onRemove)
    }
}

// Dialogs and helpers

@Composable
private fun NewPathDialog(
    library: CurriculumLibrary,
    onCreate: (name: String, subjectId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf(library.subjects.firstOrNull()?.id) }
    BookWormFormDialog(
        title = stringResource(Res.string.paths_new_title),
        onDismissRequest = onDismiss,
        confirmText = stringResource(Res.string.teacher_create),
        onConfirm = { subjectId?.let { onCreate(name, it) } },
        dismissText = stringResource(Res.string.teacher_cancel),
        confirmEnabled = name.isNotBlank() && subjectId != null,
    ) {
        BookWormTextField(value = name, onValueChange = { name = it }, label = stringResource(Res.string.paths_name))
        BookWormDropdown(
            label = stringResource(Res.string.paths_class),
            options = library.subjects,
            selected = library.subjects.firstOrNull { it.id == subjectId },
            onSelect = { subjectId = it.id },
            optionLabel = { it.name },
        )
    }
}

/** "Maths 4 · Grade 4", or just the class name before a new path is saved (its grade comes from the class). */
@Composable
private fun subjectGrade(library: CurriculumLibrary, subjectId: String, grade: Int): String {
    val name = library.subjects.firstOrNull { it.id == subjectId }?.name.orEmpty()
    return if (grade > 0) stringResource(Res.string.paths_subject_grade, name, grade) else name
}

@Composable
private fun targetLabel(library: CurriculumLibrary, classroomId: String): String =
    library.classes.firstOrNull { it.id == classroomId }?.name.orEmpty()

@Composable
private fun Chip(text: String, background: Color, content: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
        color = content,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(background).padding(horizontal = 10.dp, vertical = 2.dp),
    )
}
