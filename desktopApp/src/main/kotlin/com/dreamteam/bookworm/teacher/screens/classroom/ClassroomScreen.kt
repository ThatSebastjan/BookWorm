package com.dreamteam.bookworm.teacher.screens.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.AddTeacherFailure
import com.dreamteam.bookworm.domain.model.ClassStudent
import com.dreamteam.bookworm.domain.model.Classroom
import com.dreamteam.bookworm.domain.model.StudentStatus
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
import com.dreamteam.bookworm.teacher.resources.classroom_action_failed
import com.dreamteam.bookworm.teacher.resources.classroom_add_body
import com.dreamteam.bookworm.teacher.resources.classroom_add_students
import com.dreamteam.bookworm.teacher.resources.classroom_add_teacher
import com.dreamteam.bookworm.teacher.resources.classroom_add_teacher_hint
import com.dreamteam.bookworm.teacher.resources.classroom_approve_joins
import com.dreamteam.bookworm.teacher.resources.classroom_classes
import com.dreamteam.bookworm.teacher.resources.classroom_classes_hint
import com.dreamteam.bookworm.teacher.resources.classroom_close
import com.dreamteam.bookworm.teacher.resources.classroom_copy_link
import com.dreamteam.bookworm.teacher.resources.classroom_decline
import com.dreamteam.bookworm.teacher.resources.classroom_decline_body
import com.dreamteam.bookworm.teacher.resources.classroom_decline_title
import com.dreamteam.bookworm.teacher.resources.classroom_find
import com.dreamteam.bookworm.teacher.resources.classroom_join_code
import com.dreamteam.bookworm.teacher.resources.classroom_leave
import com.dreamteam.bookworm.teacher.resources.classroom_leave_body
import com.dreamteam.bookworm.teacher.resources.classroom_leave_title
import com.dreamteam.bookworm.teacher.resources.classroom_let_in
import com.dreamteam.bookworm.teacher.resources.classroom_link_copied
import com.dreamteam.bookworm.teacher.resources.classroom_manage_classes
import com.dreamteam.bookworm.teacher.resources.classroom_manage_paths
import com.dreamteam.bookworm.teacher.resources.classroom_no_classes
import com.dreamteam.bookworm.teacher.resources.classroom_no_match
import com.dreamteam.bookworm.teacher.resources.classroom_no_paths
import com.dreamteam.bookworm.teacher.resources.classroom_other_class
import com.dreamteam.bookworm.teacher.resources.classroom_other_classes
import com.dreamteam.bookworm.teacher.resources.classroom_owner
import com.dreamteam.bookworm.teacher.resources.classroom_paths
import com.dreamteam.bookworm.teacher.resources.classroom_print_code
import com.dreamteam.bookworm.teacher.resources.classroom_qr
import com.dreamteam.bookworm.teacher.resources.classroom_remove
import com.dreamteam.bookworm.teacher.resources.classroom_remove_body
import com.dreamteam.bookworm.teacher.resources.classroom_remove_teacher
import com.dreamteam.bookworm.teacher.resources.classroom_remove_teacher_body
import com.dreamteam.bookworm.teacher.resources.classroom_remove_teacher_title
import com.dreamteam.bookworm.teacher.resources.classroom_remove_title
import com.dreamteam.bookworm.teacher.resources.classroom_status_in
import com.dreamteam.bookworm.teacher.resources.classroom_status_offline
import com.dreamteam.bookworm.teacher.resources.classroom_status_waiting
import com.dreamteam.bookworm.teacher.resources.classroom_students
import com.dreamteam.bookworm.teacher.resources.classroom_summary
import com.dreamteam.bookworm.teacher.resources.classroom_teacher_already
import com.dreamteam.bookworm.teacher.resources.classroom_teacher_not_found
import com.dreamteam.bookworm.teacher.resources.classroom_teachers
import com.dreamteam.bookworm.teacher.resources.classroom_teachers_hint
import com.dreamteam.bookworm.teacher.resources.classroom_type_this
import com.dreamteam.bookworm.teacher.resources.teacher_cancel
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormDialog
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.Eyebrow
import com.dreamteam.bookworm.ui_components.LabeledCheckbox
import com.dreamteam.bookworm.ui_components.RoundIconButton
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClassroomScreen(
    classId: String,
    onManagePaths: () -> Unit,
    onManageClasses: () -> Unit,
    /** The teacher left this classroom (they co-taught it). */
    onLeft: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ClassroomViewModel(classId, container.classroomRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val platform = LocalTeacherPlatform.current
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    val myId = session?.userId.orEmpty()

    LaunchedEffect(state.left) { if (state.left) onLeft() }

    StateContent(state.classroom, onRetry = viewModel::load) { classroom ->
        val copyLink = { if (platform.copyToClipboard(classroom.joinLink)) viewModel.linkCopied() }

        TeacherPage {
            TeacherHeader(
                title = classroom.name,
                subtitle = stringResource(Res.string.classroom_summary, state.inClass, state.waiting, state.total),
            )
            if (state.actionFailed) {
                Callout(
                    text = stringResource(Res.string.classroom_action_failed),
                    icon = Icons.Outlined.ErrorOutline,
                    background = BookWormColors.DangerSoft,
                    contentColor = BookWormColors.DangerDark,
                )
            }
            TwoPane(
                startWeight = 1.4f,
                start = {
                    StudentList(
                        students = state.visibleStudents,
                        query = state.query,
                        onQuery = viewModel::search,
                        onAdd = { viewModel.showAddStudents(true) },
                        onAdmit = viewModel::admit,
                        onRemove = viewModel::askRemove,
                        enabled = !state.busy,
                    )
                },
                end = {
                    JoinCodeCard(
                        classroom = classroom,
                        linkCopied = state.linkCopied,
                        onCopyLink = copyLink,
                        onPrint = { platform.printJoinCode(classroom.name, classroom.joinCode, classroom.joinLink) },
                        onApproveJoins = viewModel::setApproveJoins,
                    )
                    ClassesCard(classroom, enabled = !state.busy, onToggle = viewModel::toggleClass, onManageClasses = onManageClasses)
                    TeachersCard(classroom, myId, state, viewModel)
                    LearningPathsCard(classroom, onManagePaths)
                },
            )
        }

        if (state.showAddStudents) {
            BookWormDialog(
                title = stringResource(Res.string.classroom_add_students),
                message = stringResource(Res.string.classroom_add_body, classroom.joinCode, classroom.joinLink),
                onDismissRequest = { viewModel.showAddStudents(false) },
                confirmText = stringResource(Res.string.classroom_copy_link),
                onConfirm = {
                    copyLink()
                    viewModel.showAddStudents(false)
                },
                dismissText = stringResource(Res.string.classroom_close),
            )
        }

        state.removingTeacher?.let { teacher ->
            BookWormDialog(
                title = stringResource(Res.string.classroom_remove_teacher_title, teacher.name),
                message = stringResource(Res.string.classroom_remove_teacher_body),
                onDismissRequest = { viewModel.askRemoveTeacher(null) },
                confirmText = stringResource(Res.string.classroom_remove_teacher),
                onConfirm = viewModel::removeTeacher,
                dismissText = stringResource(Res.string.teacher_cancel),
                confirmVariant = ButtonVariant.Danger,
            )
        }
        if (state.confirmLeave) {
            BookWormDialog(
                title = stringResource(Res.string.classroom_leave_title, classroom.name),
                message = stringResource(Res.string.classroom_leave_body),
                onDismissRequest = { viewModel.askLeave(false) },
                confirmText = stringResource(Res.string.classroom_leave),
                onConfirm = { viewModel.leave(myId) },
                dismissText = stringResource(Res.string.teacher_cancel),
                confirmVariant = ButtonVariant.Danger,
            )
        }

        state.removing?.let { student ->
            val waiting = student.status == StudentStatus.Waiting
            BookWormDialog(
                title = stringResource(if (waiting) Res.string.classroom_decline_title else Res.string.classroom_remove_title, student.name),
                message = stringResource(if (waiting) Res.string.classroom_decline_body else Res.string.classroom_remove_body, classroom.name),
                onDismissRequest = { viewModel.askRemove(null) },
                confirmText = stringResource(if (waiting) Res.string.classroom_decline else Res.string.classroom_remove),
                onConfirm = viewModel::remove,
                dismissText = stringResource(Res.string.teacher_cancel),
                confirmVariant = ButtonVariant.Danger,
            )
        }
    }
}

/** Which classes (chapters, notes and flashcards) the classroom's students get. */
@Composable
private fun ClassesCard(classroom: Classroom, enabled: Boolean, onToggle: (String) -> Unit, onManageClasses: () -> Unit) {
    SectionCard(stringResource(Res.string.classroom_classes)) {
        Text(
            stringResource(if (classroom.availableClasses.isEmpty()) Res.string.classroom_no_classes else Res.string.classroom_classes_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = BookWormColors.InkMuted,
        )
        classroom.availableClasses.forEach { schoolClass ->
            LabeledCheckbox(
                text = schoolClass.name,
                checked = schoolClass.id in classroom.classIds,
                onCheckedChange = { if (enabled) onToggle(schoolClass.id) },
            )
        }
        if (classroom.otherClasses.isNotEmpty()) {
            Text(stringResource(Res.string.classroom_other_classes), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp))
            classroom.otherClasses.forEach { shared ->
                Text(
                    stringResource(Res.string.classroom_other_class, shared.name, shared.teacherName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BookWormColors.InkMuted,
                )
            }
        }
        SmallButton(stringResource(Res.string.classroom_manage_classes), onManageClasses, variant = ButtonVariant.Secondary, leadingIcon = Icons.Outlined.AutoStories)
    }
}

/** Who teaches the classroom. The owner adds and removes co-teachers; a co-teacher can leave. */
@Composable
private fun TeachersCard(classroom: Classroom, myId: String, state: ClassroomUiState, viewModel: ClassroomViewModel) {
    SectionCard(stringResource(Res.string.classroom_teachers)) {
        Text(stringResource(Res.string.classroom_teachers_hint), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        classroom.teachers.forEach { teacher ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(teacher.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp))
                    Text(teacher.email, style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
                }
                if (teacher.isOwner) {
                    Text(
                        stringResource(Res.string.classroom_owner),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        color = BookWormColors.PrimaryDark,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(BookWormColors.PrimarySoft).padding(horizontal = 10.dp, vertical = 2.dp),
                    )
                } else if (classroom.isOwner) {
                    RoundIconButton(
                        Icons.Outlined.PersonRemove,
                        stringResource(Res.string.classroom_remove_teacher_title, teacher.name),
                        onClick = { viewModel.askRemoveTeacher(teacher) },
                        enabled = !state.busy,
                    )
                }
            }
        }
        if (classroom.isOwner) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BookWormTextField(
                    value = state.teacherEmail,
                    onValueChange = viewModel::setTeacherEmail,
                    placeholder = stringResource(Res.string.classroom_add_teacher_hint),
                    modifier = Modifier.weight(1f),
                    errorText = when (state.addTeacherFailure) {
                        AddTeacherFailure.NotFound -> stringResource(Res.string.classroom_teacher_not_found)
                        AddTeacherFailure.AlreadyTeaches -> stringResource(Res.string.classroom_teacher_already)
                        null -> null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.addTeacher() }),
                )
                SmallButton(
                    stringResource(Res.string.classroom_add_teacher),
                    viewModel::addTeacher,
                    enabled = state.teacherEmail.isNotBlank() && !state.busy,
                    leadingIcon = Icons.Rounded.Add,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else if (classroom.teachers.any { it.id == myId }) {
            SmallButton(
                stringResource(Res.string.classroom_leave),
                { viewModel.askLeave(true) },
                variant = ButtonVariant.Danger,
                enabled = !state.busy,
            )
        }
    }
}

/** The paths the classroom follows, one per class at most. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearningPathsCard(classroom: Classroom, onManagePaths: () -> Unit) {
    val classPaths = classroom.learningPaths
    SectionCard(stringResource(Res.string.classroom_paths)) {
        if (classPaths.isEmpty()) {
            Text(stringResource(Res.string.classroom_no_paths), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                classPaths.forEach { PathChip(it.name) }
            }
        }
        SmallButton(stringResource(Res.string.classroom_manage_paths), onManagePaths, variant = ButtonVariant.Secondary, leadingIcon = Icons.Outlined.Route)
    }
}

@Composable
private fun PathChip(name: String) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(BookWormColors.PrimarySoft).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Outlined.Route, contentDescription = null, tint = BookWormColors.PrimaryDark, modifier = Modifier.size(16.dp))
        Text(name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), color = BookWormColors.PrimaryDark)
    }
}

@Composable
private fun StudentList(
    students: List<ClassStudent>,
    query: String,
    onQuery: (String) -> Unit,
    onAdd: () -> Unit,
    onAdmit: (String) -> Unit,
    onRemove: (ClassStudent) -> Unit,
    enabled: Boolean,
) {
    BookWormCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionTitle(stringResource(Res.string.classroom_students), Modifier.weight(1f))
                val findLabel = stringResource(Res.string.classroom_find)
                BookWormTextField(
                    value = query,
                    onValueChange = onQuery,
                    placeholder = findLabel,
                    modifier = Modifier.width(200.dp).semantics { contentDescription = findLabel },
                )
                SmallButton(stringResource(Res.string.classroom_add_students), onAdd)
            }
            HorizontalDivider(thickness = 2.dp, color = BookWormColors.Divider)
            if (students.isEmpty()) {
                Text(
                    stringResource(Res.string.classroom_no_match),
                    style = MaterialTheme.typography.bodyLarge,
                    color = BookWormColors.InkMuted,
                    modifier = Modifier.padding(22.dp),
                )
            }
            students.forEachIndexed { index, student ->
                StudentRow(student, index, enabled, onAdmit = { onAdmit(student.id) }, onRemove = { onRemove(student) })
                if (index < students.lastIndex) HorizontalDivider(thickness = 1.dp, color = BookWormColors.Divider)
            }
        }
    }
}

@Composable
private fun StudentRow(student: ClassStudent, index: Int, enabled: Boolean, onAdmit: () -> Unit, onRemove: () -> Unit) {
    val tint = listOf(BookWormColors.PrimarySoft, BookWormColors.SunSoft, BookWormColors.SuccessSoft)[index % 3]
    val (label, look) = when (student.status) {
        StudentStatus.InClass -> Res.string.classroom_status_in to StatusLook(BookWormColors.SuccessDark, BookWormColors.Success, BookWormColors.Success)
        StudentStatus.Waiting -> Res.string.classroom_status_waiting to StatusLook(TeacherColors.SunInk, BookWormColors.Sun, BookWormColors.SunDark)
        StudentStatus.Offline -> Res.string.classroom_status_offline to StatusLook(BookWormColors.InkMuted, BookWormColors.Surface, BookWormColors.Outline)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            Text(student.name.take(1), style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp))
        }
        Text(student.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 17.sp), modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(look.dot).border(2.dp, look.dotBorder, CircleShape))
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = look.text)
        }
        if (student.status == StudentStatus.Waiting) {
            SmallButton(stringResource(Res.string.classroom_let_in), onAdmit, variant = ButtonVariant.Secondary, enabled = enabled)
        }
        RoundIconButton(
            Icons.Outlined.PersonRemove,
            stringResource(if (student.status == StudentStatus.Waiting) Res.string.classroom_decline_title else Res.string.classroom_remove_title, student.name),
            onClick = { if (enabled) onRemove() },
        )
    }
}

private data class StatusLook(val text: Color, val dot: Color, val dotBorder: Color)

@Composable
private fun JoinCodeCard(
    classroom: Classroom,
    linkCopied: Boolean,
    onCopyLink: () -> Unit,
    onPrint: () -> Unit,
    onApproveJoins: (Boolean) -> Unit,
) {
    SectionCard(stringResource(Res.string.classroom_join_code)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BookWormColors.SunSoft).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Eyebrow(stringResource(Res.string.classroom_type_this), color = TeacherColors.SunInk)
            Text(
                classroom.joinCode,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 52.sp, lineHeight = 57.sp, letterSpacing = 0.08.em),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // QR codes need an encoder library; this placeholder keeps the canvas layout until one is added.
            Column(
                Modifier.size(96.dp).dashedBorder(BookWormColors.Outline, width = 2.dp, cornerRadius = 14.dp, dash = 6.dp, gap = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = BookWormColors.InkMuted)
                Text(stringResource(Res.string.classroom_qr), style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted, textAlign = TextAlign.Center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton(
                    stringResource(if (linkCopied) Res.string.classroom_link_copied else Res.string.classroom_copy_link),
                    onCopyLink,
                    variant = ButtonVariant.Secondary,
                )
                SmallButton(stringResource(Res.string.classroom_print_code), onPrint, variant = ButtonVariant.Secondary)
            }
        }
        LabeledCheckbox(
            text = stringResource(Res.string.classroom_approve_joins),
            checked = classroom.approveJoins,
            onCheckedChange = onApproveJoins,
        )
    }
}
