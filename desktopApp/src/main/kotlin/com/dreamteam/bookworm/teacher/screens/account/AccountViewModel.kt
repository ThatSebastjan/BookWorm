package com.dreamteam.bookworm.teacher.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.ClassSummary
import com.dreamteam.bookworm.domain.repository.TeacherRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The classrooms the teacher teaches, for their Profile page. Name and password are
 * [com.dreamteam.bookworm.screens.account.EditProfileViewModel], shared with the student app.
 */
class AccountViewModel(private val teacherRepository: TeacherRepository) : ViewModel() {

    private val _classrooms = MutableStateFlow<UiState<List<ClassSummary>>>(UiState.Loading)
    val classrooms: StateFlow<UiState<List<ClassSummary>>> = _classrooms.asStateFlow()

    init {
        loadClassrooms()
    }

    fun loadClassrooms() {
        viewModelScope.launch {
            _classrooms.value = UiState.Loading
            _classrooms.value = loadState { teacherRepository.getTeacher().classes }
        }
    }
}
