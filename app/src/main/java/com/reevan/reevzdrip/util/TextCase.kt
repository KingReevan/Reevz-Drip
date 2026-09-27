package com.reevan.reevzdrip.util

/**
 * Capitalises the first letter of every word: "blue oxford shirt" -> "Blue Oxford Shirt".
 *
 * The rest of each word is left exactly as typed, rather than lower-cased. Title-casing properly
 * would turn "H&M" into "H&m" and "UNIQLO" into "Uniqlo"; only the first letter was ever the
 * tedious part, so only the first letter is touched.
 *
 * Three rules decide where a word starts:
 * - whitespace starts a new word;
 * - leading punctuation does **not** consume the word, so "(gym)" capitalises the G;
 * - punctuation *inside* a word does not start a new one, so "Levi's" keeps its lowercase s after
 *   the apostrophe, and a leading digit holds the word closed so "2nd" is not "2Nd".
 *
 * Uses [Char.uppercaseChar], which is locale-independent, so the result cannot change with the
 * device's locale (the Turkish dotless i being the classic way that goes wrong).
 *
 * Spacing is preserved character for character — the caller trims if it wants to.
 *
 * Ported unchanged from Reevz Mealz, where it has a unit test covering these cases.
 */
fun capitalizeWords(text: String): String {
    if (text.isEmpty()) return text

    val result = StringBuilder(text.length)
    var atWordStart = true
    for (character in text) {
        when {
            character.isWhitespace() -> {
                atWordStart = true
                result.append(character)
            }

            atWordStart && character.isLetter() -> {
                result.append(character.uppercaseChar())
                atWordStart = false
            }

            character.isLetterOrDigit() -> {
                result.append(character)
                atWordStart = false
            }

            // Punctuation neither opens nor closes a word: it just goes through.
            else -> result.append(character)
        }
    }
    return result.toString()
}
