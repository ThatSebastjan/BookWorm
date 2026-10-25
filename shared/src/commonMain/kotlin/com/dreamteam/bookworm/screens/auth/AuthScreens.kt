package com.dreamteam.bookworm.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.auth_class_code
import bookworm.shared.generated.resources.auth_class_code_placeholder
import bookworm.shared.generated.resources.auth_email
import bookworm.shared.generated.resources.auth_error_class_code
import bookworm.shared.generated.resources.auth_error_credentials
import bookworm.shared.generated.resources.auth_error_email
import bookworm.shared.generated.resources.auth_error_email_taken
import bookworm.shared.generated.resources.auth_error_network
import bookworm.shared.generated.resources.auth_error_password_short
import bookworm.shared.generated.resources.auth_error_required
import bookworm.shared.generated.resources.auth_go_login
import bookworm.shared.generated.resources.auth_go_register
import bookworm.shared.generated.resources.auth_have_account
import bookworm.shared.generated.resources.auth_login_button
import bookworm.shared.generated.resources.auth_login_subtitle
import bookworm.shared.generated.resources.auth_login_title
import bookworm.shared.generated.resources.auth_name
import bookworm.shared.generated.resources.auth_no_account
import bookworm.shared.generated.resources.auth_password
import bookworm.shared.generated.resources.auth_register_button
import bookworm.shared.generated.resources.auth_register_subtitle
import bookworm.shared.generated.resources.auth_register_title
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.AuthFailure
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.MascotIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(onRegisterClick: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LoginViewModel(container.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    AuthLayout(
        title = stringResource(Res.string.auth_login_title),
        subtitle = stringResource(Res.string.auth_login_subtitle),
        error = state.error,
        switchPrompt = stringResource(Res.string.auth_no_account),
        switchAction = stringResource(Res.string.auth_go_register),
        onSwitch = onRegisterClick,
    ) {
        EmailField(state.email, viewModel::setEmail, state.emailError, enabled = !state.submitting)
        BookWormTextField(
            state.password, viewModel::setPassword, Modifier.fillMaxWidth(),
            label = stringResource(Res.string.auth_password),
            isPassword = true,
            enabled = !state.submitting,
            errorText = state.passwordError?.message(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.login() }),
        )
        BookWormButton(
            stringResource(Res.string.auth_login_button),
            onClick = viewModel::login,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !state.submitting,
        )
    }
}

@Composable
fun RegisterScreen(onLoginClick: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { RegisterViewModel(container.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val enabled = !state.submitting

    AuthLayout(
        title = stringResource(Res.string.auth_register_title),
        subtitle = stringResource(Res.string.auth_register_subtitle),
        error = state.error,
        switchPrompt = stringResource(Res.string.auth_have_account),
        switchAction = stringResource(Res.string.auth_go_login),
        onSwitch = onLoginClick,
    ) {
        BookWormTextField(
            state.name, viewModel::setName, Modifier.fillMaxWidth(),
            label = stringResource(Res.string.auth_name),
            enabled = enabled,
            errorText = state.nameError?.message(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        EmailField(state.email, viewModel::setEmail, state.emailError, enabled)
        BookWormTextField(
            state.password, viewModel::setPassword, Modifier.fillMaxWidth(),
            label = stringResource(Res.string.auth_password),
            isPassword = true,
            enabled = enabled,
            errorText = state.passwordError?.message(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        )
        BookWormTextField(
            state.classCode, viewModel::setClassCode, Modifier.fillMaxWidth(),
            label = stringResource(Res.string.auth_class_code),
            placeholder = stringResource(Res.string.auth_class_code_placeholder),
            enabled = enabled,
            errorText = state.classCodeError?.message(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.register() }),
        )
        BookWormButton(
            stringResource(Res.string.auth_register_button),
            onClick = viewModel::register,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = enabled,
        )
    }
}

@Composable
private fun EmailField(value: String, onChange: (String) -> Unit, error: FieldError?, enabled: Boolean) {
    BookWormTextField(
        value, onChange, Modifier.fillMaxWidth(),
        label = stringResource(Res.string.auth_email),
        enabled = enabled,
        errorText = error?.message(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
}

@Composable
private fun FieldError.message(): String = when (this) {
    FieldError.Required -> stringResource(Res.string.auth_error_required)
    FieldError.InvalidEmail -> stringResource(Res.string.auth_error_email)
    FieldError.PasswordTooShort -> stringResource(Res.string.auth_error_password_short, MIN_PASSWORD_LENGTH)
}

@Composable
private fun SubmitError.message(): String = when (this) {
    SubmitError.Network -> stringResource(Res.string.auth_error_network)
    is SubmitError.Auth -> when (failure) {
        AuthFailure.InvalidCredentials -> stringResource(Res.string.auth_error_credentials)
        AuthFailure.EmailTaken -> stringResource(Res.string.auth_error_email_taken)
        AuthFailure.InvalidClassCode -> stringResource(Res.string.auth_error_class_code)
    }
}

/** Mascot, heading, the form and a link to the other auth screen. */
@Composable
private fun AuthLayout(
    title: String,
    subtitle: String,
    error: SubmitError?,
    switchPrompt: String,
    switchAction: String,
    onSwitch: () -> Unit,
    form: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(BookWormColors.Background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MascotIcon(Modifier.size(96.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = BookWormColors.Ink, textAlign = TextAlign.Center)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted, textAlign = TextAlign.Center)
            }
            if (error != null) {
                Text(
                    error.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BookWormColors.DangerDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BookWormColors.DangerSoft)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp), content = form)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(switchPrompt, style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
                TextButton(onClick = onSwitch) {
                    Text(switchAction, style = MaterialTheme.typography.labelMedium, color = BookWormColors.PrimaryDark)
                }
            }
        }
    }
}
