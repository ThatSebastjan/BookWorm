package com.dreamteam.bookworm.teacher.screens.lessoneditor

import com.dreamteam.bookworm.domain.model.Answer
import com.dreamteam.bookworm.domain.model.AuthoredQuestion
import com.dreamteam.bookworm.domain.model.Fraction
import com.dreamteam.bookworm.domain.model.Option
import com.dreamteam.bookworm.domain.model.Question

/*
 * Editing a question and its answer key together, so they never drift apart.
 * Conventions the server relies on (see docs/backend-integration.md):
 * Order items are stored in their correct order, and Match pairs are stored
 * row by row (left[i] goes with right[i]); the server shuffles both for students.
 */

/** The six question types the student app can show. */
enum class QuestionKind { SingleChoice, MultiChoice, TypedAnswer, FillBlank, Order, Match }

val AuthoredQuestion.kind: QuestionKind
    get() = when (question) {
        is Question.SingleChoice -> QuestionKind.SingleChoice
        is Question.MultiChoice -> QuestionKind.MultiChoice
        is Question.TypedAnswer -> QuestionKind.TypedAnswer
        is Question.FillBlank -> QuestionKind.FillBlank
        is Question.Order -> QuestionKind.Order
        is Question.Match -> QuestionKind.Match
    }

/** A blank question of [kind] with room for a first answer or two. */
fun newQuestion(kind: QuestionKind, id: String): AuthoredQuestion {
    val two = listOf(Option("o1", ""), Option("o2", ""))
    val question = when (kind) {
        QuestionKind.SingleChoice -> Question.SingleChoice(id, "", two)
        QuestionKind.MultiChoice -> Question.MultiChoice(id, "", two)
        QuestionKind.TypedAnswer -> Question.TypedAnswer(id, "")
        QuestionKind.FillBlank -> Question.FillBlank(id, "", textBefore = "", textAfter = "", words = listOf("", ""))
        QuestionKind.Order -> Question.Order(id, "", two)
        QuestionKind.Match -> Question.Match(id, "", left = listOf(Option("l1", ""), Option("l2", "")), right = listOf(Option("r1", ""), Option("r2", "")))
    }
    val answer = when (kind) {
        QuestionKind.SingleChoice -> Answer.Choice("")
        QuestionKind.MultiChoice -> Answer.Choices(emptySet())
        QuestionKind.TypedAnswer, QuestionKind.FillBlank -> Answer.Text("")
        QuestionKind.Order -> Answer.Ordered(two.map { it.id })
        QuestionKind.Match -> Answer.Pairs(mapOf("l1" to "r1", "l2" to "r2"))
    }
    return AuthoredQuestion(question, answer, explanation = "", retryHint = "")
}

/** "3/4" becomes a fraction the student app draws the school way; anything else stays text. */
fun optionOf(id: String, text: String): Option {
    val parts = text.trim().split('/')
    val numbers = parts.mapNotNull { it.trim().toIntOrNull() }
    val fraction = if (parts.size == 2 && numbers.size == 2 && numbers[1] != 0) Fraction(numbers[0], numbers[1]) else null
    return Option(id, text, fraction)
}

private fun nextId(prefix: String, taken: Collection<String>): String =
    generateSequence(1) { it + 1 }.map { "$prefix$it" }.first { it !in taken }

// Common fields

fun AuthoredQuestion.withPrompt(text: String) = copy(
    question = when (val q = question) {
        is Question.SingleChoice -> q.copy(prompt = text)
        is Question.MultiChoice -> q.copy(prompt = text)
        is Question.TypedAnswer -> q.copy(prompt = text)
        is Question.FillBlank -> q.copy(prompt = text)
        is Question.Order -> q.copy(prompt = text)
        is Question.Match -> q.copy(prompt = text)
    },
)

/** The optional hint shown under the question; blank removes it. */
fun AuthoredQuestion.withHint(text: String): AuthoredQuestion {
    val hint = text.ifEmpty { null }
    return copy(
        question = when (val q = question) {
            is Question.SingleChoice -> q.copy(hint = hint)
            is Question.MultiChoice -> q.copy(hint = hint)
            is Question.TypedAnswer -> q.copy(hint = hint)
            is Question.FillBlank -> q.copy(hint = hint)
            is Question.Order -> q.copy(hint = hint)
            is Question.Match -> q.copy(hint = hint)
        },
    )
}

/** Copies the question under a new id, e.g. when suggestions are added next to existing questions. */
fun AuthoredQuestion.withId(id: String) = copy(
    question = when (val q = question) {
        is Question.SingleChoice -> q.copy(id = id)
        is Question.MultiChoice -> q.copy(id = id)
        is Question.TypedAnswer -> q.copy(id = id)
        is Question.FillBlank -> q.copy(id = id)
        is Question.Order -> q.copy(id = id)
        is Question.Match -> q.copy(id = id)
    },
)

// One or several right answers

val AuthoredQuestion.choiceOptions: List<Option>
    get() = when (val q = question) {
        is Question.SingleChoice -> q.options
        is Question.MultiChoice -> q.options
        else -> emptyList()
    }

fun AuthoredQuestion.isCorrectOption(optionId: String): Boolean = when (val a = answer) {
    is Answer.Choice -> a.optionId == optionId
    is Answer.Choices -> optionId in a.optionIds
    else -> false
}

private fun AuthoredQuestion.withOptions(options: List<Option>): AuthoredQuestion {
    val ids = options.map { it.id }.toSet()
    return copy(
        question = when (val q = question) {
            is Question.SingleChoice -> q.copy(options = options)
            is Question.MultiChoice -> q.copy(options = options)
            else -> q
        },
        // Drop a right answer whose option was removed.
        answer = when (val a = answer) {
            is Answer.Choice -> if (a.optionId in ids) a else Answer.Choice("")
            is Answer.Choices -> Answer.Choices(a.optionIds intersect ids)
            else -> a
        },
    )
}

fun AuthoredQuestion.withOptionText(index: Int, text: String) =
    withOptions(choiceOptions.mapIndexed { i, o -> if (i == index) optionOf(o.id, text) else o })

fun AuthoredQuestion.withNewOption() =
    withOptions(choiceOptions + Option(nextId("o", choiceOptions.map { it.id }), ""))

fun AuthoredQuestion.withoutOption(index: Int) = withOptions(choiceOptions.filterIndexed { i, _ -> i != index })

/** One right answer: picks it. Several: toggles it. */
fun AuthoredQuestion.withCorrectToggled(optionId: String) = copy(
    answer = when (val a = answer) {
        is Answer.Choice -> Answer.Choice(optionId)
        is Answer.Choices -> Answer.Choices(if (optionId in a.optionIds) a.optionIds - optionId else a.optionIds + optionId)
        else -> a
    },
)

// Typed answer

fun AuthoredQuestion.withFractionInput(on: Boolean) = copy(
    question = (question as? Question.TypedAnswer)?.copy(fractionInput = on) ?: question,
)

val AuthoredQuestion.textAnswer: String get() = (answer as? Answer.Text)?.value.orEmpty()

fun AuthoredQuestion.withTextAnswer(text: String) = copy(answer = Answer.Text(text))

// Fill in the blank

fun AuthoredQuestion.withBlankText(before: String? = null, after: String? = null) = copy(
    question = (question as? Question.FillBlank)?.let { q -> q.copy(textBefore = before ?: q.textBefore, textAfter = after ?: q.textAfter) } ?: question,
)

private val AuthoredQuestion.words: List<String> get() = (question as? Question.FillBlank)?.words.orEmpty()

fun AuthoredQuestion.withWord(index: Int, text: String): AuthoredQuestion {
    val wasCorrect = words.getOrNull(index) == textAnswer && textAnswer.isNotEmpty()
    val q = question as? Question.FillBlank ?: return this
    return copy(
        question = q.copy(words = q.words.mapIndexed { i, w -> if (i == index) text else w }),
        answer = if (wasCorrect) Answer.Text(text) else answer,
    )
}

fun AuthoredQuestion.withNewWord() = copy(question = (question as? Question.FillBlank)?.let { it.copy(words = it.words + "") } ?: question)

fun AuthoredQuestion.withoutWord(index: Int): AuthoredQuestion {
    val q = question as? Question.FillBlank ?: return this
    val removed = q.words.getOrNull(index)
    return copy(
        question = q.copy(words = q.words.filterIndexed { i, _ -> i != index }),
        answer = if (removed == textAnswer) Answer.Text("") else answer,
    )
}

fun AuthoredQuestion.withCorrectWord(index: Int) = copy(answer = Answer.Text(words.getOrNull(index).orEmpty()))

// Put in order: items are kept in their correct order

val AuthoredQuestion.orderItems: List<Option> get() = (question as? Question.Order)?.items.orEmpty()

private fun AuthoredQuestion.withItems(items: List<Option>) = copy(
    question = (question as? Question.Order)?.copy(items = items) ?: question,
    answer = Answer.Ordered(items.map { it.id }),
)

fun AuthoredQuestion.withItemText(index: Int, text: String) =
    withItems(orderItems.mapIndexed { i, o -> if (i == index) optionOf(o.id, text) else o })

fun AuthoredQuestion.withNewItem() = withItems(orderItems + Option(nextId("o", orderItems.map { it.id }), ""))

fun AuthoredQuestion.withoutItem(index: Int) = withItems(orderItems.filterIndexed { i, _ -> i != index })

fun AuthoredQuestion.withItemMoved(index: Int, by: Int): AuthoredQuestion {
    val target = index + by
    if (target !in orderItems.indices) return this
    return withItems(orderItems.toMutableList().apply { add(target, removeAt(index)) })
}

/** Puts stored items in their correct order, for content saved without that convention. */
fun AuthoredQuestion.normalised(): AuthoredQuestion = when (val q = question) {
    is Question.Order -> {
        val order = (answer as? Answer.Ordered)?.optionIds.orEmpty()
        if (order.toSet() == q.items.map { it.id }.toSet()) withItems(order.map { id -> q.items.first { it.id == id } }) else withItems(q.items)
    }
    is Question.Match -> {
        val pairs = (answer as? Answer.Pairs)?.leftToRight.orEmpty()
        val right = q.left.mapNotNull { left -> q.right.firstOrNull { it.id == pairs[left.id] } }
        if (right.size == q.left.size && right.size == q.right.size) withPairs(q.left, right) else withPairs(q.left, q.right)
    }
    else -> this
}

// Match the pairs: row i pairs left[i] with right[i]

val AuthoredQuestion.matchPairs: List<Pair<Option, Option>>
    get() = (question as? Question.Match)?.let { it.left.zip(it.right) }.orEmpty()

private fun AuthoredQuestion.withPairs(left: List<Option>, right: List<Option>) = copy(
    question = (question as? Question.Match)?.copy(left = left, right = right) ?: question,
    answer = Answer.Pairs(left.zip(right).associate { (l, r) -> l.id to r.id }),
)

fun AuthoredQuestion.withPairText(index: Int, left: String? = null, right: String? = null): AuthoredQuestion {
    val pairs = matchPairs
    return withPairs(
        pairs.mapIndexed { i, (l, _) -> if (i == index && left != null) optionOf(l.id, left) else l },
        pairs.mapIndexed { i, (_, r) -> if (i == index && right != null) optionOf(r.id, right) else r },
    )
}

fun AuthoredQuestion.withNewPair(): AuthoredQuestion {
    val pairs = matchPairs
    val left = pairs.map { it.first }
    val right = pairs.map { it.second }
    return withPairs(left + Option(nextId("l", left.map { it.id }), ""), right + Option(nextId("r", right.map { it.id }), ""))
}

fun AuthoredQuestion.withoutPair(index: Int): AuthoredQuestion {
    val pairs = matchPairs.filterIndexed { i, _ -> i != index }
    return withPairs(pairs.map { it.first }, pairs.map { it.second })
}

// Checking

/** Whether a student could answer it: a prompt, enough filled-in choices, and a right answer. */
fun AuthoredQuestion.isComplete(): Boolean {
    if (question.prompt.isBlank()) return false
    fun List<Option>.filled() = size >= 2 && all { it.text.isNotBlank() }
    return when (val q = question) {
        is Question.SingleChoice -> q.options.filled() && q.options.any { isCorrectOption(it.id) }
        is Question.MultiChoice -> q.options.filled() && q.options.any { isCorrectOption(it.id) }
        is Question.TypedAnswer -> textAnswer.isNotBlank() &&
            (!q.fractionInput || optionOf("", textAnswer).fraction != null)
        is Question.FillBlank -> q.words.size >= 2 && q.words.all { it.isNotBlank() } && textAnswer in q.words &&
            (q.textBefore.isNotBlank() || q.textAfter.isNotBlank())
        is Question.Order -> q.items.filled()
        is Question.Match -> q.left.filled() && q.right.filled()
    }
}
