package com.dreamteam.bookworm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dreamteam.bookworm.di.AppContainer
import com.dreamteam.bookworm.di.LocalAppContainer
import com.dreamteam.bookworm.navigation.Destination
import com.dreamteam.bookworm.navigation.ScopedViewModelStore
import com.dreamteam.bookworm.navigation.Tab
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamteam.bookworm.screens.auth.LoginScreen
import com.dreamteam.bookworm.screens.auth.RegisterScreen
import com.dreamteam.bookworm.screens.chapter.ChapterScreen
import com.dreamteam.bookworm.screens.classes.JoinClassScreen
import kotlinx.coroutines.launch
import com.dreamteam.bookworm.screens.learn.LearnScreen
import com.dreamteam.bookworm.screens.lesson.LessonScreen
import com.dreamteam.bookworm.screens.library.LibraryScreen
import com.dreamteam.bookworm.screens.profile.ProfileScreen
import com.dreamteam.bookworm.ui.PlatformBackHandler
import com.dreamteam.bookworm.ui_components.BookWormColors
import com.dreamteam.bookworm.ui_components.BookWormTheme
import org.jetbrains.compose.resources.stringResource

@Composable
@Preview
fun App(container: AppContainer = remember { AppContainer.fake() }) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        BookWormTheme {
            val session by container.authRepository.session.collectAsStateWithLifecycle()
            val signedIn = session
            if (signedIn == null) {
                AuthFlow()
            } else {
                // A fresh ViewModel store per user, so nothing from a previous account lingers after logging out.
                ScopedViewModelStore(signedIn.userId) { MainContent(container) }
            }
        }
    }
}

/** Login and register, switching between the two without a back stack. */
@Composable
private fun AuthFlow() {
    var registering by rememberSaveable { mutableStateOf(false) }
    PlatformBackHandler(enabled = registering) { registering = false }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.padding(padding)) {
            if (registering) {
                ScopedViewModelStore("register") { RegisterScreen(onLoginClick = { registering = false }) }
            } else {
                ScopedViewModelStore("login") { LoginScreen(onRegisterClick = { registering = true }) }
            }
        }
    }
}

@Composable
private fun MainContent(container: AppContainer) {
    val scope = rememberCoroutineScope()
    var currentTab by rememberSaveable { mutableStateOf(Tab.Learn) }
    val backStack = remember { mutableStateListOf<Destination>() }
    val top = backStack.lastOrNull()

    fun push(destination: Destination) = backStack.add(destination)
    fun pop() = backStack.removeLastOrNull()

    PlatformBackHandler(enabled = backStack.isNotEmpty()) { pop() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (top?.showsBottomBar != false) {
                BottomBar(currentTab) { tab ->
                    backStack.clear()
                    currentTab = tab
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (top) {
                null -> when (currentTab) {
                    Tab.Learn -> LearnScreen(
                        onLessonClick = { push(Destination.Lesson(it)) },
                        onChapterClick = { push(Destination.Chapter(it)) },
                    )
                    Tab.Library -> LibraryScreen(onChapterClick = { push(Destination.Chapter(it)) })
                    Tab.Profile -> ProfileScreen(
                        onClassClick = { currentTab = Tab.Library },
                        onJoinClass = { push(Destination.JoinClass) },
                        onLogout = { scope.launch { container.authRepository.logout() } },
                    )
                }
                is Destination.Chapter -> ScopedViewModelStore(top) {
                    ChapterScreen(top.chapterId, onBack = { pop() })
                }
                is Destination.Lesson -> ScopedViewModelStore(top) {
                    LessonScreen(top.lessonId, onClose = { pop() })
                }
                Destination.JoinClass -> ScopedViewModelStore(top) {
                    JoinClassScreen(onDone = { pop() })
                }
            }
        }
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = BookWormColors.Surface) {
        Tab.entries.forEach { tab ->
            val selected = current == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = null)
                },
                label = { Text(stringResource(tab.label), style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BookWormColors.PrimaryDark,
                    selectedTextColor = BookWormColors.PrimaryDark,
                    indicatorColor = BookWormColors.PrimarySoft,
                    unselectedIconColor = BookWormColors.InkMuted,
                    unselectedTextColor = BookWormColors.InkMuted,
                ),
            )
        }
    }
}
