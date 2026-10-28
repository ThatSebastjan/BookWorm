package com.dreamteam.bookworm.teacher.screens.classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.AddTeacherException
import com.dreamteam.bookworm.domain.model.AddTeacherFailure
import com.dreamteam.bookworm.domain.model.ClassStudent
import com.dreamteam.bookworm.domain.model.ClassroomTeacher
import com.dreamteam.bookworm.domain.model.Classroom
import com.dreamteam.bookworm.domain.model.StudentStatus
import com.dreamteam.bookworm.domain.repository.ClassroomRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassroomUiState(
    val classroom: UiState<Classroom> = UiState.Loading,
    val query: String = "",
    val showAddStudents: Boolean = false,
    /** The student the teacher is about to remove (or turn down, if waiting). */
    val removing: ClassStudent? = null,
    /** Email typed into "Add teacher". */
    val teacherEmail: String = "",
    val addTeacherFailure: AddTeacherFailure? = null,
    /** The co-teacher the owner is about to remove. */
    val removingTeacher: ClassroomTeacher? = null,
    /** The co-teacher is about to leave the classroom. */
    val confirmLeave: Boolean = false,
    /** The co-teacher left; the screen closes. */
    val left: Boolean = false,
    /** A change is on its way to the server; the controls wait for it. */
    val busy: Boolean = false,
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

    fun admit(studentId: String) = change { repository.admit(classId, studentId) }

    fun askRemove(student: ClassStudent?) = _state.update { it.copy(removing = student) }

    fun remove() {
        val student = _state.value.removing ?: return
        _state.update { it.copy(removing = null) }
        change { repository.removeStudent(classId, student.id) }
    }

    fun setApproveJoins(approve: Boolean) = change { repository.setApproveJoins(classId, approve) }

    // Teachers

    fun setTeacherEmail(email: String) = _state.update { it.copy(teacherEmail = email, addTeacherFailure = null) }

    fun addTeacher() {
        val email = _state.value.teacherEmail.trim()
        if (email.isEmpty() || _state.value.busy) return
        _state.update { it.copy(busy = true, actionFailed = false, addTeacherFailure = null) }
        viewModelScope.launch {
            try {
                val classroom = repository.addTeacher(classId, email)
                _state.update { it.copy(classroom = UiState.Success(classroom), busy = false, teacherEmail = "") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AddTeacherException) {
                _state.update { it.copy(busy = false, addTeacherFailure = e.failure) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, actionFailed = true) }
            }
        }
    }

    fun askRemoveTeacher(teacher: ClassroomTeacher?) = _state.update { it.copy(removingTeacher = teacher) }

    fun removeTeacher() {
        val teacher = _state.value.removingTeacher ?: return
        _state.update { it.copy(removingTeacher = null) }
        change { repository.removeTeacher(classId, teacher.id) ?: error("Removing a co-teacher returns the classroom") }
    }

    fun askLeave(ask: Boolean) = _state.update { it.copy(confirmLeave = ask) }

    /** A co-teacher leaves; [myId] is their own user id. */
    fun leave(myId: String) {
        _state.update { it.copy(confirmLeave = false, busy = true, actionFailed = false) }
        viewModelScope.launch {
            if (loadState { repository.removeTeacher(classId, myId) } is UiState.Error) {
                _state.update { it.copy(busy = false, actionFailed = true) }
            } else {
                _state.update { it.copy(busy = false, left = true) }
            }
        }
    }

    /** Starts or stops using class [subjectId]. Stopping also drops the classroom's path for it. */
    fun toggleClass(subjectId: String) {
        val current = (_state.value.classroom as? UiState.Success)?.data?.classIds ?: return
        val next = if (subjectId in current) current - subjectId else current + subjectId
        change { repository.setClasses(classId, next) }
    }

    fun showAddStudents(show: Boolean) = _state.update { it.copy(showAddStudents = show) }

    fun linkCopied() = _state.update { it.copy(linkCopied = true) }

    /** Runs [request] and shows the classroom it returns; on failure keeps what is shown and says so. */
    private fun change(request: suspend () -> Classroom) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, actionFailed = false) }
        viewModelScope.launch {
            when (val result = loadState { request() }) {
                is UiState.Success -> _state.update { it.copy(classroom = result, busy = false) }
                is UiState.Error -> _state.update { it.copy(busy = false, actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }
}
