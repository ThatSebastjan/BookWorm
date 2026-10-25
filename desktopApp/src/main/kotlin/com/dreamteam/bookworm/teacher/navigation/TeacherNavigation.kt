package com.dreamteam.bookworm.teacher.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.graphics.vector.ImageVector
import com.dreamteam.bookworm.teacher.resources.Res
import com.dreamteam.bookworm.teacher.resources.teacher_nav_classroom
import com.dreamteam.bookworm.teacher.resources.teacher_nav_flashcards
import com.dreamteam.bookworm.teacher.resources.teacher_nav_learning_paths
import com.dreamteam.bookworm.teacher.resources.teacher_nav_materials
import com.dreamteam.bookworm.teacher.resources.teacher_nav_pop_quiz
import org.jetbrains.compose.resources.StringResource

/** Sidebar entries of the teacher desktop app. */
enum class TeacherSection(val label: StringResource, val icon: ImageVector) {
    Materials(Res.string.teacher_nav_materials, Icons.Outlined.FileUpload),
    Flashcards(Res.string.teacher_nav_flashcards, Icons.Outlined.Style),
    LearningPaths(Res.string.teacher_nav_learning_paths, Icons.Outlined.Route),
    Classroom(Res.string.teacher_nav_classroom, Icons.Outlined.Groups),
    PopQuiz(Res.string.teacher_nav_pop_quiz, Icons.AutoMirrored.Outlined.HelpOutline),
}

/** Full-window quiz screens that replace the sidebar layout while a quiz runs. */
sealed interface QuizDestination {
    val quizId: String

    data class Live(override val quizId: String) : QuizDestination
    data class Reveal(override val quizId: String) : QuizDestination
}
