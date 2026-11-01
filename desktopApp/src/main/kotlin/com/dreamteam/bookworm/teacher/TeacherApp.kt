package com.dreamteam.bookworm.teacher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.dreamteam.bookworm.data.remote.PreferencesSessionStore
import com.dreamteam.bookworm.di.AppContainer
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.GRADES
import com.dreamteam.bookworm.domain.model.TeacherProfile
import com.dreamteam.bookworm.domain.model.UserRole
import com.dreamteam.bookworm.domain.repository.TeacherRepository
import com.dreamteam.bookworm.navigation.ScopedViewModelStore
import com.dreamteam.bookworm.teacher.components.BookWormDropdown
import com.dreamteam.bookworm.teacher.components.BookWormFormDialog
import com.dreamteam.bookworm.teacher.navigation.TeacherSection
import com.dreamteam.bookworm.teacher.platform.LocalTeacherPlatform
import com.dreamteam.bookworm.teacher.platform.NoTeacherPlatform
import com.dreamteam.bookworm.teacher.platform.TeacherPlatform
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.app_name
import com.dreamteam.bookworm.teacher.resources.profile_grade
import com.dreamteam.bookworm.teacher.resources.teacher_cancel
import com.dreamteam.bookworm.teacher.resources.teacher_no_classroom
import com.dreamteam.bookworm.teacher.resources.teacher_class_grade
import com.dreamteam.bookworm.teacher.resources.teacher_class_name
import com.dreamteam.bookworm.teacher.resources.teacher_class_name_hint
import com.dreamteam.bookworm.teacher.resources.teacher_class_students
import com.dreamteam.bookworm.teacher.resources.teacher_create
import com.dreamteam.bookworm.teacher.resources.teacher_for_teachers
import com.dreamteam.bookworm.teacher.resources.teacher_log_out
import com.dreamteam.bookworm.teacher.resources.teacher_new_class
import com.dreamteam.bookworm.teacher.resources.teacher_new_class_title
import com.dreamteam.bookworm.teacher.resources.teacher_switch_class
import com.dreamteam.bookworm.teacher.screens.classroom.ClassroomScreen
import com.dreamteam.bookworm.teacher.screens.curriculum.CurriculumScreen
import com.dreamteam.bookworm.teacher.screens.login.TeacherLoginScreen
import com.dreamteam.bookworm.teacher.screens.login.TeacherRegisterScreen
import com.dreamteam.bookworm.teacher.screens.account.AccountScreen
import com.dreamteam.bookworm.teacher.screens.classes.ClassesScreen
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormButton
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
 * Loads the teacher and their classrooms for the sidebar, and keeps track of which
 * classroom the Classroom screen shows.
 */
class TeacherShellViewModel(private val repository: TeacherRepository) : ViewModel() {

    private val _teacher = MutableStateFlow<UiState<TeacherProfile>>(UiState.Loading)
    val teacher: StateFlow<UiState<TeacherProfile>> = _teacher.asStateFlow()

    private val _classId = MutableStateFlow<String?>(null)

    /** The classroom the Classroom screen shows; the first one until the teacher picks another. */
    val classId: StateFlow<String?> = _classId.asStateFlow()

    private val _createClassFailed = MutableStateFlow(false)
    val createClassFailed: StateFlow<Boolean> = _createClassFailed.asStateFlow()

    init {
        refresh()
    }

    /** Keeps showing the last profile while refreshing, so the sidebar doesn't flicker. */
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

    fun createClassroom(name: String, grade: Int) {
        viewModelScope.launch {
            when (val created = loadState { repository.createClassroom(name.trim(), grade) }) {
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

/**
 * The teacher desktop app: sign-in first, then a sidebar with the sections and a class switcher.
 * Only teacher accounts can sign in.
 */
@Composable
fun TeacherApp(
    platform: TeacherPlatform = NoTeacherPlatform,
    container: AppContainer = remember {
        AppContainer.create(sessionStore = PreferencesSessionStore(), requiredRole = UserRole.Teacher)
    },
) {
    // A session remembered from last time shows the app straight away; this checks it's still valid.
    LaunchedEffect(container) { container.authRepository.verifySession() }
    CompositionLocalProvider(LocalAppContainer provides container, LocalTeacherPlatform provides platform) {
        BookWormTheme {
            val session by container.authRepository.session.collectAsStateWithLifecycle()
            val signedIn = session
            val scope = rememberCoroutineScope()
            // A fresh ViewModel store per teacher, so nothing from a previous account lingers after logging out.
            var registering by rememberSaveable { mutableStateOf(false) }
            if (signedIn == null) {
                if (registering) {
                    ScopedViewModelStore("register") { TeacherRegisterScreen(onLoginClick = { registering = false }) }
                } else {
                    ScopedViewModelStore("login") { TeacherLoginScreen(onRegisterClick = { registering = true }) }
                }
            } else ScopedViewModelStore(signedIn.userId) {
                val shell = viewModel { TeacherShellViewModel(container.teacherRepository) }
                val teacher by shell.teacher.collectAsStateWithLifecycle()
                val classId by shell.classId.collectAsStateWithLifecycle()
                var showNewClass by remember { mutableStateOf(false) }

                var section by rememberSaveable { mutableStateOf(TeacherSection.Classes) }
                fun open(target: TeacherSection) {
                    section = target
                }

                LaunchedEffect(section) { shell.refresh() }

                Row(Modifier.fillMaxSize().background(BookWormColors.Background)) {
                    Sidebar(
                        current = section,
                        teacher = (teacher as? UiState.Success)?.data,
                        teacherName = "${signedIn.name} ${signedIn.surname}",
                        classId = classId,
                        onSelect = ::open,
                        onSelectClass = shell::selectClass,
                        onNewClass = { showNewClass = true },
                        onLogout = { scope.launch { container.authRepository.logout() } },
                    )
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        // Class screens reload when the teacher switches class.
                        ScopedViewModelStore(listOf(section, classId)) {
                            when (section) {
                                TeacherSection.Classes -> ClassesScreen()
                                TeacherSection.Profile -> AccountScreen(
                                    onOpenClassroom = { id ->
                                        shell.selectClass(id)
                                        open(TeacherSection.Classroom)
                                    },
                                    onLogout = { scope.launch { container.authRepository.logout() } },
                                )
                                TeacherSection.LearningPaths -> CurriculumScreen()
                                TeacherSection.Classroom -> classId?.let { id ->
                                    ClassroomScreen(
                                        classId = id,
                                        onManagePaths = { open(TeacherSection.LearningPaths) },
                                        onManageClasses = { open(TeacherSection.Classes) },
                                        // They left a classroom they co-taught: show another one.
                                        onLeft = shell::refresh,
                                    )
                                } ?: NoClassroom(teacher, onNew = { showNewClass = true })
                            }
                        }
                    }
                }

                if (showNewClass) {
                    NewClassDialog(
                        onCreate = { name, grade ->
                            shell.createClassroom(name, grade)
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
    teacherName: String,
    classId: String?,
    onSelect: (TeacherSection) -> Unit,
    onSelectClass: (String) -> Unit,
    onNewClass: () -> Unit,
    onLogout: () -> Unit,
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
                SidebarItem(section, selected = section == current, onClick = { onSelect(section) })
            }
        }

        Box(Modifier.weight(1f))

        if (teacher != null) {
            ClassSwitcher(teacher, teacherName, classId, onSelectClass, onNewClass, onLogout)
        }
    }
}

/** The teacher card at the bottom of the sidebar; opens a menu of the teacher's classes and logging out. */
@Composable
private fun ClassSwitcher(
    teacher: TeacherProfile,
    teacherName: String,
    classId: String?,
    onSelectClass: (String) -> Unit,
    onNewClass: () -> Unit,
    onLogout: () -> Unit,
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
                Text(teacherName.take(1), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = BookWormColors.PrimaryDark)
            }
            Column(Modifier.weight(1f)) {
                Text(teacherName, style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp))
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
            HorizontalDivider(color = BookWormColors.Divider)
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.teacher_log_out), style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = BookWormColors.InkMuted) },
                onClick = {
                    expanded = false
                    onLogout()
                },
            )
        }
    }
}

@Composable
private fun NewClassDialog(onCreate: (name: String, grade: Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf(GRADES.first) }
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
            options = GRADES.toList(),
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
    onClick: () -> Unit,
) {
    val color = if (selected) BookWormColors.PrimaryDark else BookWormColors.InkMuted
    val label = stringResource(section.label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BookWormColors.PrimarySoft else BookWormColors.Surface)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(section.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp), color = color, modifier = Modifier.weight(1f))
    }
}

/** The Classroom screen needs a classroom: loading until the teacher is known, then a prompt to create one. */
@Composable
private fun NoClassroom(teacher: UiState<TeacherProfile>, onNew: () -> Unit) {
    if (teacher !is UiState.Success) {
        LoadingView()
        return
    }
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MascotIcon(Modifier.size(72.dp))
        Text(stringResource(Res.string.teacher_no_classroom), style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted)
        BookWormButton(stringResource(Res.string.teacher_new_class_title), onNew, leadingIcon = Icons.Rounded.Add)
    }
}
