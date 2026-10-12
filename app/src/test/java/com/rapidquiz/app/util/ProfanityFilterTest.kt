package com.rapidquiz.app.util

import com.rapidquiz.app.util.PlayerName.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfanityFilterTest {
    @Test fun turkishProfanityIsDetected() {
        assertTrue(ProfanityFilter.containsProfanity("orospu"))
        assertTrue(ProfanityFilter.containsProfanity("Siktir"))
        assertTrue(ProfanityFilter.containsProfanity("ŞEREFSİZ"))
        assertTrue(ProfanityFilter.containsProfanity("gerizekalı"))
    }

    @Test fun englishProfanityIsDetected() {
        assertTrue(ProfanityFilter.containsProfanity("FuckYou"))
        assertTrue(ProfanityFilter.containsProfanity("bitch"))
        assertTrue(ProfanityFilter.containsProfanity("nigger"))
    }

    @Test fun obfuscationIsDetected() {
        assertTrue(ProfanityFilter.containsProfanity("s1kt1r"))
        assertTrue(ProfanityFilter.containsProfanity("f u c k"))
        assertTrue(ProfanityFilter.containsProfanity("sh-it"))
        assertTrue(ProfanityFilter.containsProfanity("siiiktir"))
        assertTrue(ProfanityFilter.containsProfanity("Orospuuu"))
    }

    @Test fun shortWordsMatchOnlyAsWholeWords() {
        assertTrue(ProfanityFilter.containsProfanity("Ali amk"))
        assertTrue(ProfanityFilter.containsProfanity("sik"))
        assertFalse(ProfanityFilter.containsProfanity("Siklet Ali"))
        assertFalse(ProfanityFilter.containsProfanity("Scunthorpe"))
        assertFalse(ProfanityFilter.containsProfanity("Malatya"))
        assertFalse(ProfanityFilter.containsProfanity("Dickens"))
    }

    @Test fun ordinaryNamesAreAllowed() {
        listOf("Ayşe Nur", "Çağrı Şükrü", "Amina", "Mehmet-Ali", "Niger", "Hancock", "Ali 99", "Pınar", "Sıla").forEach {
            assertFalse(it, ProfanityFilter.containsProfanity(it))
        }
    }

    @Test fun validateReportsInappropriateAfterOtherChecks() {
        assertEquals(Problem.INAPPROPRIATE, PlayerName.validate("Siktir"))
        assertEquals(Problem.INVALID_CHARS, PlayerName.validate("Siktir!"))
        assertEquals(Problem.TOO_SHORT, PlayerName.validate("s"))
        assertNull(PlayerName.validate("Ayşe"))
    }
}
