package com.dreamteam.bookworm.teacher.screens.classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.ClassGroup
import com.dreamteam.bookworm.domain.model.ClassStudent
import com.dreamteam.bookworm.domain.model.Classroom
import com.dreamteam.bookworm.domain.model.StudentStatus
import com.dreamteam.bookworm.domain.repository.ClassroomRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassroomUiState(
    val classroom: UiState<Classroom> = UiState.Loading,
    val query: String = "",
    val showAddStudents: Boolean = false,
    /** The group in the edit dialog; an empty id means a new group. Null when the dialog is closed. */
    val editingGroup: ClassGroup? = null,
    val deletingGroup: ClassGroup? = null,
    val linkCopied: Boolean = false,
    val actionFailed: Boolean = false,
) {
    private val students: List<ClassStudent> get() = (classroom as? UiState.Success)?.data?.students.orEmpty()
    val visibleStudents: List<ClassStudent>
        get() = if (query.isBlank()) students else students.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val inClass: Int get() = students.count { it.status == StudentStatus.InClass }
    val waiting: Int get() = students.count { it.status == StudentStatus.Waiting }
    val total: Int get() = students.size
}

class ClassroomViewModel(
    private val classId: String,
    private val repository: ClassroomRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ClassroomUiState())
    val state: StateFlow<ClassroomUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(classroom = UiState.Loading) }
            val classroom = loadState { repository.getClassroom(classId) }
            _state.update { it.copy(classroom = classroom) }
        }
    }

    fun search(query: String) = _state.update { it.copy(query = query) }

    fun admit(studentId: String) {
        viewModelScope.launch {
            when (val result = loadState { repository.admit(classId, studentId) }) {
                is UiState.Success -> updateClassroom { classroom ->
                    classroom.copy(students = classroom.students.map { if (it.id == studentId) result.data else it })
                }
                is UiState.Error -> _state.update { it.copy(actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    fun setApproveJoins(approve: Boolean) {
        val before = (_state.value.classroom as? UiState.Success)?.data?.approveJoins ?: return
        updateClassroom { it.copy(approveJoins = approve) }
        viewModelScope.launch {
            if (loadState { repository.setApproveJoins(classId, approve) } is UiState.Error) {
                updateClassroom { it.copy(approveJoins = before) }
                _state.update { it.copy(actionFailed = true) }
            }
        }
    }

    fun showAddStudents(show: Boolean) = _state.update { it.copy(showAddStudents = show) }

    fun newGroup() = _state.update { it.copy(editingGroup = ClassGroup(id = "", name = "", studentIds = emptyList())) }

    fun editGroup(group: ClassGroup) = _state.update { it.copy(editingGroup = group) }

    fun closeGroupDialog() = _state.update { it.copy(editingGroup = null) }

    fun saveGroup(group: ClassGroup) {
        _state.update { it.copy(editingGroup = null) }
        viewModelScope.launch {
            when (val saved = loadState { repository.saveGroup(classId, group.copy(name = group.name.trim())) }) {
                is UiState.Success -> updateClassroom { classroom ->
                    val exists = classroom.groups.any { it.id == saved.data.id }
                    classroom.copy(groups = if (exists) classroom.groups.map { if (it.id == saved.data.id) saved.data else it } else classroom.groups + saved.data)
                }
                is UiState.Error -> _state.update { it.copy(actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    fun askDeleteGroup(group: ClassGroup?) = _state.update { it.copy(deletingGroup = group) }

    /** Reloads afterwards: the group's learning path assignments go with it. */
    fun deleteGroup() {
        val group = _state.value.deletingGroup ?: return
        _state.update { it.copy(deletingGroup = null) }
        viewModelScope.launch {
            if (loadState { repository.deleteGroup(classId, group.id) } is UiState.Error) {
                _state.update { it.copy(actionFailed = true) }
            } else {
                val refreshed = loadState { repository.getClassroom(classId) }
                if (refreshed is UiState.Success) _state.update { it.copy(classroom = refreshed) }
            }
        }
    }

    fun linkCopied() = _state.update { it.copy(linkCopied = true) }

    fun dismissError() = _state.update { it.copy(actionFailed = false) }

    private fun updateClassroom(change: (Classroom) -> Classroom) = _state.update { current ->
        val classroom = (current.classroom as? UiState.Success)?.data ?: return@update current
        current.copy(classroom = UiState.Success(change(classroom)), actionFailed = false)
    }
}
