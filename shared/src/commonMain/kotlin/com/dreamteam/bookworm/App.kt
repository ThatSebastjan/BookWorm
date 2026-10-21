package com.dreamteam.bookworm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import com.dreamteam.bookworm.ui_components.BookWormTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.dreamteam.bookworm.screens.LibraryScreen
import com.dreamteam.bookworm.screens.ProfileScreen
import com.dreamteam.bookworm.screens.SearchScreen

enum class Tab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    Library("Library", Icons.AutoMirrored.Filled.LibraryBooks,
        Icons.AutoMirrored.Outlined.LibraryBooks
    ),
    Search("Search", Icons.Filled.Search, Icons.Outlined.Search),
    Profile("Profile", Icons.Filled.Person, Icons.Outlined.Person),
}
@Composable
@Preview
fun App() {
    BookWormTheme {
        var currentTab by rememberSaveable { mutableStateOf(Tab.Library) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (currentTab) {
                    Tab.Library -> LibraryScreen()
                    Tab.Search -> SearchScreen()
                    Tab.Profile -> ProfileScreen()
                }
            }
        }
    }
}

