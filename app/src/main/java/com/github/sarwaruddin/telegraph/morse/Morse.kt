package com.github.sarwaruddin.telegraph.morse

/**
 * International Morse code (ITU-R M.1677-1) plus common prosigns.
 * Pure Kotlin, no Android: unit-tested.
 */
object Morse {
    val CODE: Map<Char, String> = linkedMapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
        ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
        '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-.",
    )

    /** Prosigns are sent as one run with no letter gap, written <SOS>, <AR>... */
    val PROSIGNS: Map<String, String> = linkedMapOf(
        "SOS" to "...---...", "AR" to ".-.-.", "SK" to "...-.-", "BT" to "-...-",
        "KN" to "-.--.", "AS" to ".-...", "CT" to "-.-.-", "HH" to "........",
    )

    private val DECODE: Map<String, String> =
        CODE.entries.associate { it.value to it.key.toString() } +
            PROSIGNS.filterKeys { it == "SOS" || it == "SK" || it == "HH" }.entries.associate { it.value to "<${it.key}>" }

    /** A token to send: a character, a prosign, or a word space. */
    sealed interface Token {
        data class Char(val text: String, val code: String) : Token   // text: "A" or "<SOS>"
        data object WordSpace : Token
    }

    /** Text -> tokens. Unknown characters are dropped. "<SOS>" style prosigns are recognised. */
    fun tokenize(text: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        val s = text.uppercase()
        while (i < s.length) {
            val c = s[i]
            if (c == '<') {
                val end = s.indexOf('>', i)
                val name = if (end > i) s.substring(i + 1, end) else null
                val code = name?.let { PROSIGNS[it] }
                if (code != null) { out += Token.Char("<$name>", code); i = end + 1; continue }
            }
            if (c.isWhitespace()) {
                if (out.isNotEmpty() && out.last() != Token.WordSpace) out += Token.WordSpace
            } else CODE[c]?.let { out += Token.Char(c.toString(), it) }
            i++
        }
        while (out.lastOrNull() == Token.WordSpace) out.removeAt(out.lastIndex)
        return out
    }

    fun encode(text: String): String = tokenize(text).joinToString(" ") {
        when (it) { is Token.Char -> it.code; Token.WordSpace -> "/" }
    }

    /** ".-" -> "A", or null. Accepts prosign runs too. */
    fun decodeChar(code: String): String? = DECODE[code]

    /** "... --- ... / .-" -> "SOS A" */
    fun decode(morse: String): String = morse.trim().split(Regex("\\s*/\\s*|\\s{3,}")).joinToString(" ") { word ->
        word.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString("") { decodeChar(it) ?: "?" }
    }
}

/**
 * Timing in milliseconds. One "unit" = a dot. dash = 3, gap inside a character = 1,
 * between characters = 3, between words = 7 (PARIS standard: 1200 / wpm ms per unit).
 *
 * Farnsworth: characters are sent at [charWpm] but the gaps between characters and
 * words are stretched so the overall speed is [effectiveWpm] (ARRL formula). This is
 * how you learn: hear letters at full speed as sound shapes, with thinking time between.
 */
data class Timing(val charWpm: Int = 20, val effectiveWpm: Int = 20) {
    val unitMs: Long get() = 1200L / charWpm.coerceIn(5, 60)
    val dotMs get() = unitMs
    val dashMs get() = 3 * unitMs
    val elementGapMs get() = unitMs

    private val farnsworthUnitMs: Double get() {
        val c = charWpm.coerceIn(5, 60).toDouble()
        val s = effectiveWpm.coerceIn(3, charWpm.coerceIn(5, 60)).toDouble()
        if (s >= c) return unitMs.toDouble()
        val ta = (60 * c - 37.2 * s) / (s * c) * 1000.0   // total extra delay per word, ms
        return ta / 19.0                                     // 19 gap-units per PARIS word
    }
    val charGapMs: Long get() = Math.round(3 * farnsworthUnitMs)
    val wordGapMs: Long get() = Math.round(7 * farnsworthUnitMs)
}

/** One on/off step of a transmission. [tokenIndex] says which token it belongs to (for highlighting). */
data class Step(val on: Boolean, val ms: Long, val tokenIndex: Int)

/** Tokens -> an on/off schedule for a sounder, lamp or vibrator. */
fun schedule(tokens: List<Morse.Token>, t: Timing): List<Step> {
    val out = mutableListOf<Step>()
    tokens.forEachIndexed { i, tok ->
        when (tok) {
            Morse.Token.WordSpace -> {
                // replace the preceding char gap with a word gap
                if (out.lastOrNull()?.on == false) out.removeAt(out.lastIndex)
                out += Step(false, t.wordGapMs, i)
            }
            is Morse.Token.Char -> {
                tok.code.forEachIndexed { j, e ->
                    if (j > 0) out += Step(false, t.elementGapMs, i)
                    out += Step(true, if (e == '.') t.dotMs else t.dashMs, i)
                }
                out += Step(false, t.charGapMs, i)
            }
        }
    }
    if (out.lastOrNull()?.on == false) out.removeAt(out.lastIndex)
    return out
}

fun totalMs(steps: List<Step>) = steps.sumOf { it.ms }
