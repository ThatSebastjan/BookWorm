package com.dreamteam.bookworm.data.fake

import com.dreamteam.bookworm.domain.model.Answer
import com.dreamteam.bookworm.domain.model.Chapter
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.Flashcard
import com.dreamteam.bookworm.domain.model.Fraction
import com.dreamteam.bookworm.domain.model.Lesson
import com.dreamteam.bookworm.domain.model.LessonStatus
import com.dreamteam.bookworm.domain.model.NoteSection
import com.dreamteam.bookworm.domain.model.Option
import com.dreamteam.bookworm.domain.model.Question
import com.dreamteam.bookworm.domain.model.Subject

/** Sample content from the design canvas. Content comes from teachers, so it is not localised. */
internal object FakeData {

    /** Teaches Class 3B; also the account signed in to the teacher app. */
    const val teacherName = "Ms Novak"

    /** The student signed in to the student app. */
    const val studentId = "mia"

    val subjects = listOf(
        Subject("maths", "Maths"),
        Subject("nature", "Nature"),
        Subject("language", "Language"),
    )

    val chapters = listOf(
        ChapterSummary("maths-1", "maths", 1, "Numbers to 1000", 4, 20, 20),
        ChapterSummary("maths-2", "maths", 2, "Add and subtract", 5, 24, 18),
        ChapterSummary("maths-3", "maths", 3, "Fractions", 4, 4, 0),
        ChapterSummary("maths-4", "maths", 4, "Telling the time", 3, 16, 0),
        ChapterSummary("maths-5", "maths", 5, "Shapes", 4, 14, 0),
        ChapterSummary("nature-1", "nature", 1, "Plants and seeds", 3, 15, 15),
        ChapterSummary("nature-2", "nature", 2, "Animal homes", 4, 18, 6),
        ChapterSummary("nature-3", "nature", 3, "The water cycle", 3, 12, 0),
        ChapterSummary("language-1", "language", 1, "Nouns and verbs", 4, 20, 20),
        ChapterSummary("language-2", "language", 2, "Spelling patterns", 5, 22, 14),
        ChapterSummary("language-3", "language", 3, "Writing a story", 3, 10, 0),
    )

    val currentChapterBySubject = mapOf(
        "maths" to "maths-3",
        "nature" to "nature-2",
        "language" to "language-2",
    )

    val lessonsByChapter: Map<String, List<Pair<String, String>>> = mapOf(
        "maths-3" to listOf(
            "maths-3-halves" to "Halves",
            "maths-3-quarters" to "Quarters",
            "maths-3-comparing" to "Comparing",
            "maths-3-numberline" to "Number line",
            "maths-3-groups" to "Fractions of a group",
            "maths-3-quiz" to "Chapter quiz",
        ),
        "nature-2" to listOf(
            "nature-2-nests" to "Nests",
            "nature-2-burrows" to "Burrows",
            "nature-2-ponds" to "Ponds",
            "nature-2-quiz" to "Chapter quiz",
        ),
        "language-2" to listOf(
            "language-2-double" to "Double letters",
            "language-2-silent" to "Silent letters",
            "language-2-endings" to "Word endings",
            "language-2-quiz" to "Chapter quiz",
        ),
    )

    val lessonStatus: Map<String, LessonStatus> = mapOf(
        "maths-3-halves" to LessonStatus.Done,
        "maths-3-quarters" to LessonStatus.Done,
        "maths-3-comparing" to LessonStatus.Current,
        "nature-2-nests" to LessonStatus.Done,
        "nature-2-burrows" to LessonStatus.Current,
        "language-2-double" to LessonStatus.Done,
        "language-2-silent" to LessonStatus.Done,
        "language-2-endings" to LessonStatus.Current,
    )

    val chapterDetails = mapOf(
        "maths-3" to Chapter(
            id = "maths-3",
            subjectName = "Maths",
            number = 3,
            title = "Fractions",
            teacherName = teacherName,
            notes = listOf(
                NoteSection(
                    "What is a fraction?",
                    "A fraction is a part of a whole. When we cut something into equal parts, each part is a fraction of the whole. " +
                        "The top number is the numerator: the parts we have. The bottom number is the denominator: the parts in all.",
                ),
                NoteSection(
                    "Equal parts",
                    "The parts must all be the same size. If one piece is bigger than the other, they are not halves.",
                    remember = "The more equal parts there are, the smaller each part is. A quarter is smaller than a half.",
                ),
            ),
            flashcards = listOf(
                Flashcard("fc-numerator", "Numerator", "The top number. It tells how many parts we have."),
                Flashcard("fc-denominator", "Denominator", "The bottom number. It tells how many equal parts make the whole."),
                Flashcard("fc-half", "Half", "One of two equal parts. We write it as 1/2."),
                Flashcard("fc-quarter", "Quarter", "One of four equal parts. We write it as 1/4."),
            ),
        ),
    )

    /** For chapters without written content yet, including ones teachers create. */
    fun placeholderChapter(summary: ChapterSummary): Chapter {
        return Chapter(
            id = summary.id,
            subjectName = subjects.first { it.id == summary.subjectId }.name,
            number = summary.number,
            title = summary.title,
            teacherName = teacherName,
            notes = listOf(NoteSection(summary.title, "Your teacher hasn't added notes for this chapter yet.")),
            flashcards = emptyList(),
        )
    }

    private fun frac(id: String, n: Int, d: Int) = Option(id, "$n/$d", Fraction(n, d))

    val sampleLesson = Lesson(
        id = "",
        title = "Comparing",
        questions = listOf(
            Question.SingleChoice(
                "q-smallest", "Which fraction is the smallest?",
                listOf(frac("a", 1, 2), frac("b", 1, 4), frac("c", 1, 8), frac("d", 1, 3)),
            ),
            Question.MultiChoice(
                "q-half", "Which fractions are equal to one half?",
                listOf(frac("a", 2, 4), frac("b", 1, 3), frac("c", 3, 6), frac("d", 2, 3)),
            ),
            Question.TypedAnswer(
                "q-pizza", "A pizza is cut into 8 equal slices. You eat 1 slice. What fraction of the pizza did you eat?",
                fractionInput = true,
            ),
            Question.FillBlank(
                "q-blank", "Tap a word to complete the sentence.",
                textBefore = "A fraction has a numerator on the top and a",
                textAfter = "on the bottom.",
                words = listOf("denominator", "whole", "quarter"),
            ),
            Question.Order(
                "q-order", "Order the fractions from smallest to biggest.",
                listOf(frac("x", 1, 2), frac("y", 1, 8), frac("z", 1, 4), frac("w", 1, 3)),
            ),
            Question.Match(
                "q-match", "Match each fraction to its meaning.",
                left = listOf(frac("A", 1, 2), frac("B", 1, 4), frac("C", 3, 4)),
                right = listOf(
                    Option("R1", "3 of 4 equal parts"),
                    Option("R2", "1 of 2 equal parts"),
                    Option("R3", "1 of 4 equal parts"),
                ),
            ),
        ),
    )

    class Key(val answer: Answer, val explanation: String, val retryHint: String)

    val answerKey = mapOf(
        "q-smallest" to Key(
            Answer.Choice("c"),
            "The more equal parts, the smaller each part. Eighths are the smallest here.",
            "The bigger the bottom number, the smaller the piece. Which bottom number is biggest?",
        ),
        "q-half" to Key(
            Answer.Choices(setOf("a", "c")),
            "2/4 and 3/6 both show the same amount as 1/2.",
            "Pick every fraction where the top number is half of the bottom number.",
        ),
        "q-pizza" to Key(
            Answer.Text("1/8"),
            "1 slice out of 8 equal slices is 1/8.",
            "The bottom number is how many slices there are in all. The top number is how many you ate.",
        ),
        "q-blank" to Key(
            Answer.Text("denominator"),
            "The denominator is the bottom number. It tells how many equal parts make the whole.",
            "Remember: the bottom number tells how many equal parts make the whole.",
        ),
        "q-order" to Key(
            Answer.Ordered(listOf("y", "z", "w", "x")),
            "1/8 is smallest and 1/2 is biggest: more parts means smaller pieces.",
            "Look at the bottom numbers. The biggest bottom number is the smallest fraction.",
        ),
        "q-match" to Key(
            Answer.Pairs(mapOf("A" to "R2", "B" to "R3", "C" to "R1")),
            "Great matching! The bottom number is the parts in all, the top is the parts we have.",
            "The bottom number tells how many equal parts, the top tells how many we take.",
        ),
    )
}
