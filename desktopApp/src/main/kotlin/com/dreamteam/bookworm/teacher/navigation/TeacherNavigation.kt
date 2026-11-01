package com.dreamteam.bookworm.teacher.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Route
import androidx.compose.ui.graphics.vector.ImageVector
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.teacher_nav_classroom
import com.dreamteam.bookworm.teacher.resources.teacher_nav_learning_paths
import com.dreamteam.bookworm.teacher.resources.teacher_nav_classes
import com.dreamteam.bookworm.teacher.resources.teacher_nav_profile
import org.jetbrains.compose.resources.StringResource

/** Sidebar entries of the teacher desktop app. Classes hold the content; classrooms hold the students. */
enum class TeacherSection(val label: StringResource, val icon: ImageVector) {
    Classes(Res.string.teacher_nav_classes, Icons.Outlined.AutoStories),
    LearningPaths(Res.string.teacher_nav_learning_paths, Icons.Outlined.Route),
    Classroom(Res.string.teacher_nav_classroom, Icons.Outlined.Groups),
    Profile(Res.string.teacher_nav_profile, Icons.Outlined.AccountCircle),
}

