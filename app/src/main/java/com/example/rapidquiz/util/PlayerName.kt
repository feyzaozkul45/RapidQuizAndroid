package com.example.rapidquiz.util

object PlayerName {
    const val MIN = 2
    const val MAX = 20
    private val pattern = Regex("^[\\p{L}\\p{N} -]+$")
    private val whitespace = Regex("\\s+")

    fun normalize(raw: String) = raw.trim().replace(whitespace, " ")

    enum class Problem { TOO_SHORT, TOO_LONG, INVALID_CHARS }

    fun validate(raw: String): Problem? {
        val n = normalize(raw)
        val len = n.codePointCount(0, n.length) // Python len() ile aynı sayım
        return when {
            len < MIN -> Problem.TOO_SHORT
            len > MAX -> Problem.TOO_LONG
            !pattern.matches(n) -> Problem.INVALID_CHARS
            else -> null
        }
    }
}
