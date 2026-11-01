package com.dreamteam.bookworm.domain.model

/** A class the student belongs to, or has asked to join. */
data class StudentClass(
    val id: String,
    val name: String,
    /** Null for a class the teacher hasn't given a grade. */
    val grade: Int?,
    val teacherName: String,
    val status: MembershipStatus,
)

enum class MembershipStatus {
    Joined,
    /** The teacher approves new students and hasn't let this one in yet. */
    WaitingForTeacher,
}

enum class JoinFailure { CodeNotFound, AlreadyInClass }

class JoinClassException(val failure: JoinFailure) : Exception(failure.name)

/**
 * Turns what the student typed or pasted into a bare class code: accepts the
 * join link teachers share (…/join/K7M2Q), ignores spaces and case.
 */
fun normalizeClassCode(input: String): String =
    input.trim().substringAfterLast("/join/").substringBefore('?').filterNot { it.isWhitespace() }.uppercase()
