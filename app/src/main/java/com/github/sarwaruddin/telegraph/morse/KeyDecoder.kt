package com.github.sarwaruddin.telegraph.morse

/**
 * Turns straight-key presses into text, adapting to the operator's speed.
 *
 * Feed it press durations ([down]/[up] with timestamps). A press shorter than the
 * threshold is a dot, longer is a dash. The threshold tracks the operator: it sits
 * halfway (geometric mean) between their recent dot and dash lengths. Silence longer
 * than ~2 units ends a letter, ~5 units ends a word (call [tick] regularly to flush).
 */
class KeyDecoder(initialWpm: Int = 12) {
    private var dotEstimate = 1200.0 / initialWpm
    private var dashEstimate = 3 * dotEstimate
    private var downAt = -1L
    private var lastUpAt = -1L
    private val current = StringBuilder()

    /** Decoded text so far ("?" for unknown codes). */
    val text = StringBuilder()
    /** The dots and dashes of the letter being keyed right now. */
    val pending: String get() = current.toString()
    /** Estimated sending speed. */
    val wpm: Int get() = (1200.0 / dotEstimate).toInt().coerceIn(3, 60)

    private val threshold get() = Math.sqrt(dotEstimate * dashEstimate)
    private val unit get() = dotEstimate

    fun down(now: Long) {
        if (downAt >= 0) return
        flushIfGap(now)
        downAt = now
    }

    fun up(now: Long) {
        if (downAt < 0) return
        val d = (now - downAt).toDouble()
        downAt = -1
        lastUpAt = now
        if (d < 15) return   // contact bounce
        if (d < threshold) {
            current.append('.'); dotEstimate = 0.75 * dotEstimate + 0.25 * d
        } else {
            current.append('-'); dashEstimate = 0.75 * dashEstimate + 0.25 * d
        }
        // keep the two estimates sane relative to each other
        if (dashEstimate < 2 * dotEstimate) dashEstimate = 2 * dotEstimate
        if (dashEstimate > 5 * dotEstimate) dotEstimate = dashEstimate / 5
    }

    /** Call every ~50 ms while idle; returns true if the text changed. */
    fun tick(now: Long): Boolean = if (downAt < 0) flushIfGap(now) else false

    private var wordSpaced = true

    private fun flushIfGap(now: Long): Boolean {
        if (lastUpAt < 0) return false
        val gap = now - lastUpAt
        var changed = false
        // Beginners pause between dots and dashes more than the standard allows, so the
        // letter and word gaps have floors (260 ms / 700 ms) on top of the speed-based ones.
        if (current.isNotEmpty() && gap > maxOf(2.5 * unit, 260.0)) {
            text.append(Morse.decodeChar(current.toString()) ?: "?")
            current.clear(); wordSpaced = false; changed = true
        }
        if (current.isEmpty() && !wordSpaced && gap > maxOf(6.0 * unit, 700.0)) {
            text.append(' '); wordSpaced = true; changed = true
        }
        return changed
    }

    fun clear() { text.clear(); current.clear(); lastUpAt = -1; downAt = -1; wordSpaced = true }
}
