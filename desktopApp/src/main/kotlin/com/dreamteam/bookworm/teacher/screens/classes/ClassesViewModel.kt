package com.dreamteam.bookworm.teacher.screens.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.TeacherClass
import com.dreamteam.bookworm.domain.repository.ClassesRepository
import com.dreamteam.bookworm.domain.repository.TeacherRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The class dialog: a new class, or editing an existing one. */
sealed interface ClassDialog {
    data object New : ClassDialog
    data class Edit(val schoolClass: TeacherClass) : ClassDialog
}

data class ClassesUiState(
    val classes: UiState<List<TeacherClass>> = UiState.Loading,
    /** Names of the teacher's classrooms, to say which ones use a class. */
    val classroomNames: Map<String, String> = emptyMap(),
    val selectedId: String? = null,
    val dialog: ClassDialog? = null,
    val deletingClass: TeacherClass? = null,
    val deletingChapter: ChapterSummary? = null,
    val newChapterTitle: String = "",
    val busy: Boolean = false,
    val actionFailed: Boolean = false,
) {
    val selected: TeacherClass?
        get() = (classes as? UiState.Success)?.data?.let { list -> list.firstOrNull { it.id == selectedId } ?: list.firstOrNull() }
}

class ClassesViewModel(
    private val repository: ClassesRepository,
    private val teacherRepository: TeacherRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ClassesUiState())
    val state: StateFlow<ClassesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(classes = UiState.Loading) }
            val classes = loadState { repository.getClasses() }
            _state.update { it.copy(classes = classes) }
            val teacher = loadState { teacherRepository.getTeacher() }
            if (teacher is UiState.Success) {
                _state.update { it.copy(classroomNames = teacher.data.classes.associate { room -> room.id to room.name }) }
            }
        }
    }

    /** Reloads in the background, e.g. after editing a chapter. Keeps the list if it fails. */
    fun refresh() {
        viewModelScope.launch {
            val classes = loadState { repository.getClasses() }
            if (classes is UiState.Success) _state.update { it.copy(classes = classes) }
        }
    }

    fun select(classId: String) = _state.update { it.copy(selectedId = classId, newChapterTitle = "", actionFailed = false) }

    // Classes

    fun showDialog(dialog: ClassDialog?) = _state.update { it.copy(dialog = dialog) }

    fun saveClass(name: String, grade: Int) {
        val dialog = _state.value.dialog ?: return
        _state.update { it.copy(dialog = null) }
        when (dialog) {
            ClassDialog.New -> change(
                request = { repository.createClass(name, grade) },
                onDone = { created -> _state.update { it.copy(selectedId = created.id) } },
            )
            is ClassDialog.Edit -> change(request = { repository.updateClass(dialog.schoolClass.id, name, grade) })
        }
    }

    fun askDeleteClass(schoolClass: TeacherClass?) = _state.update { it.copy(deletingClass = schoolClass) }

    fun deleteClass() {
        val schoolClass = _state.value.deletingClass ?: return
        _state.update { it.copy(deletingClass = null) }
        change(
            request = { repository.deleteClass(schoolClass.id) },
            onDone = { _state.update { it.copy(selectedId = null) } },
        )
    }

    // Chapters

    fun setNewChapterTitle(title: String) = _state.update { it.copy(newChapterTitle = title) }

    fun addChapter() {
        val schoolClass = _state.value.selected ?: return
        val title = _state.value.newChapterTitle.trim()
        if (title.isEmpty()) return
        change(
            request = { repository.addChapter(schoolClass.id, title) },
            onDone = { _state.update { it.copy(newChapterTitle = "") } },
        )
    }

    fun moveChapter(index: Int, by: Int) {
        val schoolClass = _state.value.selected ?: return
        val ids = schoolClass.chapters.map { it.id }.toMutableList()
        val target = index + by
        if (target !in ids.indices) return
        ids.add(target, ids.removeAt(index))
        change(request = { repository.reorderChapters(schoolClass.id, ids) })
    }

    fun askDeleteChapter(chapter: ChapterSummary?) = _state.update { it.copy(deletingChapter = chapter) }

    fun deleteChapter() {
        val chapter = _state.value.deletingChapter ?: return
        _state.update { it.copy(deletingChapter = null) }
        change(request = { repository.deleteChapter(chapter.id) })
    }

    /** Runs [request], then reloads the classes. On failure keeps what is shown and says so. */
    private fun <T> change(request: suspend () -> T, onDone: (T) -> Unit = {}) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, actionFailed = false) }
        viewModelScope.launch {
            when (val result = loadState { request() }) {
                is UiState.Success -> {
                    onDone(result.data)
                    val classes = loadState { repository.getClasses() }
                    _state.update { it.copy(busy = false, classes = (classes as? UiState.Success) ?: it.classes) }
                }
                is UiState.Error -> _state.update { it.copy(busy = false, actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }
}
