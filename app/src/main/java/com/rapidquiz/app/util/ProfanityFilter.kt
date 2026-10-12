package com.rapidquiz.app.util

import java.text.Normalizer

/**
 * Takma adlar için temel (Türkçe + İngilizce) küfür/hakaret kontrolü. Yalnızca hızlı istemci geri bildirimidir;
 * kesin doğrulama sunucudadır ve liste bilerek kısa tutulmuştur.
 *
 * Eşleştirme: aksan/Türkçe harf katlama (ç→c, ş→s, ı→i…), basit rakam→harf eşlemesi (5→s, 1→i…),
 * ayırıcıları (boşluk, tire) atma ve art arda tekrarlayan harfleri teke indirme. Kısa/çok anlamlı kelimeler
 * ("sik", "got", "mal", "cunt"…) yalnızca tam sözcük olarak aranır; böylece "Scunthorpe" gibi masum adlar takılmaz.
 */
object ProfanityFilter {
    // Ayırıcılar atıldıktan sonra metnin herhangi bir yerinde aranan, ayırt edici kökler.
    private val stems = listOf(
        "orospu", "kahpe", "pezevenk", "ibne", "gavat", "yarrak", "amcik", "siktir", "sikeyim",
        "sikerim", "sikik", "yavsak", "serefsiz", "gerizekali", "dangalak", "puşt", "gotveren",
        "fuck", "shit", "bitch", "asshole", "bastard", "whore", "motherfucker", "retard",
    ).map(::fold)

    // Katlanmış haliyle yalnızca art arda tekrar indirgemesi YAPILMADAN aranan kökler.
    private val exactStems = listOf("nigger", "nigga", "faggot").map(::fold)

    // Yalnızca ayrı bir sözcük olarak eşleşenler.
    private val words = setOf(
        "sik", "amk", "aq", "oc", "got", "pic", "mal", "salak", "aptal", "cunt", "slut", "dick", "cock",
    )

    fun containsProfanity(raw: String): Boolean {
        val tokens = raw.split(Regex("[\\s-]+")).filter { it.isNotEmpty() }.map { fold(it) }
        if (tokens.any { it in words || squeeze(it) in words }) return true
        val joined = tokens.joinToString("")
        val squeezed = squeeze(joined)
        return stems.any { joined.contains(it) || squeezed.contains(squeeze(it)) } ||
            exactStems.any { joined.contains(it) }
    }

    /** Küçük harf, aksansız, Türkçe 'ı/İ' → 'i', rakam→harf (leet) ve harf/rakam dışını atar. */
    private fun fold(s: String): String {
        val lowered = buildString {
            for (c in s) append(
                when (c) {
                    'İ', 'I', 'ı' -> 'i'
                    else -> c.lowercaseChar()
                },
            )
        }
        val noMarks = Normalizer.normalize(lowered, Normalizer.Form.NFD).filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        return noMarks.map {
            when (it) {
                '0' -> 'o'
                '1' -> 'i'
                '3' -> 'e'
                '4' -> 'a'
                '5' -> 's'
                '7' -> 't'
                else -> it
            }
        }.filter { it.isLetterOrDigit() }.joinToString("")
    }

    /** Art arda tekrarlanan harfleri teke indirir ("siiiktir" → "siktir"). */
    private fun squeeze(s: String): String = buildString {
        for (c in s) if (isEmpty() || last() != c) append(c)
    }
}
