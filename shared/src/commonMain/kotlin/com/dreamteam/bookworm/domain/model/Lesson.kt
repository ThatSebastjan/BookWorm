package com.dreamteam.bookworm.domain.model

data class Lesson(
    val id: String,
    val title: String,
    val questions: List<Question>,
    /** Stars for finishing it; 0 when [alreadyDone]. */
    val stars: Int = 0,
    /** Finished before, so it earns no stars again. */
    val alreadyDone: Boolean = false,
)

/** A fraction shown as stacked numerator over denominator. */
data class Fraction(val numerator: Int, val denominator: Int) {
    override fun toString() = "$numerator/$denominator"
}

/** An answer option. Shows [fraction] when set, otherwise [text]. */
data class Option(
    val id: String,
    val text: String,
    val fraction: Fraction? = null,
)

/**
 * Questions as the client receives them. Correct answers stay on the server:
 * the client sends an [Answer] and gets an [AnswerResult] back.
 */
sealed interface Question {
    val id: String
    val prompt: String
    val hint: String?

    data class SingleChoice(
        override val id: String,
        override val prompt: String,
        val options: List<Option>,
        override val hint: String? = null,
    ) : Question

    data class MultiChoice(
        override val id: String,
        override val prompt: String,
        val options: List<Option>,
        override val hint: String? = null,
    ) : Question

    /** Free text answer, or numerator and denominator boxes when [fractionInput] is true. */
    data class TypedAnswer(
        override val id: String,
        override val prompt: String,
        val fractionInput: Boolean = false,
        override val hint: String? = null,
    ) : Question

    data class FillBlank(
        override val id: String,
        override val prompt: String,
        val textBefore: String,
        val textAfter: String,
        val words: List<String>,
        override val hint: String? = null,
    ) : Question

    data class Order(
        override val id: String,
        override val prompt: String,
        val items: List<Option>,
        override val hint: String? = null,
    ) : Question

    data class Match(
        override val id: String,
        override val prompt: String,
        val left: List<Option>,
        val right: List<Option>,
        override val hint: String? = null,
    ) : Question
}

sealed interface Answer {
    data class Choice(val optionId: String) : Answer
    data class Choices(val optionIds: Set<String>) : Answer
    data class Text(val value: String) : Answer
    data class Ordered(val optionIds: List<String>) : Answer
    data class Pairs(val leftToRight: Map<String, String>) : Answer
}

data class AnswerResult(
    val correct: Boolean,
    val explanation: String,
)

/** [starsEarned] are the lesson's stars the first time it is finished, and 0 after that. */
data class LessonSummary(
    val starsEarned: Int,
    val correctAnswers: Int,
    val totalQuestions: Int,
)
