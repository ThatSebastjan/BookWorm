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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.AssignmentTarget
import com.dreamteam.bookworm.domain.model.ClassGroup
import com.dreamteam.bookworm.domain.model.ClassStudent
import com.dreamteam.bookworm.domain.model.Classroom
import com.dreamteam.bookworm.domain.model.StudentStatus
import com.dreamteam.bookworm.teacher.components.BookWormFormDialog
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
import com.dreamteam.bookworm.teacher.resources.classroom_approve_joins
import com.dreamteam.bookworm.teacher.resources.classroom_close
import com.dreamteam.bookworm.teacher.resources.classroom_copy_link
import com.dreamteam.bookworm.teacher.resources.classroom_delete_group
import com.dreamteam.bookworm.teacher.resources.classroom_delete_group_body
import com.dreamteam.bookworm.teacher.resources.classroom_delete_group_title
import com.dreamteam.bookworm.teacher.resources.classroom_edit_group
import com.dreamteam.bookworm.teacher.resources.classroom_find
import com.dreamteam.bookworm.teacher.resources.classroom_group_members
import com.dreamteam.bookworm.teacher.resources.classroom_group_name
import com.dreamteam.bookworm.teacher.resources.classroom_group_name_hint
import com.dreamteam.bookworm.teacher.resources.classroom_groups
import com.dreamteam.bookworm.teacher.resources.classroom_groups_hint
import com.dreamteam.bookworm.teacher.resources.classroom_join_code
import com.dreamteam.bookworm.teacher.resources.classroom_let_in
import com.dreamteam.bookworm.teacher.resources.classroom_link_copied
import com.dreamteam.bookworm.teacher.resources.classroom_manage_paths
import com.dreamteam.bookworm.teacher.resources.classroom_new_group
import com.dreamteam.bookworm.teacher.resources.classroom_no_match
import com.dreamteam.bookworm.teacher.resources.classroom_no_paths
import com.dreamteam.bookworm.teacher.resources.classroom_paths
import com.dreamteam.bookworm.teacher.resources.classroom_print_code
import com.dreamteam.bookworm.teacher.resources.classroom_qr
import com.dreamteam.bookworm.teacher.resources.classroom_quiz_cta
import com.dreamteam.bookworm.teacher.resources.classroom_set_up_quiz
import com.dreamteam.bookworm.teacher.resources.classroom_status_in
import com.dreamteam.bookworm.teacher.resources.classroom_status_offline
import com.dreamteam.bookworm.teacher.resources.classroom_status_waiting
import com.dreamteam.bookworm.teacher.resources.classroom_students
import com.dreamteam.bookworm.teacher.resources.classroom_summary
import com.dreamteam.bookworm.teacher.resources.classroom_type_this
import com.dreamteam.bookworm.teacher.resources.teacher_cancel
import com.dreamteam.bookworm.teacher.resources.teacher_class_students
import com.dreamteam.bookworm.teacher.resources.teacher_save
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormDialog
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.Eyebrow
import com.dreamteam.bookworm.ui_components.LabeledCheckbox
import com.dreamteam.bookworm.ui_components.MascotIcon
import com.dreamteam.bookworm.ui_components.RoundIconButton
import com.dreamteam.bookworm.ui_components.SmallButton
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClassroomScreen(
    classId: String,
    onSetUpQuiz: () -> Unit,
    onManagePaths: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ClassroomViewModel(classId, container.classroomRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val platform = LocalTeacherPlatform.current

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
                    LearningPathsCard(classroom, onManagePaths)
                    GroupsCard(
                        classroom = classroom,
                        onNew = viewModel::newGroup,
                        onEdit = viewModel::editGroup,
                        onDelete = viewModel::askDeleteGroup,
                    )
                    QuizPrompt(onSetUpQuiz)
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

        state.editingGroup?.let { group ->
            GroupDialog(
                group = group,
                students = classroom.students,
                onSave = viewModel::saveGroup,
                onDismiss = viewModel::closeGroupDialog,
            )
        }

        state.deletingGroup?.let { group ->
            BookWormDialog(
                title = stringResource(Res.string.classroom_delete_group_title, group.name),
                message = stringResource(Res.string.classroom_delete_group_body),
                onDismissRequest = { viewModel.askDeleteGroup(null) },
                confirmText = stringResource(Res.string.classroom_delete_group),
                onConfirm = viewModel::deleteGroup,
                dismissText = stringResource(Res.string.teacher_cancel),
                confirmVariant = ButtonVariant.Danger,
            )
        }
    }
}

/** The paths the whole class follows. Group paths are listed with each group. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearningPathsCard(classroom: Classroom, onManagePaths: () -> Unit) {
    val classPaths = classroom.learningPaths.filter { AssignmentTarget.WholeClass(classroom.id) in it.assignedTo }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupsCard(
    classroom: Classroom,
    onNew: () -> Unit,
    onEdit: (ClassGroup) -> Unit,
    onDelete: (ClassGroup) -> Unit,
) {
    SectionCard(stringResource(Res.string.classroom_groups)) {
        Text(stringResource(Res.string.classroom_groups_hint), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        classroom.groups.forEach { group ->
            val paths = classroom.learningPaths.filter { AssignmentTarget.Group(classroom.id, group.id) in it.assignedTo }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BookWormColors.Background)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(group.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp))
                        Text(
                            pluralStringResource(Res.plurals.teacher_class_students, group.studentIds.size, group.studentIds.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = BookWormColors.InkMuted,
                        )
                    }
                    RoundIconButton(Icons.Outlined.Edit, stringResource(Res.string.classroom_edit_group, group.name), onClick = { onEdit(group) })
                    RoundIconButton(Icons.Outlined.Delete, stringResource(Res.string.classroom_delete_group_title, group.name), onClick = { onDelete(group) })
                }
                if (paths.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        paths.forEach { PathChip(it.name) }
                    }
                }
            }
        }
        SmallButton(stringResource(Res.string.classroom_new_group), onNew, variant = ButtonVariant.Secondary, leadingIcon = Icons.Rounded.Add)
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
private fun GroupDialog(
    group: ClassGroup,
    students: List<ClassStudent>,
    onSave: (ClassGroup) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(group) { mutableStateOf(group.name) }
    var members by remember(group) { mutableStateOf(group.studentIds.toSet()) }
    BookWormFormDialog(
        title = stringResource(if (group.id.isEmpty()) Res.string.classroom_new_group else Res.string.classroom_edit_group, group.name),
        onDismissRequest = onDismiss,
        confirmText = stringResource(Res.string.teacher_save),
        onConfirm = { onSave(group.copy(name = name, studentIds = students.map { it.id }.filter { it in members })) },
        dismissText = stringResource(Res.string.teacher_cancel),
        confirmEnabled = name.isNotBlank() && members.isNotEmpty(),
    ) {
        BookWormTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(Res.string.classroom_group_name),
            placeholder = stringResource(Res.string.classroom_group_name_hint),
        )
        Text(stringResource(Res.string.classroom_group_members), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp))
        students.forEach { student ->
            LabeledCheckbox(
                text = student.name,
                checked = student.id in members,
                onCheckedChange = { checked -> members = if (checked) members + student.id else members - student.id },
            )
        }
    }
}

@Composable
private fun StudentList(
    students: List<ClassStudent>,
    query: String,
    onQuery: (String) -> Unit,
    onAdd: () -> Unit,
    onAdmit: (String) -> Unit,
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
                StudentRow(student, index, onAdmit = { onAdmit(student.id) })
                if (index < students.lastIndex) HorizontalDivider(thickness = 1.dp, color = BookWormColors.Divider)
            }
        }
    }
}

@Composable
private fun StudentRow(student: ClassStudent, index: Int, onAdmit: () -> Unit) {
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
            SmallButton(stringResource(Res.string.classroom_let_in), onAdmit, variant = ButtonVariant.Secondary)
        }
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

@Composable
private fun QuizPrompt(onSetUpQuiz: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(BookWormColors.PrimarySoft).padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MascotIcon(Modifier.size(64.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(Res.string.classroom_quiz_cta), style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp), color = BookWormColors.PrimaryDeep)
            BookWormButton(stringResource(Res.string.classroom_set_up_quiz), onSetUpQuiz)
        }
    }
}
