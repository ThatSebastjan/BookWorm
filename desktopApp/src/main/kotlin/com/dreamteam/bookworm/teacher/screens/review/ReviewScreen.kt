package com.dreamteam.bookworm.teacher.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.DraftCard
import com.dreamteam.bookworm.domain.model.DraftStatus
import com.dreamteam.bookworm.domain.model.ReviewBatch
import com.dreamteam.bookworm.teacher.components.Callout
import com.dreamteam.bookworm.teacher.components.SectionCard
import com.dreamteam.bookworm.teacher.components.TeacherColors
import com.dreamteam.bookworm.teacher.components.TeacherHeader
import com.dreamteam.bookworm.teacher.components.TeacherPage
import com.dreamteam.bookworm.teacher.components.dashedBorder
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.review_approve
import com.dreamteam.bookworm.teacher.resources.review_approve_clear
import com.dreamteam.bookworm.teacher.resources.review_approved_count
import com.dreamteam.bookworm.teacher.resources.review_back
import com.dreamteam.bookworm.teacher.resources.review_back_label
import com.dreamteam.bookworm.teacher.resources.review_check_this
import com.dreamteam.bookworm.teacher.resources.review_edit_title
import com.dreamteam.bookworm.teacher.resources.review_edited
import com.dreamteam.bookworm.teacher.resources.review_empty_body
import com.dreamteam.bookworm.teacher.resources.review_empty_title
import com.dreamteam.bookworm.teacher.resources.review_eyebrow
import com.dreamteam.bookworm.teacher.resources.review_flag
import com.dreamteam.bookworm.teacher.resources.review_front
import com.dreamteam.bookworm.teacher.resources.review_front_label
import com.dreamteam.bookworm.teacher.resources.review_go_upload
import com.dreamteam.bookworm.teacher.resources.review_pending_count
import com.dreamteam.bookworm.teacher.resources.review_publish
import com.dreamteam.bookworm.teacher.resources.review_publish_none
import com.dreamteam.bookworm.teacher.resources.review_published
import com.dreamteam.bookworm.teacher.resources.review_published_chip
import com.dreamteam.bookworm.teacher.resources.review_remove
import com.dreamteam.bookworm.teacher.resources.review_removed_count
import com.dreamteam.bookworm.teacher.resources.review_save_failed
import com.dreamteam.bookworm.teacher.resources.review_source
import com.dreamteam.bookworm.teacher.resources.review_status_approved
import com.dreamteam.bookworm.teacher.resources.review_status_pending
import com.dreamteam.bookworm.teacher.resources.review_status_removed
import com.dreamteam.bookworm.teacher.resources.review_subtitle
import com.dreamteam.bookworm.teacher.resources.review_title
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormTextField
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.Eyebrow
import com.dreamteam.bookworm.ui_components.LedgeSurface
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReviewScreen(
    uploadId: String?,
    onCardsChanged: () -> Unit,
    onUpload: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ReviewViewModel(uploadId, container.flashcardReviewRepository, onCardsChanged) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    StateContent(state.batch, onRetry = viewModel::load) { batch ->
        TeacherPage {
            if (batch == null) EmptyReview(onUpload) else ReviewContent(batch, state, viewModel)
        }
    }
}

@Composable
private fun ReviewContent(batch: ReviewBatch, state: ReviewUiState, viewModel: ReviewViewModel) {
    TeacherHeader(
        eyebrow = stringResource(Res.string.review_eyebrow, batch.subjectName, batch.chapter.number, batch.chapter.title),
        title = stringResource(Res.string.review_title),
        subtitle = stringResource(Res.string.review_subtitle, batch.fileName),
        actions = {
            BookWormButton(
                stringResource(Res.string.review_approve_clear, state.clearPending),
                viewModel::approveClearCards,
                variant = ButtonVariant.Secondary,
                enabled = state.clearPending > 0,
            )
            BookWormButton(
                if (state.toPublish > 0) stringResource(Res.string.review_publish, state.toPublish) else stringResource(Res.string.review_publish_none),
                viewModel::publish,
                enabled = state.toPublish > 0,
            )
        },
    )

    state.justPublished?.let { count ->
        Callout(
            text = pluralStringResource(Res.plurals.review_published, count, count, batch.chapter.number),
            icon = Icons.Outlined.CheckCircle,
            background = BookWormColors.SuccessSoft,
            contentColor = BookWormColors.SuccessDark,
            fontSize = 16,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (state.saveFailed) {
        Callout(
            text = stringResource(Res.string.review_save_failed),
            icon = Icons.Outlined.ErrorOutline,
            background = BookWormColors.DangerSoft,
            contentColor = BookWormColors.DangerDark,
        )
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 900.dp
        val list = @Composable { modifier: Modifier ->
            CardList(batch, state, onSelect = viewModel::select, modifier = modifier)
        }
        val detail = @Composable { modifier: Modifier ->
            state.selected?.let { card ->
                CardDetail(
                    card = card,
                    onFront = viewModel::editFront,
                    onBack = viewModel::editBack,
                    onApprove = viewModel::approve,
                    onRemove = viewModel::remove,
                    modifier = modifier,
                )
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                list(Modifier.width(340.dp))
                detail(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                list(Modifier.fillMaxWidth())
                detail(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EmptyReview(onUpload: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(BookWormColors.Surface)
            .dashedBorder(BookWormColors.Outline)
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Outlined.Style, contentDescription = null, tint = BookWormColors.PrimaryDark)
        Text(stringResource(Res.string.review_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(stringResource(Res.string.review_empty_body), style = MaterialTheme.typography.bodyLarge, color = BookWormColors.InkMuted, textAlign = TextAlign.Center)
        BookWormButton(stringResource(Res.string.review_go_upload), onUpload)
    }
}

@Composable
private fun CardList(
    batch: ReviewBatch,
    state: ReviewUiState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp)
            Text(stringResource(Res.string.review_approved_count, state.approved), style = style, color = BookWormColors.InkMuted)
            Text(stringResource(Res.string.review_pending_count, state.pending), style = style, color = BookWormColors.InkMuted)
            Text(stringResource(Res.string.review_removed_count, state.removed), style = style, color = BookWormColors.InkMuted)
        }
        batch.cards.forEach { card ->
            CardListItem(card, selected = card.id == state.selected?.id, onClick = { onSelect(card.id) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardListItem(card: DraftCard, selected: Boolean, onClick: () -> Unit) {
    LedgeSurface(
        color = if (selected) BookWormColors.PrimaryTint else BookWormColors.Surface,
        ledgeColor = if (selected) BookWormColors.Primary else BookWormColors.Border,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = if (selected) BookWormColors.Primary else BookWormColors.Border,
        ledge = if (selected) 4.dp else 3.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(card.flashcard.term, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, lineHeight = 21.sp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val (label, colors) = when (card.status) {
                    DraftStatus.Pending -> Res.string.review_status_pending to (BookWormColors.Track to BookWormColors.InkMuted)
                    DraftStatus.Approved -> Res.string.review_status_approved to (BookWormColors.SuccessSoft to BookWormColors.SuccessDark)
                    DraftStatus.Removed -> Res.string.review_status_removed to (BookWormColors.DangerSoft to BookWormColors.DangerDark)
                }
                Chip(stringResource(label), colors.first, colors.second)
                if (card.published) Chip(stringResource(Res.string.review_published_chip), BookWormColors.SuccessSoft, BookWormColors.SuccessDark)
                if (card.edited) Chip(stringResource(Res.string.review_edited), BookWormColors.PrimarySoft, BookWormColors.PrimaryDark)
                if (card.issue != null) Chip(stringResource(Res.string.review_check_this), BookWormColors.SunSoft, TeacherColors.SunInk)
            }
        }
    }
}

@Composable
private fun Chip(text: String, background: Color, content: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
        color = content,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(background).padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

@Composable
private fun CardDetail(
    card: DraftCard,
    onFront: (String) -> Unit,
    onBack: (String) -> Unit,
    onApprove: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        card.issue?.let { issue ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BookWormColors.SunSoft)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = TeacherColors.SunInkDark)
                Text(stringResource(Res.string.review_flag, issue), style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp), color = TeacherColors.SunInkDark)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CardFace(
                label = stringResource(Res.string.review_front_label),
                text = card.flashcard.term,
                face = BookWormColors.Surface,
                ledge = BookWormColors.Border,
                textColor = BookWormColors.Ink,
                fontSize = 28,
                modifier = Modifier.weight(1f),
            )
            CardFace(
                label = stringResource(Res.string.review_back_label),
                text = card.flashcard.definition,
                face = BookWormColors.Primary,
                ledge = BookWormColors.PrimaryDark,
                textColor = Color.White,
                fontSize = 22,
                modifier = Modifier.weight(1f),
            )
        }

        SectionCard(stringResource(Res.string.review_edit_title)) {
            BookWormTextField(card.flashcard.term, onFront, label = stringResource(Res.string.review_front), singleLine = false, minLines = 2)
            BookWormTextField(card.flashcard.definition, onBack, label = stringResource(Res.string.review_back), singleLine = false, minLines = 3)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BookWormColors.Background)
                    .dashedBorder(BookWormColors.TrackShadow, width = 2.dp, cornerRadius = 14.dp, dash = 6.dp, gap = 5.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Eyebrow(stringResource(Res.string.review_source, card.sourcePage), color = BookWormColors.InkMuted)
                Text(
                    "“${card.sourceQuote}”",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic, lineHeight = 24.sp),
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            BookWormButton(stringResource(Res.string.review_remove), onRemove, variant = ButtonVariant.Secondary, enabled = card.status != DraftStatus.Removed)
            BookWormButton(stringResource(Res.string.review_approve), onApprove, variant = ButtonVariant.Success, enabled = card.status != DraftStatus.Approved)
        }
    }
}

@Composable
private fun CardFace(
    label: String,
    text: String,
    face: Color,
    ledge: Color,
    textColor: Color,
    fontSize: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(label, color = BookWormColors.InkMuted)
        LedgeSurface(
            color = face,
            ledgeColor = ledge,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 186.dp),
            borderColor = if (face == BookWormColors.Surface) BookWormColors.Border else null,
            borderWidth = 3.dp,
            ledge = 6.dp,
            contentPadding = PaddingValues(24.dp),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize.sp, lineHeight = (fontSize * 1.3f).sp),
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
