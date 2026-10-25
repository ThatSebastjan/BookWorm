package com.dreamteam.bookworm.screens.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HourglassTop
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.auth_class_code
import bookworm.shared.generated.resources.auth_class_code_placeholder
import bookworm.shared.generated.resources.common_back
import bookworm.shared.generated.resources.join_class_another
import bookworm.shared.generated.resources.join_class_button
import bookworm.shared.generated.resources.join_class_done
import bookworm.shared.generated.resources.join_class_error_already
import bookworm.shared.generated.resources.join_class_error_empty
import bookworm.shared.generated.resources.join_class_error_network
import bookworm.shared.generated.resources.join_class_error_not_found
import bookworm.shared.generated.resources.join_class_joined_body
import bookworm.shared.generated.resources.join_class_joined_title
import bookworm.shared.generated.resources.join_class_subtitle
import bookworm.shared.generated.resources.join_class_title
import bookworm.shared.generated.resources.join_class_waiting_body
import bookworm.shared.generated.resources.join_class_waiting_title
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.JoinFailure
import com.dreamteam.bookworm.domain.model.MembershipStatus
import com.dreamteam.bookworm.domain.model.StudentClass
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.RoundIconButton
import org.jetbrains.compose.resources.stringResource

/** Student types the code (or pastes the link) their teacher gave them. */
@Composable
fun JoinClassScreen(onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { JoinClassViewModel(container.studentClassesRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            onClick = onDone,
            tint = BookWormColors.Ink,
            background = BookWormColors.Surface,
            modifier = Modifier.border(2.dp, BookWormColors.Border, CircleShape),
        )
        Column(
            Modifier.padding(start = 8.dp, top = 24.dp).widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val joined = state.joined
            if (joined != null) {
                JoinedResult(joined, onDone = onDone, onJoinAnother = viewModel::reset)
            } else {
                Text(stringResource(Res.string.join_class_title), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(Res.string.join_class_subtitle), style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted)
                BookWormTextField(
                    state.code, viewModel::setCode, Modifier.fillMaxWidth(),
                    label = stringResource(Res.string.auth_class_code),
                    placeholder = stringResource(Res.string.auth_class_code_placeholder),
                    enabled = !state.submitting,
                    errorText = state.error?.message(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.join() }),
                )
                BookWormButton(
                    stringResource(Res.string.join_class_button),
                    onClick = viewModel::join,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !state.submitting,
                )
            }
        }
    }
}

@Composable
private fun JoinError.message(): String = when (this) {
    JoinError.Empty -> stringResource(Res.string.join_class_error_empty)
    JoinError.Network -> stringResource(Res.string.join_class_error_network)
    is JoinError.Failed -> when (failure) {
        JoinFailure.CodeNotFound -> stringResource(Res.string.join_class_error_not_found)
        JoinFailure.AlreadyInClass -> stringResource(Res.string.join_class_error_already)
    }
}

@Composable
private fun JoinedResult(joined: StudentClass, onDone: () -> Unit, onJoinAnother: () -> Unit) {
    val waiting = joined.status == MembershipStatus.WaitingForTeacher
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier.size(112.dp).clip(CircleShape).background(if (waiting) BookWormColors.SunSoft else BookWormColors.SuccessSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (waiting) Icons.Rounded.HourglassTop else Icons.Rounded.Check,
                contentDescription = null,
                tint = if (waiting) BookWormColors.SunDark else BookWormColors.Success,
                modifier = Modifier.size(64.dp),
            )
        }
        Text(
            stringResource(if (waiting) Res.string.join_class_waiting_title else Res.string.join_class_joined_title, joined.name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(if (waiting) Res.string.join_class_waiting_body else Res.string.join_class_joined_body, joined.teacherName),
            style = MaterialTheme.typography.bodyLarge,
            color = BookWormColors.InkMuted,
            textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookWormButton(stringResource(Res.string.join_class_another), onJoinAnother, Modifier.weight(1f), variant = ButtonVariant.Secondary)
            BookWormButton(stringResource(Res.string.join_class_done), onDone, Modifier.weight(1f), variant = ButtonVariant.Success)
        }
    }
}
