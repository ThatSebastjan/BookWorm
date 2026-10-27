package com.dreamteam.bookworm.data.remote

import com.dreamteam.bookworm.domain.model.Answer
import com.dreamteam.bookworm.domain.model.AuthoredQuestion
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.Flashcard
import com.dreamteam.bookworm.domain.model.Fraction
import com.dreamteam.bookworm.domain.model.NoteSection
import com.dreamteam.bookworm.domain.model.Option
import com.dreamteam.bookworm.domain.model.Question
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/*
 * JSON shapes the backend sends and accepts, shared by the student and teacher
 * repositories, and their mapping to the domain models.
 */

internal suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T = successOrThrow().body()

internal fun HttpResponse.successOrThrow(): HttpResponse =
    if (status.isSuccess()) this else throw ApiException(status)

@Serializable
internal data class ChapterSummaryJson(
    val id: String,
    val classId: String,
    val number: Int,
    val title: String,
    val noteCount: Int,
    val flashcardCount: Int,
    val knownFlashcards: Int,
) {
    /** The class is the chapter's "subject" in the app. */
    fun toModel() = ChapterSummary(id, classId, number, title, noteCount, flashcardCount, knownFlashcards)
}

@Serializable
internal data class NoteJson(val heading: String, val body: String, val remember: String? = null) {
    fun toModel() = NoteSection(heading, body, remember)
}

internal fun NoteSection.toJson() = NoteJson(heading, body, remember?.takeIf { it.isNotBlank() })

/** [id] is left out for a card the teacher just added. [known] only comes from the server, for students. */
@Serializable
internal data class FlashcardJson(val id: String? = null, val term: String, val definition: String, val known: Boolean = false) {
    fun toModel() = Flashcard(id.orEmpty(), term, definition)
}

internal fun Flashcard.toJson() = FlashcardJson(id.ifEmpty { null }, term, definition)

@Serializable
internal data class ChapterJson(
    val id: String,
    val classId: String,
    val className: String,
    val teacherName: String,
    val number: Int,
    val title: String,
    val notes: List<NoteJson>,
    val flashcards: List<FlashcardJson>,
)

// Lesson questions. The backend stores them as they are, so this shape is the app's own.

@Serializable
internal data class OptionJson(val id: String, val text: String, val numerator: Int? = null, val denominator: Int? = null) {
    fun toModel() = Option(id, text, if (numerator != null && denominator != null) Fraction(numerator, denominator) else null)
}

private fun Option.toJson() = OptionJson(id, text, fraction?.numerator, fraction?.denominator)

/** One of the fields is set, depending on the question type. */
@Serializable
internal data class AnswerJson(
    val choice: String? = null,
    val choices: List<String>? = null,
    val text: String? = null,
    val ordered: List<String>? = null,
    val pairs: Map<String, String>? = null,
) {
    fun toModel(): Answer = when {
        choice != null -> Answer.Choice(choice)
        choices != null -> Answer.Choices(choices.toSet())
        ordered != null -> Answer.Ordered(ordered)
        pairs != null -> Answer.Pairs(pairs)
        else -> Answer.Text(text.orEmpty())
    }
}

internal fun Answer.toJson() = when (this) {
    is Answer.Choice -> AnswerJson(choice = optionId)
    is Answer.Choices -> AnswerJson(choices = optionIds.toList())
    is Answer.Text -> AnswerJson(text = value)
    is Answer.Ordered -> AnswerJson(ordered = optionIds)
    is Answer.Pairs -> AnswerJson(pairs = leftToRight)
}

/**
 * A question with its answer key. [type] picks which fields matter: [options] holds the
 * choices, the items to order, or the left column of a match ([right] the other column).
 */
@Serializable
internal data class AuthoredQuestionJson(
    val type: String,
    val id: String,
    val prompt: String,
    val hint: String? = null,
    val options: List<OptionJson> = emptyList(),
    val right: List<OptionJson> = emptyList(),
    val fractionInput: Boolean = false,
    val textBefore: String = "",
    val textAfter: String = "",
    val words: List<String> = emptyList(),
    val answer: AnswerJson,
    val explanation: String = "",
    val retryHint: String = "",
) {
    fun toModel() = AuthoredQuestion(
        question = questionOf(type, id, prompt, hint, options, right, fractionInput, textBefore, textAfter, words),
        answer = answer.toModel(),
        explanation = explanation,
        retryHint = retryHint,
    )
}

/** A question as students get it: the same shape as [AuthoredQuestionJson], without the answer key. */
@Serializable
internal data class QuestionJson(
    val type: String,
    val id: String,
    val prompt: String,
    val hint: String? = null,
    val options: List<OptionJson> = emptyList(),
    val right: List<OptionJson> = emptyList(),
    val fractionInput: Boolean = false,
    val textBefore: String = "",
    val textAfter: String = "",
    val words: List<String> = emptyList(),
) {
    fun toModel() = questionOf(type, id, prompt, hint, options, right, fractionInput, textBefore, textAfter, words)
}

private fun questionOf(
    type: String,
    id: String,
    prompt: String,
    hint: String?,
    options: List<OptionJson>,
    right: List<OptionJson>,
    fractionInput: Boolean,
    textBefore: String,
    textAfter: String,
    words: List<String>,
): Question {
    val opts = options.map { it.toModel() }
    return when (type) {
        "multiChoice" -> Question.MultiChoice(id, prompt, opts, hint)
        "typed" -> Question.TypedAnswer(id, prompt, fractionInput, hint)
        "fillBlank" -> Question.FillBlank(id, prompt, textBefore, textAfter, words, hint)
        "order" -> Question.Order(id, prompt, opts, hint)
        "match" -> Question.Match(id, prompt, opts, right.map { it.toModel() }, hint)
        else -> Question.SingleChoice(id, prompt, opts, hint)
    }
}

internal fun AuthoredQuestion.toJson(): AuthoredQuestionJson {
    val q = question
    val base = AuthoredQuestionJson(
        type = "singleChoice", id = q.id, prompt = q.prompt, hint = q.hint,
        answer = answer.toJson(), explanation = explanation, retryHint = retryHint,
    )
    return when (q) {
        is Question.SingleChoice -> base.copy(type = "singleChoice", options = q.options.map { it.toJson() })
        is Question.MultiChoice -> base.copy(type = "multiChoice", options = q.options.map { it.toJson() })
        is Question.TypedAnswer -> base.copy(type = "typed", fractionInput = q.fractionInput)
        is Question.FillBlank -> base.copy(type = "fillBlank", textBefore = q.textBefore, textAfter = q.textAfter, words = q.words)
        is Question.Order -> base.copy(type = "order", options = q.items.map { it.toJson() })
        is Question.Match -> base.copy(type = "match", options = q.left.map { it.toJson() }, right = q.right.map { it.toJson() })
    }
}
