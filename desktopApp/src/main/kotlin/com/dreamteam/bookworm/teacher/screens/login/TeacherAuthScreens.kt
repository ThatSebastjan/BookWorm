package com.dreamteam.bookworm.teacher.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.AuthFailure
import com.dreamteam.bookworm.screens.auth.FieldError
import com.dreamteam.bookworm.screens.auth.LoginViewModel
import com.dreamteam.bookworm.screens.auth.MIN_PASSWORD_LENGTH
import com.dreamteam.bookworm.screens.auth.RegisterViewModel
import com.dreamteam.bookworm.screens.auth.SubmitError
import com.dreamteam.bookworm.teacher.components.Callout
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.app_name
import com.dreamteam.bookworm.teacher.resources.login_button
import com.dreamteam.bookworm.teacher.resources.login_email
import com.dreamteam.bookworm.teacher.resources.login_error_credentials
import com.dreamteam.bookworm.teacher.resources.login_error_email
import com.dreamteam.bookworm.teacher.resources.login_error_network
import com.dreamteam.bookworm.teacher.resources.login_error_required
import com.dreamteam.bookworm.teacher.resources.login_error_wrong_role
import com.dreamteam.bookworm.teacher.resources.login_go_register
import com.dreamteam.bookworm.teacher.resources.login_no_account
import com.dreamteam.bookworm.teacher.resources.login_password
import com.dreamteam.bookworm.teacher.resources.login_subtitle
import com.dreamteam.bookworm.teacher.resources.login_title
import com.dreamteam.bookworm.teacher.resources.register_button
import com.dreamteam.bookworm.teacher.resources.register_error_email_taken
import com.dreamteam.bookworm.teacher.resources.register_error_invalid
import com.dreamteam.bookworm.teacher.resources.register_error_password
import com.dreamteam.bookworm.teacher.resources.register_go_login
import com.dreamteam.bookworm.teacher.resources.register_have_account
import com.dreamteam.bookworm.teacher.resources.register_name
import com.dreamteam.bookworm.teacher.resources.register_subtitle
import com.dreamteam.bookworm.teacher.resources.register_surname
import com.dreamteam.bookworm.teacher.resources.register_title
import com.dreamteam.bookworm.teacher.resources.teacher_for_teachers
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.MascotIcon
import org.jetbrains.compose.resources.stringResource

/*
 * Sign-in and sign-up for teachers. The container is created with a required role, so only
 * teacher accounts get through sign-in, and sign-up creates teacher accounts. Either one
 * flips the session and the app shows the sidebar.
 */

@Composable
fun TeacherLoginScreen(onRegisterClick: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LoginViewModel(container.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val enabled = !state.submitting

    TeacherAuthLayout(
        title = stringResource(Res.string.login_title),
        subtitle = stringResource(Res.string.login_subtitle),
        error = state.error,
        switchPrompt = stringResource(Res.string.login_no_account),
        switchAction = stringResource(Res.string.login_go_register),
        onSwitch = onRegisterClick,
    ) {
        EmailField(state.email, viewModel::setEmail, state.emailError, enabled)
        PasswordField(state.password, viewModel::setPassword, state.passwordError, enabled, onDone = viewModel::login)
        BookWormButton(
            stringResource(Res.string.login_button),
            onClick = viewModel::login,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            enabled = enabled,
        )
    }
}

@Composable
fun TeacherRegisterScreen(onLoginClick: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { RegisterViewModel(container.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val enabled = !state.submitting

    TeacherAuthLayout(
        title = stringResource(Res.string.register_title),
        subtitle = stringResource(Res.string.register_subtitle),
        error = state.error,
        switchPrompt = stringResource(Res.string.register_have_account),
        switchAction = stringResource(Res.string.register_go_login),
        onSwitch = onLoginClick,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NameField(state.name, viewModel::setName, stringResource(Res.string.register_name), state.nameError, enabled, Modifier.weight(1f))
            NameField(state.surname, viewModel::setSurname, stringResource(Res.string.register_surname), state.surnameError, enabled, Modifier.weight(1f))
        }
        EmailField(state.email, viewModel::setEmail, state.emailError, enabled)
        PasswordField(state.password, viewModel::setPassword, state.passwordError, enabled, onDone = viewModel::register)
        BookWormButton(
            stringResource(Res.string.register_button),
            onClick = viewModel::register,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            enabled = enabled,
        )
    }
}

/** The app name above a card with the form, and a link to the other form under it. */
@Composable
private fun TeacherAuthLayout(
    title: String,
    subtitle: String,
    error: SubmitError?,
    switchPrompt: String,
    switchAction: String,
    onSwitch: () -> Unit,
    form: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(BookWormColors.Background).verticalScroll(rememberScrollState()).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MascotIcon(Modifier.size(48.dp))
                Column {
                    Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp))
                    Text(stringResource(Res.string.teacher_for_teachers), style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
                }
            }

            BookWormCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(28.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = BookWormColors.Ink,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                    }
                    error?.let {
                        Callout(
                            text = it.message(),
                            icon = Icons.Rounded.ErrorOutline,
                            background = BookWormColors.DangerSoft,
                            contentColor = BookWormColors.DangerDark,
                        )
                    }
                    form()
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(switchPrompt, style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                Text(
                    switchAction,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                    color = BookWormColors.PrimaryDark,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onSwitch).padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: FieldError?,
    enabled: Boolean,
    modifier: Modifier,
) {
    BookWormTextField(
        value, onChange, modifier,
        label = label,
        enabled = enabled,
        errorText = error?.message(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
}

@Composable
private fun EmailField(value: String, onChange: (String) -> Unit, error: FieldError?, enabled: Boolean) {
    BookWormTextField(
        value, onChange, Modifier.fillMaxWidth(),
        label = stringResource(Res.string.login_email),
        enabled = enabled,
        errorText = error?.message(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
}

/** Enter in the password field submits the form. */
@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, error: FieldError?, enabled: Boolean, onDone: () -> Unit) {
    BookWormTextField(
        value, onChange, Modifier.fillMaxWidth(),
        label = stringResource(Res.string.login_password),
        isPassword = true,
        enabled = enabled,
        errorText = error?.message(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Composable
private fun FieldError.message(): String = when (this) {
    FieldError.Required -> stringResource(Res.string.login_error_required)
    FieldError.InvalidEmail -> stringResource(Res.string.login_error_email)
    FieldError.PasswordTooWeak -> stringResource(Res.string.register_error_password, MIN_PASSWORD_LENGTH)
}

@Composable
private fun SubmitError.message(): String = when (this) {
    SubmitError.Network -> stringResource(Res.string.login_error_network)
    is SubmitError.Auth -> when (failure) {
        AuthFailure.InvalidCredentials -> stringResource(Res.string.login_error_credentials)
        AuthFailure.WrongRole -> stringResource(Res.string.login_error_wrong_role)
        AuthFailure.EmailTaken -> stringResource(Res.string.register_error_email_taken)
        AuthFailure.InvalidDetails -> stringResource(Res.string.register_error_invalid)
    }
}
