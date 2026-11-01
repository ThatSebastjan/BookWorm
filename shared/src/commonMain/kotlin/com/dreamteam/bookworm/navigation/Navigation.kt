package com.dreamteam.bookworm.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import bookworm.shared.generated.resources.Res
import bookworm.shared.generated.resources.nav_learn
import bookworm.shared.generated.resources.nav_library
import bookworm.shared.generated.resources.nav_profile
import org.jetbrains.compose.resources.StringResource

enum class Tab(val label: StringResource, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    Learn(Res.string.nav_learn, Icons.Filled.School, Icons.Outlined.School),
    Library(Res.string.nav_library, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    Profile(Res.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
}

/** Screens pushed on top of a tab. */
sealed interface Destination {
    /** Shows the bottom bar underneath, like the canvas does for chapters. */
    val showsBottomBar: Boolean

    data class Chapter(val chapterId: String) : Destination {
        override val showsBottomBar = true
    }

    data class Lesson(val lessonId: String) : Destination {
        override val showsBottomBar = false
    }

    data object JoinClass : Destination {
        override val showsBottomBar = false
    }

    data object EditProfile : Destination {
        override val showsBottomBar = false
    }
}

/**
 * Gives a pushed screen its own ViewModelStore, cleared when the screen leaves
 * composition, so its ViewModels don't outlive it. Tabs use the root store instead
 * and keep their state while switching tabs.
 */
@Composable
fun ScopedViewModelStore(key: Any, content: @Composable () -> Unit) {
    val owner = remember(key) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
