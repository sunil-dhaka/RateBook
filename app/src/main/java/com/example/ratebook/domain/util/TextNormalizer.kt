package com.example.ratebook.domain.util

object TextNormalizer {

    private val hindiToLatinMap = mapOf(
        '\u0905' to "a",      // अ
        '\u0906' to "aa",     // आ
        '\u0907' to "i",      // इ
        '\u0908' to "ee",     // ई
        '\u0909' to "u",      // उ
        '\u090A' to "oo",     // ऊ
        '\u090B' to "ri",     // ऋ
        '\u090F' to "e",      // ए
        '\u0910' to "ai",     // ऐ
        '\u0913' to "o",      // ओ
        '\u0914' to "au",     // औ
        '\u0915' to "k",      // क
        '\u0916' to "kh",     // ख
        '\u0917' to "g",      // ग
        '\u0918' to "gh",     // घ
        '\u0919' to "ng",     // ङ
        '\u091A' to "ch",     // च
        '\u091B' to "chh",    // छ
        '\u091C' to "j",      // ज
        '\u091D' to "jh",     // झ
        '\u091E' to "ny",     // ञ
        '\u091F' to "t",      // ट
        '\u0920' to "th",     // ठ
        '\u0921' to "d",      // ड
        '\u0922' to "dh",     // ढ
        '\u0923' to "n",      // ण
        '\u0924' to "t",      // त
        '\u0925' to "th",     // थ
        '\u0926' to "d",      // द
        '\u0927' to "dh",     // ध
        '\u0928' to "n",      // न
        '\u092A' to "p",      // प
        '\u092B' to "ph",     // फ
        '\u092C' to "b",      // ब
        '\u092D' to "bh",     // भ
        '\u092E' to "m",      // म
        '\u092F' to "y",      // य
        '\u0930' to "r",      // र
        '\u0932' to "l",      // ल
        '\u0935' to "v",      // व
        '\u0936' to "sh",     // श
        '\u0937' to "sh",     // ष
        '\u0938' to "s",      // स
        '\u0939' to "h",      // ह
        '\u093E' to "aa",     // ा
        '\u093F' to "i",      // ि
        '\u0940' to "ee",     // ी
        '\u0941' to "u",      // ु
        '\u0942' to "oo",     // ू
        '\u0943' to "ri",     // ृ
        '\u0947' to "e",      // े
        '\u0948' to "ai",     // ै
        '\u094B' to "o",      // ो
        '\u094C' to "au",     // ौ
        '\u094D' to "",       // ् (halant - no sound)
        '\u0902' to "n",      // ं (anusvara)
        '\u0903' to "h",      // ः (visarga)
        '\u0901' to "n",      // ँ (chandrabindu)
        '\u093C' to "",       // ़ (nukta)
        '\u0964' to ".",      // । (danda)
        '\u0965' to "..",     // ॥ (double danda)
        '\u0966' to "0",      // ०
        '\u0967' to "1",      // १
        '\u0968' to "2",      // २
        '\u0969' to "3",      // ३
        '\u096A' to "4",      // ४
        '\u096B' to "5",      // ५
        '\u096C' to "6",      // ६
        '\u096D' to "7",      // ७
        '\u096E' to "8",      // ८
        '\u096F' to "9"       // ९
    )

    fun normalize(text: String): String {
        val result = StringBuilder()
        for (char in text) {
            when {
                char in hindiToLatinMap -> result.append(hindiToLatinMap[char])
                char.isWhitespace() -> result.append(' ')
                char.isLetterOrDigit() -> result.append(char.lowercaseChar())
                else -> result.append(char)
            }
        }
        return result.toString().trim()
    }

    fun normalizeForSearch(query: String): String {
        return normalize(query)
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }
}
