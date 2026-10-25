package com.dreamteam.bookworm.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.auth_cancel
import bookworm.shared.generated.resources.auth_logout
import bookworm.shared.generated.resources.auth_logout_body
import bookworm.shared.generated.resources.auth_logout_title
import bookworm.shared.generated.resources.common_error_title
import bookworm.shared.generated.resources.join_class_title
import bookworm.shared.generated.resources.profile_badge_locked
import bookworm.shared.generated.resources.profile_badges
import bookworm.shared.generated.resources.profile_chapter_of
import bookworm.shared.generated.resources.profile_class_waiting
import bookworm.shared.generated.resources.profile_classes
import bookworm.shared.generated.resources.profile_day_not_yet
import bookworm.shared.generated.resources.profile_day_practised
import bookworm.shared.generated.resources.profile_day_streak
import bookworm.shared.generated.resources.profile_grade
import bookworm.shared.generated.resources.profile_lessons_done
import bookworm.shared.generated.resources.profile_level
import bookworm.shared.generated.resources.profile_level_progress
import bookworm.shared.generated.resources.profile_my_classes
import bookworm.shared.generated.resources.profile_stars_earned
import bookworm.shared.generated.resources.profile_stars_to_level
import bookworm.shared.generated.resources.profile_this_week
import bookworm.shared.generated.resources.profile_weekday_initials
import bookworm.shared.generated.resources.profile_weekday_names
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.domain.model.Badge
import com.dreamteam.bookworm.domain.model.BadgeKind
import com.dreamteam.bookworm.domain.model.ClassProgress
import com.dreamteam.bookworm.domain.model.DayActivity
import com.dreamteam.bookworm.domain.model.MembershipStatus
import com.dreamteam.bookworm.domain.model.StudentClass
import com.dreamteam.bookworm.domain.model.StudentProfile
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui_components.BadgeTile
import com.dreamteam.bookworm.ui_components.BookWormButton
import com.dreamteam.bookworm.ui_components.BookWormCard
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormDialog
import com.dreamteam.bookworm.ui_components.BookWormProgressBar
import com.dreamteam.bookworm.ui_components.ButtonVariant
import com.dreamteam.bookworm.ui_components.LoadingView
import com.dreamteam.bookworm.ui_components.StatTile
import com.dreamteam.bookworm.ui_components.StateContent
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileScreen(onClassClick: (subjectId: String) -> Unit, onJoinClass: () -> Unit, onLogout: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ProfileViewModel(container.profileRepository, container.studentClassesRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    StateContent(state, onRetry = viewModel::retry) { profile ->
        ProfileContent(profile, classes, onClassClick, onJoinClass, onLogoutClick = { confirmLogout = true })
    }

    if (confirmLogout) {
        BookWormDialog(
            title = stringResource(Res.string.auth_logout_title),
            message = stringResource(Res.string.auth_logout_body),
            onDismissRequest = { confirmLogout = false },
            confirmText = stringResource(Res.string.auth_logout),
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            dismissText = stringResource(Res.string.auth_cancel),
            confirmVariant = ButtonVariant.Danger,
        )
    }
}

@Composable
private fun ProfileContent(
    profile: StudentProfile,
    classes: UiState<List<StudentClass>>,
    onClassClick: (String) -> Unit,
    onJoinClass: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(BookWormColors.Background).verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        Header(profile)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Icons.Rounded.LocalFireDepartment, "${profile.streakDays}", stringResource(Res.string.profile_day_streak), Modifier.weight(1f), iconTint = BookWormColors.Flame)
            StatTile(Icons.Rounded.Star, "${profile.stars}", stringResource(Res.string.profile_stars_earned), Modifier.weight(1f))
            StatTile(Icons.Rounded.Check, "${profile.lessonsDone}", stringResource(Res.string.profile_lessons_done), Modifier.weight(1f), iconTint = BookWormColors.Success)
        }

        Section(stringResource(Res.string.profile_this_week)) { Week(profile.week) }

        Section(stringResource(Res.string.profile_classes)) {
            when (classes) {
                UiState.Loading -> LoadingView(Modifier.height(80.dp))
                is UiState.Error -> Text(stringResource(Res.string.common_error_title), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.Danger)
                is UiState.Success -> classes.data.forEach { StudentClassRow(it) }
            }
            BookWormButton(
                stringResource(Res.string.join_class_title),
                onClick = onJoinClass,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                leadingIcon = Icons.Rounded.GroupAdd,
            )
        }

        Section(stringResource(Res.string.profile_my_classes)) {
            profile.classes.forEachIndexed { i, item -> ClassCard(item, i, onClick = { onClassClick(item.subjectId) }) }
        }

        Section(stringResource(Res.string.profile_badges)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.badges.forEach { BadgeView(it, Modifier.weight(1f)) }
            }
        }

        BookWormButton(
            stringResource(Res.string.auth_logout),
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Secondary,
            leadingIcon = Icons.AutoMirrored.Rounded.Logout,
        )
    }
}

@Composable
private fun Header(profile: StudentProfile) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(104.dp).clip(CircleShape).background(BookWormColors.SunSoft).border(3.dp, BookWormColors.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.name.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp), color = BookWormColors.Ink)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(profile.name, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp))
            Text(stringResource(Res.string.profile_grade, profile.grade), style = MaterialTheme.typography.bodyMedium, color = BookWormColors.InkMuted)
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.profile_level, profile.level), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp))
                Text(
                    stringResource(Res.string.profile_stars_to_level, profile.starsToNextLevel, profile.level + 1),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = BookWormColors.InkMuted,
                )
            }
            val progressLabel = stringResource(Res.string.profile_level_progress, (profile.levelProgress * 100).toInt(), profile.level + 1)
            BookWormProgressBar(
                profile.levelProgress,
                Modifier.semantics { contentDescription = progressLabel },
                color = BookWormColors.Primary,
                height = 14.dp,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun Week(days: List<DayActivity>) {
    val initials = stringArrayResource(Res.array.profile_weekday_initials)
    val names = stringArrayResource(Res.array.profile_weekday_names)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BookWormColors.Surface)
            .border(2.dp, BookWormColors.Border, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        days.forEachIndexed { i, day ->
            val name = names.getOrElse(i) { "" }
            val label = if (day.practised) stringResource(Res.string.profile_day_practised, name) else stringResource(Res.string.profile_day_not_yet, name)
            Column(
                Modifier.semantics(mergeDescendants = true) { contentDescription = label },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val ring = when {
                    day.practised -> BookWormColors.Success
                    day.isToday -> BookWormColors.Primary
                    else -> BookWormColors.TrackShadow
                }
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(if (day.practised) BookWormColors.Success else BookWormColors.Surface).border(2.dp, ring, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (day.practised) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(
                    initials.getOrElse(i) { "" },
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = if (day.isToday) BookWormColors.PrimaryDark else BookWormColors.InkMuted,
                )
            }
        }
    }
}

private val classColors = listOf(
    BookWormColors.PrimarySoft to BookWormColors.Primary,
    BookWormColors.SuccessSoft to BookWormColors.Success,
    BookWormColors.SunSoft to Color(0xFFB87500),
)

@Composable
private fun StudentClassRow(item: StudentClass) {
    val waiting = item.status == MembershipStatus.WaitingForTeacher
    BookWormCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeTile("${item.grade}")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.teacherName, style = MaterialTheme.typography.bodySmall, color = BookWormColors.InkMuted)
            }
            if (waiting) {
                Text(
                    stringResource(Res.string.profile_class_waiting),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = BookWormColors.Ink,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(BookWormColors.SunSoft).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ClassCard(item: ClassProgress, index: Int, onClick: () -> Unit) {
    val (tint, bar) = classColors[index % classColors.size]
    BookWormCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeTile(item.subjectName.take(1), background = tint, contentColor = BookWormColors.Ink)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.subjectName, style = MaterialTheme.typography.titleMedium)
                    Text("${(item.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                }
                BookWormProgressBar(item.progress, color = bar)
                Text(
                    stringResource(Res.string.profile_chapter_of, item.currentChapter, item.chapterCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = BookWormColors.InkMuted,
                )
            }
        }
    }
}

private fun BadgeKind.icon(): ImageVector = when (this) {
    BadgeKind.Star -> Icons.Rounded.Star
    BadgeKind.Streak -> Icons.Rounded.LocalFireDepartment
    BadgeKind.Fractions -> Icons.Rounded.PieChart
    BadgeKind.Quiz -> Icons.Rounded.EmojiEvents
}

@Composable
private fun BadgeView(badge: Badge, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(68.dp).clip(CircleShape).background(if (badge.earned) BookWormColors.Sun else BookWormColors.Track),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (badge.earned) badge.kind.icon() else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (badge.earned) BookWormColors.Ink else BookWormColors.LockedIcon,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            if (badge.earned) badge.name else stringResource(Res.string.profile_badge_locked, badge.name),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            color = if (badge.earned) BookWormColors.Ink else BookWormColors.InkMuted,
            textAlign = TextAlign.Center,
        )
    }
}
