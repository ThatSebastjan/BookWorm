package com.dreamteam.bookworm.teacher.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.ClassSummary
import com.dreamteam.bookworm.screens.account.EditProfileViewModel
import com.dreamteam.bookworm.screens.account.FormResult
import com.dreamteam.bookworm.screens.auth.MIN_PASSWORD_LENGTH
import com.dreamteam.bookworm.teacher.components.Callout
import com.dreamteam.bookworm.teacher.components.SectionCard
import com.dreamteam.bookworm.teacher.components.TeacherHeader
import com.dreamteam.bookworm.teacher.components.TeacherPage
import com.dreamteam.bookworm.teacher.components.TwoPane
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.account_change_password
import com.dreamteam.bookworm.teacher.resources.account_classrooms
import com.dreamteam.bookworm.teacher.resources.account_co_teacher
import com.dreamteam.bookworm.teacher.resources.account_current_password
import com.dreamteam.bookworm.teacher.resources.account_details
import com.dreamteam.bookworm.teacher.resources.account_email
import com.dreamteam.bookworm.teacher.resources.account_email_hint
import com.dreamteam.bookworm.teacher.resources.account_failed
import com.dreamteam.bookworm.teacher.resources.account_name
import com.dreamteam.bookworm.teacher.resources.account_new_password
import com.dreamteam.bookworm.teacher.resources.account_no_classrooms
import com.dreamteam.bookworm.teacher.resources.account_owner
import com.dreamteam.bookworm.teacher.resources.account_password
import com.dreamteam.bookworm.teacher.resources.account_password_changed
import com.dreamteam.bookworm.teacher.resources.account_password_rule
import com.dreamteam.bookworm.teacher.resources.account_required
import com.dreamteam.bookworm.teacher.resources.account_save
import com.dreamteam.bookworm.teacher.resources.account_saved
import com.dreamteam.bookworm.teacher.resources.account_subtitle
import com.dreamteam.bookworm.teacher.resources.account_surname
import com.dreamteam.bookworm.teacher.resources.account_title
import com.dreamteam.bookworm.teacher.resources.account_wrong_password
import com.dreamteam.bookworm.teacher.resources.profile_grade
import com.dreamteam.bookworm.teacher.resources.teacher_class_students
import com.dreamteam.bookworm.teacher.resources.teacher_log_out
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The teacher's profile: their name, password and the classrooms they teach. */
@Composable
fun AccountScreen(onOpenClassroom: (String) -> Unit, onLogout: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { EditProfileViewModel(container.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account = viewModel { AccountViewModel(container.teacherRepository) }
    val classroomList by account.classrooms.collectAsStateWithLifecycle()
    val required = stringResource(Res.string.account_required)

    TeacherPage {
        TeacherHeader(
            title = "${state.name} ${state.surname}".ifBlank { stringResource(Res.string.account_title) },
            subtitle = stringResource(Res.string.account_subtitle),
            actions = { BookWormButton(stringResource(Res.string.teacher_log_out), onLogout, variant = ButtonVariant.Secondary, leadingIcon = Icons.AutoMirrored.Rounded.Logout) },
        )
        TwoPane(
            start = {
                SectionCard(stringResource(Res.string.account_details)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BookWormTextField(
                            state.name, viewModel::setName, Modifier.weight(1f),
                            label = stringResource(Res.string.account_name),
                            errorText = required.takeIf { state.showErrors && state.name.isBlank() },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        )
                        BookWormTextField(
                            state.surname, viewModel::setSurname, Modifier.weight(1f),
                            label = stringResource(Res.string.account_surname),
                            errorText = required.takeIf { state.showErrors && state.surname.isBlank() },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        )
                    }
                    BookWormTextField(state.email, {}, Modifier.fillMaxWidth(), label = stringResource(Res.string.account_email), enabled = false)
                    Text(stringResource(Res.string.account_email_hint), style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
                    Result(state.detailsResult, saved = stringResource(Res.string.account_saved))
                    BookWormButton(stringResource(Res.string.account_save), viewModel::saveDetails, enabled = !state.savingDetails)
                }
                SectionCard(stringResource(Res.string.account_password)) {
                    BookWormTextField(
                        state.currentPassword, viewModel::setCurrentPassword, Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.account_current_password),
                        isPassword = true,
                        errorText = required.takeIf { state.showErrors && state.currentPassword.isEmpty() },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    )
                    BookWormTextField(
                        state.newPassword, viewModel::setNewPassword, Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.account_new_password),
                        isPassword = true,
                        errorText = stringResource(Res.string.account_password_rule, MIN_PASSWORD_LENGTH).takeIf { state.showErrors && state.newPasswordWeak },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.changePassword() }),
                    )
                    Result(state.passwordResult, saved = stringResource(Res.string.account_password_changed))
                    BookWormButton(stringResource(Res.string.account_change_password), viewModel::changePassword, enabled = !state.changingPassword)
                }
            },
            end = {
                SectionCard(stringResource(Res.string.account_classrooms)) {
                    StateContent(classroomList, onRetry = account::loadClassrooms) { classrooms ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (classrooms.isEmpty()) {
                                Text(stringResource(Res.string.account_no_classrooms), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                            }
                            classrooms.forEach { ClassroomRow(it, onClick = { onOpenClassroom(it.id) }) }
                        }
                    }
                }
            },
        )
    }
}

/** One classroom the teacher teaches; opens it on the Classroom page. */
@Composable
private fun ClassroomRow(classroom: ClassSummary, onClick: () -> Unit) {
    BookWormCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(classroom.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp))
                Text(
                    listOfNotNull(
                        classroom.grade.takeIf { it > 0 }?.let { stringResource(Res.string.profile_grade, it) },
                        pluralStringResource(Res.plurals.teacher_class_students, classroom.studentCount, classroom.studentCount),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = BookWormColors.InkMuted,
                )
            }
            Text(
                stringResource(if (classroom.isOwner) Res.string.account_owner else Res.string.account_co_teacher),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = BookWormColors.PrimaryDark,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(BookWormColors.PrimarySoft).padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun Result(result: FormResult?, saved: String) {
    val (text, error) = when (result) {
        FormResult.Saved -> saved to false
        FormResult.WrongPassword -> stringResource(Res.string.account_wrong_password) to true
        FormResult.Failed -> stringResource(Res.string.account_failed) to true
        null -> return
    }
    Callout(
        text = text,
        icon = if (error) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
        background = if (error) BookWormColors.DangerSoft else BookWormColors.SuccessSoft,
        contentColor = if (error) BookWormColors.DangerDark else BookWormColors.SuccessDark,
    )
}
