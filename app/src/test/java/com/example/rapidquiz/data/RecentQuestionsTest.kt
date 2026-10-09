package com.example.rapidquiz.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentQuestionsTest {
    @Test fun appendsToEnd() = assertEquals(listOf(1, 2, 3), RecentQuestions.add(listOf(1, 2), 3))

    @Test fun existingIdMovesToEnd() = assertEquals(listOf(2, 3, 1), RecentQuestions.add(listOf(1, 2, 3), 1))

    @Test fun keepsOnlyLast40() {
        val full = (1..40).toList()
        val result = RecentQuestions.add(full, 41)
        assertEquals(40, result.size)
        assertEquals(2, result.first())
        assertEquals(41, result.last())
    }

    @Test fun parseDropsNonPositiveAndGarbage() =
        assertEquals(listOf(5, 7), RecentQuestions.parse("5,0,-3,x,7"))

    @Test fun parseNullIsEmpty() = assertEquals(emptyList<Int>(), RecentQuestions.parse(null))

    @Test fun roundTrip() = assertEquals(listOf(4, 9), RecentQuestions.parse(RecentQuestions.serialize(listOf(4, 9))))
}
