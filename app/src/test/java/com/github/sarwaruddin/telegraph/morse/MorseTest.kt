package com.github.sarwaruddin.telegraph.morse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseTest {
    @Test fun encodesAndDecodes() {
        assertEquals("... --- ...", Morse.encode("sos"))
        assertEquals("-- --- .-. ... . / -.-. --- -.. .", Morse.encode("morse  code"))
        assertEquals("MORSE CODE", Morse.decode("-- --- .-. ... . / -.-. --- -.. ."))
    }

    @Test fun prosignIsOneRun() {
        val t = Morse.tokenize("<SOS> hi")
        assertEquals(Morse.Token.Char("<SOS>", "...---..."), t[0])
        assertEquals(Morse.Token.WordSpace, t[1])
    }

    @Test fun parisIsFiftyUnits() {
        // The PARIS standard word is 50 units including the trailing word gap.
        val t = Timing(20, 20)
        val steps = schedule(Morse.tokenize("PARIS"), t)
        assertEquals(50 - 7, (totalMs(steps) / t.unitMs).toInt())
    }

    @Test fun farnsworthStretchesGapsOnly() {
        val fast = Timing(20, 20); val slow = Timing(20, 8)
        assertEquals(fast.dotMs, slow.dotMs)
        assertTrue(slow.charGapMs > fast.charGapMs)
        assertTrue(slow.wordGapMs > slow.charGapMs)
    }

    @Test fun kochCharsetGrows() {
        assertEquals("KM", Koch.charset(1))
        assertEquals("KMU", Koch.charset(2))
        assertEquals(100, Koch.accuracy("KM MK", "KMMK"))
        assertEquals(75, Koch.accuracy("KMMK", "KMMM"))
    }

    @Test fun keyDecoderReadsSos() {
        val d = KeyDecoder(initialWpm = 12)   // unit 100 ms
        var t = 0L
        fun key(ms: Long) { d.down(t); t += ms; d.up(t); t += 100 }
        repeat(3) { key(100) }; t += 200; d.tick(t)
        repeat(3) { key(300) }; t += 200; d.tick(t)
        repeat(3) { key(100) }; t += 300; d.tick(t)
        assertEquals("SOS", d.text.toString().trim())
    }
}
