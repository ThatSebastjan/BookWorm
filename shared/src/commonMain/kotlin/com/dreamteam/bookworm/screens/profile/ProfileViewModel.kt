package com.dreamteam.bookworm.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.StudentProfile
import com.dreamteam.bookworm.domain.model.StudentClass
import com.dreamteam.bookworm.domain.repository.ProfileRepository
import com.dreamteam.bookworm.domain.repository.StudentClassesRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val classesRepository: StudentClassesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<StudentProfile>>(UiState.Loading)
    val state: StateFlow<UiState<StudentProfile>> = _state.asStateFlow()
    private var job: Job? = null

    /** The student's classes, loaded separately so a failure here doesn't hide the profile. */
    private val _classes = MutableStateFlow<UiState<List<StudentClass>>>(UiState.Loading)
    val classes: StateFlow<UiState<List<StudentClass>>> = _classes.asStateFlow()
    private var classesJob: Job? = null

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    /** Reloads in the background so stars and streaks stay current. */
    fun refresh() = load(showLoading = false)

    private fun load(showLoading: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            if (showLoading || _state.value !is UiState.Success) _state.value = UiState.Loading
            val result = loadState { repository.getProfile() }
            if (result is UiState.Error && !showLoading && _state.value is UiState.Success) return@launch
            _state.value = result
        }
        classesJob?.cancel()
        classesJob = viewModelScope.launch {
            if (showLoading || _classes.value !is UiState.Success) _classes.value = UiState.Loading
            val result = loadState { classesRepository.getMyClasses() }
            if (result is UiState.Error && !showLoading && _classes.value is UiState.Success) return@launch
            _classes.value = result
        }
    }
}
