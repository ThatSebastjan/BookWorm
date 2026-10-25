package com.dreamteam.bookworm.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.AppContainer
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.TeacherProfile
import com.dreamteam.bookworm.domain.repository.TeacherRepository
import com.dreamteam.bookworm.navigation.ScopedViewModelStore
import com.dreamteam.bookworm.teacher.components.BookWormDropdown
import com.dreamteam.bookworm.teacher.components.BookWormFormDialog
import com.dreamteam.bookworm.teacher.navigation.QuizDestination
import com.dreamteam.bookworm.teacher.navigation.TeacherSection
import com.dreamteam.bookworm.teacher.platform.LocalTeacherPlatform
import com.dreamteam.bookworm.teacher.platform.NoTeacherPlatform
import com.dreamteam.bookworm.teacher.platform.TeacherPlatform
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.app_name
import com.dreamteam.bookworm.teacher.resources.profile_grade
import com.dreamteam.bookworm.teacher.resources.teacher_cancel
import com.dreamteam.bookworm.teacher.resources.teacher_cards_to_review
import com.dreamteam.bookworm.teacher.resources.teacher_class_grade
import com.dreamteam.bookworm.teacher.resources.teacher_class_name
import com.dreamteam.bookworm.teacher.resources.teacher_class_name_hint
import com.dreamteam.bookworm.teacher.resources.teacher_class_students
import com.dreamteam.bookworm.teacher.resources.teacher_create
import com.dreamteam.bookworm.teacher.resources.teacher_for_teachers
import com.dreamteam.bookworm.teacher.resources.teacher_new_class
import com.dreamteam.bookworm.teacher.resources.teacher_new_class_title
import com.dreamteam.bookworm.teacher.resources.teacher_switch_class
import com.dreamteam.bookworm.teacher.screens.classroom.ClassroomScreen
import com.dreamteam.bookworm.teacher.screens.curriculum.CurriculumScreen
import com.dreamteam.bookworm.teacher.screens.live.LiveQuizScreen
import com.dreamteam.bookworm.teacher.screens.materials.MaterialsScreen
import com.dreamteam.bookworm.teacher.screens.quizsetup.QuizSetupScreen
import com.dreamteam.bookworm.teacher.screens.reveal.RevealScreen
import com.dreamteam.bookworm.teacher.screens.review.ReviewScreen
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.BookWormTheme
import com.dreamteam.bookworm.ui_components.LoadingView
import com.dreamteam.bookworm.ui_components.MascotIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Loads the teacher shown in the sidebar and the "cards to review" badge, and
 * keeps track of which of the teacher's classes the class-specific screens show.
 */
class TeacherShellViewModel(private val repository: TeacherRepository) : ViewModel() {

    private val _teacher = MutableStateFlow<UiState<TeacherProfile>>(UiState.Loading)
    val teacher: StateFlow<UiState<TeacherProfile>> = _teacher.asStateFlow()

    private val _classId = MutableStateFlow<String?>(null)

    /** The class Classroom and Pop quiz work with; the first class until the teacher picks another. */
    val classId: StateFlow<String?> = _classId.asStateFlow()

    private val _createClassFailed = MutableStateFlow(false)
    val createClassFailed: StateFlow<Boolean> = _createClassFailed.asStateFlow()

    init {
        refresh()
    }

    /** Keeps showing the last profile while refreshing, so the badge doesn't flicker. */
    fun refresh() {
        viewModelScope.launch {
            val loaded = loadState { repository.getTeacher() }
            if (loaded is UiState.Success || _teacher.value !is UiState.Success) _teacher.value = loaded
            val classes = (loaded as? UiState.Success)?.data?.classes ?: return@launch
            if (classes.none { it.id == _classId.value }) _classId.value = classes.firstOrNull()?.id
        }
    }

    fun selectClass(classId: String) {
        _classId.value = classId
    }

    fun createClass(name: String, grade: Int) {
        viewModelScope.launch {
            when (val created = loadState { repository.createClass(name.trim(), grade) }) {
                is UiState.Success -> {
                    _createClassFailed.value = false
                    _classId.value = created.data.id
                    refresh()
                }
                is UiState.Error -> _createClassFailed.value = true
                UiState.Loading -> Unit
            }
        }
    }
}

/** The teacher desktop app: a sidebar with the sections and a class switcher, and full-window pop quiz screens. */
@Composable
fun TeacherApp(
    platform: TeacherPlatform = NoTeacherPlatform,
    container: AppContainer = remember { AppContainer.fake() },
) {
    CompositionLocalProvider(LocalAppContainer provides container, LocalTeacherPlatform provides platform) {
        BookWormTheme {
            ScopedViewModelStore("teacher") {
                val shell = viewModel { TeacherShellViewModel(container.teacherRepository) }
                val teacher by shell.teacher.collectAsStateWithLifecycle()
                val classId by shell.classId.collectAsStateWithLifecycle()
                var showNewClass by remember { mutableStateOf(false) }

                var section by rememberSaveable { mutableStateOf(TeacherSection.Materials) }
                var reviewUploadId by remember { mutableStateOf<String?>(null) }
                var quiz by remember { mutableStateOf<QuizDestination?>(null) }

                fun open(target: TeacherSection) {
                    quiz = null
                    if (target == TeacherSection.Flashcards) reviewUploadId = null
                    section = target
                }

                LaunchedEffect(section, quiz) { shell.refresh() }

                when (val current = quiz) {
                    is QuizDestination.Live -> ScopedViewModelStore(current) {
                        LiveQuizScreen(
                            quizId = current.quizId,
                            onShowResults = { quiz = QuizDestination.Reveal(current.quizId) },
                            onEnd = { open(TeacherSection.Classroom) },
                        )
                    }
                    is QuizDestination.Reveal -> ScopedViewModelStore(current) {
                        RevealScreen(quizId = current.quizId, onEnd = { open(TeacherSection.Classroom) })
                    }
                    null -> Row(Modifier.fillMaxSize().background(BookWormColors.Background)) {
                        Sidebar(
                            current = section,
                            teacher = (teacher as? UiState.Success)?.data,
                            classId = classId,
                            onSelect = ::open,
                            onSelectClass = shell::selectClass,
                            onNewClass = { showNewClass = true },
                        )
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            // Class screens reload when the teacher switches class.
                            ScopedViewModelStore(listOf(section, reviewUploadId, classId)) {
                                when (section) {
                                    TeacherSection.Materials -> MaterialsScreen(
                                        onReview = { uploadId ->
                                            reviewUploadId = uploadId
                                            section = TeacherSection.Flashcards
                                        },
                                    )
                                    TeacherSection.Flashcards -> ReviewScreen(
                                        uploadId = reviewUploadId,
                                        onCardsChanged = shell::refresh,
                                        onUpload = { open(TeacherSection.Materials) },
                                    )
                                    TeacherSection.LearningPaths -> CurriculumScreen()
                                    TeacherSection.Classroom -> classId?.let { id ->
                                        ClassroomScreen(
                                            classId = id,
                                            onSetUpQuiz = { open(TeacherSection.PopQuiz) },
                                            onManagePaths = { open(TeacherSection.LearningPaths) },
                                        )
                                    } ?: LoadingView()
                                    TeacherSection.PopQuiz -> classId?.let { id ->
                                        QuizSetupScreen(
                                            classId = id,
                                            onStarted = { quiz = QuizDestination.Live(it) },
                                        )
                                    } ?: LoadingView()
                                }
                            }
                        }
                    }
                }

                if (showNewClass) {
                    NewClassDialog(
                        onCreate = { name, grade ->
                            shell.createClass(name, grade)
                            showNewClass = false
                        },
                        onDismiss = { showNewClass = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    current: TeacherSection,
    teacher: TeacherProfile?,
    classId: String?,
    onSelect: (TeacherSection) -> Unit,
    onSelectClass: (String) -> Unit,
    onNewClass: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(248.dp)
            .fillMaxHeight()
            .background(BookWormColors.Surface)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRect(BookWormColors.Divider, topLeft = Offset(size.width - stroke, 0f), size = size.copy(width = stroke))
            }
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MascotIcon(Modifier.size(40.dp))
            Column {
                Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
                Text(stringResource(Res.string.teacher_for_teachers), style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
            }
        }

        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TeacherSection.entries.forEach { section ->
                val badge = if (section == TeacherSection.Flashcards) teacher?.cardsToReview?.takeIf { it > 0 } else null
                SidebarItem(section, selected = section == current, badge = badge, onClick = { onSelect(section) })
            }
        }

        Box(Modifier.weight(1f))

        if (teacher != null) {
            ClassSwitcher(teacher, classId, onSelectClass, onNewClass)
        }
    }
}

/** The teacher card at the bottom of the sidebar; opens a menu of the teacher's classes. */
@Composable
private fun ClassSwitcher(
    teacher: TeacherProfile,
    classId: String?,
    onSelectClass: (String) -> Unit,
    onNewClass: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = teacher.classes.firstOrNull { it.id == classId }
    val switchLabel = stringResource(Res.string.teacher_switch_class)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BookWormColors.Background)
                .clickable(role = Role.DropdownList, onClickLabel = switchLabel) { expanded = true }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(BookWormColors.PrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(teacher.name.take(1), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = BookWormColors.PrimaryDark)
            }
            Column(Modifier.weight(1f)) {
                Text(teacher.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp))
                Text(current?.name.orEmpty(), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = BookWormColors.InkMuted)
            }
            Icon(Icons.Rounded.UnfoldMore, contentDescription = null, tint = BookWormColors.InkMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = BookWormColors.Surface) {
            teacher.classes.forEach { schoolClass ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(schoolClass.name, style = MaterialTheme.typography.labelMedium)
                            Text(
                                pluralStringResource(Res.plurals.teacher_class_students, schoolClass.studentCount, schoolClass.studentCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = BookWormColors.InkMuted,
                            )
                        }
                    },
                    leadingIcon = if (schoolClass.id == classId) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = BookWormColors.PrimaryDark) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelectClass(schoolClass.id)
                    },
                )
            }
            HorizontalDivider(color = BookWormColors.Divider)
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.teacher_new_class), style = MaterialTheme.typography.labelMedium, color = BookWormColors.PrimaryDark) },
                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, tint = BookWormColors.PrimaryDark) },
                onClick = {
                    expanded = false
                    onNewClass()
                },
            )
        }
    }
}

@Composable
private fun NewClassDialog(onCreate: (name: String, grade: Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf(3) }
    BookWormFormDialog(
        title = stringResource(Res.string.teacher_new_class_title),
        onDismissRequest = onDismiss,
        confirmText = stringResource(Res.string.teacher_create),
        onConfirm = { onCreate(name, grade) },
        dismissText = stringResource(Res.string.teacher_cancel),
        confirmEnabled = name.isNotBlank(),
    ) {
        BookWormTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(Res.string.teacher_class_name),
            placeholder = stringResource(Res.string.teacher_class_name_hint),
        )
        BookWormDropdown(
            label = stringResource(Res.string.teacher_class_grade),
            options = (1..9).toList(),
            selected = grade,
            onSelect = { grade = it },
            optionLabel = { stringResource(Res.string.profile_grade, it) },
        )
    }
}

@Composable
private fun SidebarItem(
    section: TeacherSection,
    selected: Boolean,
    badge: Int?,
    onClick: () -> Unit,
) {
    val color = if (selected) BookWormColors.PrimaryDark else BookWormColors.InkMuted
    val badgeDescription = badge?.let { pluralStringResource(Res.plurals.teacher_cards_to_review, it, it) }
    val label = stringResource(section.label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BookWormColors.PrimarySoft else BookWormColors.Surface)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (badgeDescription != null) "$label, $badgeDescription" else label
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(section.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp), color = color, modifier = Modifier.weight(1f))
        if (badge != null) {
            Box(
                Modifier
                    .widthIn(min = 24.dp)
                    .heightIn(min = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BookWormColors.Sun)
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("$badge", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), color = BookWormColors.Ink)
            }
        }
    }
}
