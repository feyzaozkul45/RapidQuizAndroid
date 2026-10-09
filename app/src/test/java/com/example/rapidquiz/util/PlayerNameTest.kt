package com.example.rapidquiz.util

import com.example.rapidquiz.util.PlayerName.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerNameTest {
    @Test fun turkishLettersAreValid() = assertNull(PlayerName.validate("Çağrı Şükrü"))

    @Test fun oneCharIsTooShort() = assertEquals(Problem.TOO_SHORT, PlayerName.validate("a"))

    @Test fun twoCharsAreValid() = assertNull(PlayerName.validate("ab"))

    @Test fun twentyCharsAreValid() = assertNull(PlayerName.validate("a".repeat(20)))

    @Test fun twentyOneCharsAreTooLong() = assertEquals(Problem.TOO_LONG, PlayerName.validate("a".repeat(21)))

    @Test fun emojiIsInvalid() = assertEquals(Problem.INVALID_CHARS, PlayerName.validate("Ali 😀"))

    @Test fun whitespaceIsTrimmedAndCollapsed() {
        assertEquals("Ali Veli", PlayerName.normalize("  Ali    Veli  "))
        assertNull(PlayerName.validate("   a    b   "))
    }

    @Test fun lengthUsesCodePoints() {
        // 10 adet U+1D400 (matematiksel kalın A, harf): 20 UTF-16 birimi ama 10 kod noktası
        assertNull(PlayerName.validate("𝐀".repeat(10)))
    }

    @Test fun hyphenAndDigitsAreValid() = assertNull(PlayerName.validate("Ali-Veli 99"))

    @Test fun punctuationIsInvalid() = assertEquals(Problem.INVALID_CHARS, PlayerName.validate("Ali!"))
}
