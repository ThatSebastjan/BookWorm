package com.dreamteam.bookworm.domain.model

data class Subject(
    val id: String,
    val name: String,
)

data class ChapterSummary(
    val id: String,
    val subjectId: String,
    val number: Int,
    val title: String,
    val noteCount: Int,
    val flashcardCount: Int,
    val knownFlashcards: Int,
) {
    val mastery: Float get() = if (flashcardCount == 0) 0f else knownFlashcards.toFloat() / flashcardCount
}

data class Chapter(
    val id: String,
    val subjectName: String,
    val number: Int,
    val title: String,
    val teacherName: String,
    val notes: List<NoteSection>,
    val flashcards: List<Flashcard>,
    /** Cards the student knew the last time they practised them. Practice only shows the others. */
    val knownFlashcardIds: Set<String> = emptySet(),
)

data class NoteSection(
    val heading: String,
    val body: String,
    /** Optional highlighted "Remember" box under the body. */
    val remember: String? = null,
)

data class Flashcard(
    val id: String,
    val term: String,
    val definition: String,
)
