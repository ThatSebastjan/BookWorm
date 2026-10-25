package com.dreamteam.bookworm.data.fake

import com.dreamteam.bookworm.domain.model.Answer
import com.dreamteam.bookworm.domain.model.AssignmentTarget
import com.dreamteam.bookworm.domain.model.AuthoredQuestion
import com.dreamteam.bookworm.domain.model.ClassGroup
import com.dreamteam.bookworm.domain.model.ClassStudent
import com.dreamteam.bookworm.domain.model.CurriculumLesson
import com.dreamteam.bookworm.domain.model.DraftCard
import com.dreamteam.bookworm.domain.model.DraftStatus
import com.dreamteam.bookworm.domain.model.Flashcard
import com.dreamteam.bookworm.domain.model.Fraction
import com.dreamteam.bookworm.domain.model.Option
import com.dreamteam.bookworm.domain.model.Question
import com.dreamteam.bookworm.domain.model.StudentStatus
import com.dreamteam.bookworm.domain.model.UnlockMode

/** Sample teacher-side content from the design canvas. Content comes from teachers, so it is not localised. */
internal object FakeTeacherData {

    fun joinLink(code: String) = "https://bookworm.app/join/$code"

    val readingGrades = listOf(2, 3, 4)
    const val defaultGrade = 3

    fun card(id: String, front: String, back: String, page: Int, source: String, issue: String? = null) =
        DraftCard(Flashcard(id, front, back), page, source, DraftStatus.Pending, edited = false, published = false, issue = issue)

    val worksheetCards = listOf(
        card("ws-1", "Numerator", "The top number of a fraction. It tells how many parts we have.", 1,
            "The numerator is the top number. It shows how many parts we have."),
        card("ws-2", "Denominator", "The bottom number of a fraction. It tells how many equal parts make the whole.", 1,
            "The denominator is the bottom number. It shows how many equal parts make the whole."),
        card("ws-3", "Which fraction is one half written as a number?", "1/2", 2,
            "One half is written 1/2."),
        card("ws-4", "What are equal parts?", "Parts that are exactly the same size.", 2,
            "Fractions need equal parts: all parts are exactly the same size."),
        card("ws-5", "Which is bigger, 1/2 or 1/4?", "1/2. Fewer parts means bigger pieces.", 3,
            "The fewer parts a whole is cut into, the bigger each part is."),
        card("ws-6", "How many quarters make one whole?", "Three quarters make one whole.", 3,
            "Four quarters make one whole.",
            issue = "The back says three quarters, but your worksheet says four quarters."),
        card("ws-7", "One quarter", "One of four equal parts of a whole. We write it 1/4.", 3,
            "When we cut a whole into four equal parts, each part is one quarter, 1/4."),
        card("ws-8", "How many halves make one whole?", "Two halves make one whole.", 2,
            "Two halves make one whole."),
        card("ws-9", "What does 3/4 of a pizza mean?", "The pizza is cut into 4 equal slices and we have 3 of them.", 4,
            "3/4 means 3 of 4 equal parts."),
        card("ws-10", "Fraction", "A number that shows part of a whole.", 1,
            "A fraction shows a part of a whole thing."),
        card("ws-11", "Which is smaller, 1/3 or 1/2?", "1/2 is smaller.", 4,
            "1/3 is smaller than 1/2, because the whole is cut into more parts.",
            issue = "The back says 1/2, but your worksheet says 1/3 is smaller."),
        card("ws-12", "What is half of 8 apples?", "4 apples.", 5,
            "Half of 8 apples is 4 apples: share them into 2 equal groups."),
    )

    val slidesCards = listOf(
        card("sl-1", "Whole", "All the parts together, the complete thing.", 2,
            "A whole is the complete shape or object before we cut it."),
        card("sl-2", "What do we call 1 of 3 equal parts?", "One third, written 1/3.", 4,
            "One of three equal parts is called one third, 1/3."),
        card("sl-3", "Can parts of different sizes make a fraction?", "No. The parts must be equal.", 5,
            "Only equal parts make a fraction."),
        card("sl-4", "Number line", "A line with numbers in order. Fractions sit between 0 and 1.", 7,
            "On a number line, halves and quarters sit between 0 and 1."),
        card("sl-5", "Where is 1/2 on a number line from 0 to 1?", "Exactly in the middle.", 7,
            "1/2 is exactly halfway between 0 and 1."),
    )

    /** Cards made for any other upload, so new files go through the whole flow. */
    fun generatedCards(prefix: String, chapterTitle: String, fileName: String) = listOf(
        card("$prefix-1", chapterTitle, "What this chapter is about, in one short sentence.", 1,
            "The first page of $fileName introduces $chapterTitle."),
        card("$prefix-2", "A key word from $fileName", "Its meaning, written for young readers.", 1,
            "The key word is explained in the first section."),
        card("$prefix-3", "Why does $chapterTitle matter?", "Because we use it every day.", 2,
            "The material gives an everyday example."),
        card("$prefix-4", "Give an example from $chapterTitle", "An example taken from the material.", 2,
            "Example from page 2.", issue = "The material has two examples here. Check that the card uses the right one."),
    )

    class SeedClass(
        val id: String,
        val name: String,
        val grade: Int,
        val joinCode: String,
        val students: List<ClassStudent>,
        val groups: List<ClassGroup> = emptyList(),
    )

    const val studentClassId = "class-3b"

    val classes = listOf(
        SeedClass(
            id = studentClassId,
            name = "Class 3B",
            grade = 3,
            joinCode = "K7M2Q",
            students = listOf(
                // Mia is the student signed in to the student app.
                ClassStudent(FakeData.studentId, "Mia", StudentStatus.InClass),
                ClassStudent("s1", "Ana K.", StudentStatus.InClass),
                ClassStudent("s2", "Luka M.", StudentStatus.InClass),
                ClassStudent("s3", "Maja P.", StudentStatus.InClass),
                ClassStudent("s4", "Nik T.", StudentStatus.InClass),
                ClassStudent("s5", "Eva S.", StudentStatus.Waiting),
                ClassStudent("s6", "Jan R.", StudentStatus.Offline),
                ClassStudent("s7", "Zala B.", StudentStatus.InClass),
                ClassStudent("s8", "Tim H.", StudentStatus.InClass),
                ClassStudent("s9", "Lara V.", StudentStatus.Waiting),
                ClassStudent("s10", "Matej D.", StudentStatus.Offline),
                ClassStudent("s11", "Pia G.", StudentStatus.InClass),
                ClassStudent("s12", "Rok F.", StudentStatus.InClass),
            ),
            groups = listOf(ClassGroup("g-reading", "Reading stars", listOf(FakeData.studentId, "s1", "s3", "s7"))),
        ),
        SeedClass(
            id = "class-3a",
            name = "Class 3A",
            grade = 3,
            joinCode = "P4X9T",
            students = listOf(
                ClassStudent("a1", "Gal Z.", StudentStatus.InClass),
                ClassStudent("a2", "Neža K.", StudentStatus.InClass),
                ClassStudent("a3", "Filip O.", StudentStatus.Offline),
                ClassStudent("a4", "Ula M.", StudentStatus.InClass),
                ClassStudent("a5", "Jaka P.", StudentStatus.InClass),
                ClassStudent("a6", "Ema L.", StudentStatus.Waiting),
            ),
        ),
        SeedClass(
            id = "class-4b",
            name = "Class 4B",
            grade = 4,
            joinCode = "R2D7W",
            students = listOf(
                ClassStudent("b1", "Vid S.", StudentStatus.Offline),
                ClassStudent("b2", "Lana J.", StudentStatus.Offline),
                ClassStudent("b3", "Žan B.", StudentStatus.Offline),
            ),
        ),
    )

    /** The lessons a chapter starts with when it is added to a path. */
    fun defaultLessons(chapterId: String): List<CurriculumLesson> =
        FakeData.lessonsByChapter[chapterId]?.map { (id, title) -> CurriculumLesson(id, title, isChapterQuiz = id.endsWith("quiz")) }
            ?: listOf(
                CurriculumLesson("$chapterId-words", "Key words"),
                CurriculumLesson("$chapterId-practise", "Practise"),
                CurriculumLesson("$chapterId-quiz", "Chapter quiz", isChapterQuiz = true),
            )

    class SeedCurriculum(
        val id: String,
        val name: String,
        val subjectId: String,
        val grade: Int,
        val unlockMode: UnlockMode,
        val chapterIds: List<String>,
        val assignedTo: List<AssignmentTarget>,
    )

    /** Grade 3 paths are shared by 3A and 3B; the reading group follows its own language path. 4B has none yet. */
    val curricula = listOf(
        SeedCurriculum(
            "cur-maths-3", "Maths · Grade 3", "maths", 3, UnlockMode.InOrder,
            listOf("maths-1", "maths-2", "maths-3", "maths-4", "maths-5"),
            listOf(AssignmentTarget.WholeClass("class-3b"), AssignmentTarget.WholeClass("class-3a")),
        ),
        SeedCurriculum(
            "cur-nature-3", "Nature · Grade 3", "nature", 3, UnlockMode.InOrder,
            listOf("nature-1", "nature-2", "nature-3"),
            listOf(AssignmentTarget.WholeClass("class-3b"), AssignmentTarget.WholeClass("class-3a")),
        ),
        SeedCurriculum(
            "cur-language-3", "Language · Grade 3", "language", 3, UnlockMode.InOrder,
            listOf("language-1", "language-2", "language-3"),
            listOf(AssignmentTarget.WholeClass("class-3b"), AssignmentTarget.WholeClass("class-3a")),
        ),
        SeedCurriculum(
            "cur-language-reading", "Language · Reading stars", "language", 3, UnlockMode.AnyChapter,
            listOf("language-2", "language-3"),
            listOf(AssignmentTarget.Group("class-3b", "g-reading")),
        ),
    )

    /** A pop quiz question with its answer key and how the fake class does on it. */
    class QuizQuestion(
        val question: Question.SingleChoice,
        val correctOptionId: String,
        /** Share of the class that picks the right answer. */
        val correctShare: Float,
        val explanation: String,
        val fraction: Fraction? = null,
    )

    /** Options are lettered a, b, c, d; fraction-looking answers are drawn as fractions. */
    private fun quiz(
        id: String,
        prompt: String,
        options: List<String>,
        correctIndex: Int,
        correctShare: Float,
        explanation: String,
        fraction: Fraction? = null,
    ): QuizQuestion {
        val opts = options.mapIndexed { i, text ->
            val parts = text.split('/').mapNotNull { it.toIntOrNull() }
            Option(('a' + i).toString(), text, if (parts.size == 2) Fraction(parts[0], parts[1]) else null)
        }
        return QuizQuestion(Question.SingleChoice(id, prompt, opts), opts[correctIndex].id, correctShare, explanation, fraction)
    }

    val quizQuestions = listOf(
        quiz("pq-1", "Which fraction shows one half?", listOf("1/2", "1/3", "2/1", "1/4"), 0, 0.9f,
            "One half means one of two equal parts, so we write 1/2.", Fraction(1, 2)),
        quiz("pq-2", "How many quarters make one whole?", listOf("2", "3", "4", "5"), 2, 0.75f,
            "A whole cut into four equal parts has four quarters.", Fraction(4, 4)),
        quiz("pq-3", "Which number is the denominator in 3/4?", listOf("3", "4", "1", "7"), 1, 0.625f,
            "The denominator is the bottom number. It tells how many equal parts make the whole. Here the whole is cut into 4 parts.",
            Fraction(3, 4)),
        quiz("pq-4", "Which is bigger?", listOf("1/2", "1/4", "1/8", "They are the same"), 0, 0.6f,
            "Fewer parts means bigger pieces, so 1/2 is the biggest.", Fraction(1, 2)),
        quiz("pq-5", "What is half of 8 apples?", listOf("2", "4", "6", "8"), 1, 0.85f,
            "Share 8 apples into 2 equal groups and each group has 4."),
        quiz("pq-6", "Which shape is cut into equal parts?", listOf("A pizza in 4 same slices", "A cake in big and small pieces", "A broken plate", "A torn page"), 0, 0.9f,
            "Fractions need equal parts. Only the pizza is cut into parts of the same size."),
        quiz("pq-7", "Which number is the numerator in 2/3?", listOf("3", "2", "5", "1"), 1, 0.7f,
            "The numerator is the top number. It tells how many parts we have: 2.", Fraction(2, 3)),
        quiz("pq-8", "Where is 1/2 on a number line from 0 to 1?", listOf("At 0", "Near 1", "In the middle", "After 1"), 2, 0.65f,
            "1/2 is exactly halfway between 0 and 1."),
        quiz("pq-9", "How many thirds make one whole?", listOf("1", "2", "3", "4"), 2, 0.7f,
            "A whole cut into three equal parts has three thirds.", Fraction(3, 3)),
        quiz("pq-10", "Which fraction is smaller, 1/3 or 1/2?", listOf("1/3", "1/2", "They are the same", "Can't tell"), 0, 0.55f,
            "When the whole is cut into more parts, each part is smaller, so 1/3 is smaller.", Fraction(1, 3)),
    )

    // Lesson content

    /** The canvas sample lesson as authored content, items and right-hand matches stored in their correct order. */
    fun sampleLessonContent(): List<AuthoredQuestion> = FakeData.sampleLesson.questions.map { question ->
        val key = FakeData.answerKey.getValue(question.id)
        val stored = when (question) {
            is Question.Order -> {
                val order = (key.answer as Answer.Ordered).optionIds
                question.copy(items = order.map { id -> question.items.first { it.id == id } })
            }
            is Question.Match -> {
                val pairs = (key.answer as Answer.Pairs).leftToRight
                question.copy(right = question.left.map { left -> question.right.first { it.id == pairs.getValue(left.id) } })
            }
            else -> question
        }
        AuthoredQuestion(stored, key.answer, key.explanation, key.retryHint)
    }

    /** Lessons that already have written questions, besides the sample one. */
    val authoredLessons: Map<String, List<AuthoredQuestion>> = mapOf(
        "language-2-endings" to listOf(
            AuthoredQuestion(
                Question.SingleChoice("le-1", "Which word ends in -ing?", listOf(Option("a", "jumped"), Option("b", "jumping"), Option("c", "jumps"))),
                Answer.Choice("b"), "Jumping ends in -ing.", "Look at the last three letters of each word.",
            ),
            AuthoredQuestion(
                Question.FillBlank("le-2", "Tap the word that completes the sentence.", "Yesterday we", "in the park.", listOf("play", "played", "playing")),
                Answer.Text("played"), "Yesterday is in the past, so we write played.", "It happened yesterday. Which ending shows the past?",
            ),
            AuthoredQuestion(
                Question.TypedAnswer("le-3", "Add -s to make more than one: one cat, two …"),
                Answer.Text("cats"), "One cat, two cats.", "Write cat and add one letter at the end.",
            ),
        ),
        "nature-2-burrows" to listOf(
            AuthoredQuestion(
                Question.MultiChoice("nb-1", "Which animals live in burrows?", listOf(Option("a", "Rabbit"), Option("b", "Eagle"), Option("c", "Mole"), Option("d", "Fish"))),
                Answer.Choices(setOf("a", "c")), "Rabbits and moles dig burrows under the ground.", "Think of animals that dig in the ground.",
            ),
            AuthoredQuestion(
                Question.Match(
                    "nb-2", "Match each animal to its home.",
                    left = listOf(Option("A", "Bird"), Option("B", "Fox"), Option("C", "Bee")),
                    right = listOf(Option("R1", "Nest"), Option("R2", "Den"), Option("R3", "Hive")),
                ),
                Answer.Pairs(mapOf("A" to "R1", "B" to "R2", "C" to "R3")), "Birds live in nests, foxes in dens and bees in hives.",
                "Start with the one you are sure about.",
            ),
        ),
    )

    /** Practice questions made from flashcards: "What does X mean?" with up to four meanings to choose from. */
    fun questionsFromFlashcards(cards: List<Flashcard>, max: Int = 6): List<AuthoredQuestion> {
        if (cards.size < 2) return emptyList()
        return cards.take(max).mapIndexed { index, card ->
            val others = (cards - card).let { rest -> List(minOf(3, rest.size)) { rest[(index + it) % rest.size] } }
            val meanings = (others.map { it.definition } + card.definition).distinct()
            // Put the right meaning in a different place each time.
            val rotated = meanings.indices.map { meanings[(it + index) % meanings.size] }
            val options = rotated.mapIndexed { i, text -> Option(('a' + i).toString(), text) }
            AuthoredQuestion(
                question = Question.SingleChoice("fc-${card.id}", "What does \u201C${card.term}\u201D mean?", options),
                answer = Answer.Choice(options.first { it.text == card.definition }.id),
                explanation = "${card.term}: ${card.definition}",
                retryHint = "Think back to the flashcard for \u201C${card.term}\u201D.",
            )
        }
    }
}
