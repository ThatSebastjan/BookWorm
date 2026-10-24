package com.dreamteam.bookworm.ui_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.common_error_body
import bookworm.shared.generated.resources.common_error_title
import bookworm.shared.generated.resources.common_try_again
import com.dreamteam.bookworm.ui.UiState
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BookWormColors.Primary, trackColor = BookWormColors.Track)
    }
}

@Composable
fun ErrorView(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = BookWormColors.InkMuted, modifier = Modifier.size(48.dp))
            Text(stringResource(Res.string.common_error_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(stringResource(Res.string.common_error_body), style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted, textAlign = TextAlign.Center)
            BookWormButton(stringResource(Res.string.common_try_again), onRetry)
        }
    }
}

/** Shows loading and error states, and [content] once data is there. */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> LoadingView(modifier)
        is UiState.Error -> ErrorView(onRetry, modifier)
        is UiState.Success -> content(state.data)
    }
}
