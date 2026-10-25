package com.dreamteam.bookworm

import com.dreamteam.bookworm.data.fake.FakeBackend
import com.dreamteam.bookworm.domain.model.AuthSession
import com.dreamteam.bookworm.domain.model.JoinClassException
import com.dreamteam.bookworm.domain.model.JoinFailure
import com.dreamteam.bookworm.domain.model.MembershipStatus
import com.dreamteam.bookworm.domain.model.StudentStatus
import com.dreamteam.bookworm.domain.model.normalizeClassCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JoinClassTest {

    private val newStudent = AuthSession("student-new", "Nina", "nina@example.com", "token")
    private val backend = FakeBackend(latencyMs = 0, currentStudent = { newStudent })

    @Test
    fun normalisesTypedCodesAndJoinLinks() {
        assertEquals("K7M2Q", normalizeClassCode(" k7m 2q "))
        assertEquals("K7M2Q", normalizeClassCode("https://bookworm.app/join/K7M2Q"))
        assertEquals("K7M2Q", normalizeClassCode("https://bookworm.app/join/k7m2q?ref=print"))
    }

    @Test
    fun joiningWaitsForTeacherAndShowsInClassroom() = runBlocking {
        val joined = backend.joinClass("K7M2Q")
        assertEquals("Class 3B", joined.name)
        assertEquals(MembershipStatus.WaitingForTeacher, joined.status)
        assertTrue(backend.getMyClasses().any { it.id == joined.id })

        val student = backend.getClassroom(joined.id).students.first { it.id == newStudent.userId }
        assertEquals(StudentStatus.Waiting, student.status)

        backend.admit(joined.id, newStudent.userId)
        assertEquals(MembershipStatus.Joined, backend.getMyClasses().first { it.id == joined.id }.status)
        val error = assertFailsWith<JoinClassException> { backend.joinClass("K7M2Q") }
        assertEquals(JoinFailure.AlreadyInClass, error.failure)
    }

    @Test
    fun joinsStraightAwayWhenTeacherDoesNotApprove() = runBlocking {
        backend.setApproveJoins("class-3a", approve = false)
        assertEquals(MembershipStatus.Joined, backend.joinClass("P4X9T").status)
    }

    @Test
    fun unknownCodeFails() = runBlocking {
        val error = assertFailsWith<JoinClassException> { backend.joinClass("NOPE1") }
        assertEquals(JoinFailure.CodeNotFound, error.failure)
    }
}
