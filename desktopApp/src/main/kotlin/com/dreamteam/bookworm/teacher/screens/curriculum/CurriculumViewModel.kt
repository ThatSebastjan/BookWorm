package com.dreamteam.bookworm.teacher.screens.curriculum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.Curriculum
import com.dreamteam.bookworm.domain.model.CurriculumLesson
import com.dreamteam.bookworm.domain.model.CurriculumLibrary
import com.dreamteam.bookworm.domain.model.CurriculumUnit
import com.dreamteam.bookworm.domain.model.MAX_LESSON_STARS
import com.dreamteam.bookworm.domain.model.MIN_LESSON_STARS
import com.dreamteam.bookworm.domain.model.PathProgress
import com.dreamteam.bookworm.domain.model.UnlockMode
import com.dreamteam.bookworm.domain.repository.CurriculumRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CurriculumUiState(
    val library: UiState<CurriculumLibrary> = UiState.Loading,
    /** The path being edited, with unsaved changes. Null when none is open. */
    val draft: Curriculum? = null,
    val loadingDraft: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val justSaved: Boolean = false,
    val actionFailed: Boolean = false,
    val showNewDialog: Boolean = false,
    val confirmDelete: Boolean = false,
    /** A path the teacher clicked while the open one has unsaved changes. */
    val pendingSelection: String? = null,
    /** Showing how students are getting on with the open path, instead of editing it. */
    val showProgress: Boolean = false,
    /** Of the open path as last saved; null before it was first saved. */
    val progress: UiState<PathProgress>? = null,
) {
    val canSave: Boolean get() = draft != null && dirty && draft.name.isNotBlank() && !saving
}

class CurriculumViewModel(
    private val repository: CurriculumRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CurriculumUiState())
    val state: StateFlow<CurriculumUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(library = UiState.Loading) }
            val library = loadState { repository.getLibrary() }
            _state.update { it.copy(library = library) }
            val first = (library as? UiState.Success)?.data?.curricula?.firstOrNull()
            if (_state.value.draft == null && first != null) open(first.id)
        }
    }

    /** Opens another path, asking first when the open one has unsaved changes. */
    fun select(curriculumId: String) {
        if (curriculumId == _state.value.draft?.id) return
        if (_state.value.dirty) _state.update { it.copy(pendingSelection = curriculumId) } else open(curriculumId)
    }

    fun discardAndOpenPending() {
        val pending = _state.value.pendingSelection ?: return
        _state.update { it.copy(pendingSelection = null, dirty = false) }
        open(pending)
    }

    fun keepEditing() = _state.update { it.copy(pendingSelection = null) }

    private fun open(curriculumId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingDraft = true, justSaved = false, actionFailed = false) }
            when (val loaded = loadState { repository.getCurriculum(curriculumId) }) {
                is UiState.Success -> {
                    _state.update { it.copy(draft = loaded.data, loadingDraft = false, dirty = false, progress = null) }
                    if (_state.value.showProgress) loadProgress()
                }
                is UiState.Error -> _state.update { it.copy(loadingDraft = false, actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    // Student progress

    fun showProgress(show: Boolean) {
        _state.update { it.copy(showProgress = show) }
        if (show && _state.value.progress !is UiState.Success) loadProgress()
    }

    fun loadProgress() {
        val id = _state.value.draft?.id?.takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            _state.update { it.copy(progress = UiState.Loading) }
            val progress = loadState { repository.getProgress(id) }
            // The teacher may have opened another path meanwhile.
            _state.update { if (it.draft?.id == id) it.copy(progress = progress) else it }
        }
    }

    /** After the lesson editor: reload the open path so its question counts are current, unless it has unsaved changes. */
    fun refreshQuestionCounts() {
        val current = _state.value
        val id = current.draft?.id ?: return
        if (id.isNotEmpty() && !current.dirty) open(id)
    }

    // New path

    fun showNewDialog(show: Boolean) = _state.update { it.copy(showNewDialog = show) }

    /** Starts an unsaved path through class [subjectId]; it is created on the server with the first save. */
    fun startNew(name: String, subjectId: String) = _state.update {
        it.copy(
            // The grade comes from the class once saved.
            draft = Curriculum("", name.trim(), subjectId, grade = 0, UnlockMode.InOrder, emptyList(), emptyList()),
            dirty = true,
            showNewDialog = false,
            justSaved = false,
            showProgress = false,
            progress = null,
        )
    }

    // Editing

    fun rename(name: String) = edit { it.copy(name = name) }

    fun setUnlockMode(mode: UnlockMode) = edit { it.copy(unlockMode = mode) }

    fun toggleTarget(classroomId: String) = edit {
        it.copy(assignedTo = if (classroomId in it.assignedTo) it.assignedTo - classroomId else it.assignedTo + classroomId)
    }

    /** Adds [chapter] at the end with a practice lesson and a chapter quiz, titled in the teacher's language. */
    fun addChapter(chapter: ChapterSummary, practiseTitle: String, quizTitle: String) = edit { path ->
        if (path.units.any { it.chapter.id == chapter.id }) return@edit path
        val lessons = listOf(CurriculumLesson("", practiseTitle), CurriculumLesson("", quizTitle, isChapterQuiz = true))
        path.copy(units = path.units + CurriculumUnit(chapter, lessons))
    }

    /** Creates a new chapter at the end of the path's class and adds it to the path. */
    fun createChapter(title: String, practiseTitle: String, quizTitle: String) {
        val subjectId = _state.value.draft?.subjectId ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            when (val created = loadState { repository.createChapter(subjectId, title.trim()) }) {
                is UiState.Success -> {
                    _state.update { current ->
                        val library = (current.library as? UiState.Success)?.data ?: return@update current
                        current.copy(library = UiState.Success(library.copy(chapters = library.chapters + created.data)))
                    }
                    addChapter(created.data, practiseTitle, quizTitle)
                }
                is UiState.Error -> _state.update { it.copy(actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    fun removeUnit(unitIndex: Int) = editUnits { it.removeAt(unitIndex) }

    fun moveUnit(unitIndex: Int, by: Int) = editUnits { it.move(unitIndex, by) }

    fun addLesson(unitIndex: Int, title: String) = editLessons(unitIndex) { lessons ->
        // New lessons go before the chapter quiz, which stays last.
        val quizAt = lessons.indexOfFirst { it.isChapterQuiz }
        lessons.add(if (quizAt >= 0) quizAt else lessons.size, CurriculumLesson("", title))
    }

    fun renameLesson(unitIndex: Int, lessonIndex: Int, title: String) = editLessons(unitIndex) {
        it[lessonIndex] = it[lessonIndex].copy(title = title)
    }

    fun toggleQuiz(unitIndex: Int, lessonIndex: Int) = editLessons(unitIndex) {
        it[lessonIndex] = it[lessonIndex].copy(isChapterQuiz = !it[lessonIndex].isChapterQuiz)
    }

    /** Stars for finishing the lesson the first time; anything but a number is ignored, and it is kept in range. */
    fun setLessonStars(unitIndex: Int, lessonIndex: Int, text: String) = editLessons(unitIndex) {
        val stars = text.filter(Char::isDigit).take(3).toIntOrNull() ?: MIN_LESSON_STARS
        it[lessonIndex] = it[lessonIndex].copy(stars = stars.coerceIn(MIN_LESSON_STARS, MAX_LESSON_STARS))
    }

    fun removeLesson(unitIndex: Int, lessonIndex: Int) = editLessons(unitIndex) { it.removeAt(lessonIndex) }

    fun moveLesson(unitIndex: Int, lessonIndex: Int, by: Int) = editLessons(unitIndex) { it.move(lessonIndex, by) }

    private fun edit(change: (Curriculum) -> Curriculum) = _state.update { current ->
        val draft = current.draft ?: return@update current
        val changed = change(draft)
        if (changed == draft) current else current.copy(draft = changed, dirty = true, justSaved = false)
    }

    private fun editUnits(change: (MutableList<CurriculumUnit>) -> Unit) = edit { path ->
        path.copy(units = path.units.toMutableList().also(change))
    }

    private fun editLessons(unitIndex: Int, change: (MutableList<CurriculumLesson>) -> Unit) = editUnits { units ->
        units[unitIndex] = units[unitIndex].copy(lessons = units[unitIndex].lessons.toMutableList().also(change))
    }

    private fun <T> MutableList<T>.move(index: Int, by: Int) {
        val target = index + by
        if (target !in indices) return
        add(target, removeAt(index))
    }

    // Saving

    fun save() {
        val draft = _state.value.draft ?: return
        if (!_state.value.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, actionFailed = false) }
            when (val saved = loadState { repository.saveCurriculum(draft.copy(name = draft.name.trim())) }) {
                is UiState.Success -> {
                    _state.update { it.copy(draft = saved.data, saving = false, dirty = false, justSaved = true, progress = null) }
                    // Saving can move classrooms off other paths of the same class, so refresh the list.
                    refreshLibrary()
                }
                is UiState.Error -> _state.update { it.copy(saving = false, actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    fun askDelete(ask: Boolean) = _state.update { it.copy(confirmDelete = ask) }

    fun delete() {
        val id = _state.value.draft?.id ?: return
        _state.update { it.copy(confirmDelete = false) }
        if (id.isEmpty()) {
            // Never saved: just drop the draft.
            _state.update { it.copy(draft = null, dirty = false) }
            return
        }
        viewModelScope.launch {
            if (loadState { repository.deleteCurriculum(id) } is UiState.Error) {
                _state.update { it.copy(actionFailed = true) }
            } else {
                _state.update { it.copy(draft = null, dirty = false) }
                load()
            }
        }
    }

    private suspend fun refreshLibrary() {
        val library = loadState { repository.getLibrary() }
        if (library is UiState.Success) _state.update { it.copy(library = library) }
    }
}
