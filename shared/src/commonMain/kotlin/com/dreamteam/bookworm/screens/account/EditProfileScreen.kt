package com.dreamteam.bookworm.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.auth_email
import bookworm.shared.generated.resources.auth_error_password_short
import bookworm.shared.generated.resources.auth_error_required
import bookworm.shared.generated.resources.auth_name
import bookworm.shared.generated.resources.auth_surname
import bookworm.shared.generated.resources.common_back
import bookworm.shared.generated.resources.common_error_body
import bookworm.shared.generated.resources.edit_profile_change_password
import bookworm.shared.generated.resources.edit_profile_current_password
import bookworm.shared.generated.resources.edit_profile_details
import bookworm.shared.generated.resources.edit_profile_email_hint
import bookworm.shared.generated.resources.edit_profile_new_password
import bookworm.shared.generated.resources.edit_profile_password
import bookworm.shared.generated.resources.edit_profile_password_changed
import bookworm.shared.generated.resources.edit_profile_save
import bookworm.shared.generated.resources.edit_profile_saved
import bookworm.shared.generated.resources.edit_profile_title
import bookworm.shared.generated.resources.edit_profile_wrong_password
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.screens.auth.MIN_PASSWORD_LENGTH
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.RoundIconButton
import org.jetbrains.compose.resources.stringResource

/** The student changes their name or password. The email is how they sign in, so it stays. */
@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { EditProfileViewModel(container.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val required = stringResource(Res.string.auth_error_required)

    Column(
        Modifier
            .fillMaxSize()
            .background(BookWormColors.Background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
    ) {
        RoundIconButton(
            Icons.AutoMirrored.Rounded.ArrowBack,
            stringResource(Res.string.common_back),
            onClick = onBack,
            tint = BookWormColors.Ink,
            background = BookWormColors.Surface,
            modifier = Modifier.border(2.dp, BookWormColors.Border, CircleShape),
        )
        Column(
            Modifier.padding(start = 8.dp, top = 24.dp).widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(Res.string.edit_profile_title), style = MaterialTheme.typography.headlineLarge)

            BookWormCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(Res.string.edit_profile_details), style = MaterialTheme.typography.titleMedium)
                    BookWormTextField(
                        state.name, viewModel::setName, Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.auth_name),
                        errorText = required.takeIf { state.showErrors && state.name.isBlank() },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    )
                    BookWormTextField(
                        state.surname, viewModel::setSurname, Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.auth_surname),
                        errorText = required.takeIf { state.showErrors && state.surname.isBlank() },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.saveDetails() }),
                    )
                    BookWormTextField(state.email, {}, Modifier.fillMaxWidth(), label = stringResource(Res.string.auth_email), enabled = false)
                    Text(stringResource(Res.string.edit_profile_email_hint), style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
                    Result(state.detailsResult, saved = stringResource(Res.string.edit_profile_saved))
                    BookWormButton(
                        stringResource(Res.string.edit_profile_save),
                        onClick = viewModel::saveDetails,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.savingDetails,
                    )
                }
            }

            BookWormCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(Res.string.edit_profile_password), style = MaterialTheme.typography.titleMedium)
                    BookWormTextField(
                        state.currentPassword, viewModel::setCurrentPassword, Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.edit_profile_current_password),
                        isPassword = true,
                        errorText = required.takeIf { state.showErrors && state.currentPassword.isEmpty() },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    )
                    BookWormTextField(
                        state.newPassword, viewModel::setNewPassword, Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.edit_profile_new_password),
                        isPassword = true,
                        errorText = stringResource(Res.string.auth_error_password_short, MIN_PASSWORD_LENGTH)
                            .takeIf { state.showErrors && state.newPasswordWeak },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.changePassword() }),
                    )
                    Result(state.passwordResult, saved = stringResource(Res.string.edit_profile_password_changed))
                    BookWormButton(
                        stringResource(Res.string.edit_profile_change_password),
                        onClick = viewModel::changePassword,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.changingPassword,
                    )
                }
            }
        }
    }
}

@Composable
private fun Result(result: FormResult?, saved: String) {
    val (text, error) = when (result) {
        FormResult.Saved -> saved to false
        FormResult.WrongPassword -> stringResource(Res.string.edit_profile_wrong_password) to true
        FormResult.Failed -> stringResource(Res.string.common_error_body) to true
        null -> return
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (error) BookWormColors.DangerSoft else BookWormColors.SuccessSoft)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (error) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (error) BookWormColors.DangerDark else BookWormColors.SuccessDark,
            modifier = Modifier.size(22.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (error) BookWormColors.DangerDark else BookWormColors.SuccessDark)
    }
}
