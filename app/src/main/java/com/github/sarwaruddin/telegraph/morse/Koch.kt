package com.github.sarwaruddin.telegraph.morse

import kotlin.random.Random

/**
 * The Koch method (Ludwig Koch, 1936; popularised by LCWO and G4FON):
 * learn at full character speed from day one, starting with two characters,
 * and add one more each time you copy a session at 90% or better.
 */
object Koch {
    /** The standard LCWO order. */
    const val ORDER = "KMURESNAPTLWI.JZ=FOY,VG5/Q92H38B?47C1D60X"
    const val PASS_PERCENT = 90

    /** Characters in play at [level] (level 1 = "KM"). */
    fun charset(level: Int): String = ORDER.take((level + 1).coerceIn(2, ORDER.length))

    /** The newest character at [level] (the one to drill). */
    fun newest(level: Int): Char = charset(level).last()

    val maxLevel get() = ORDER.length - 1

    /**
     * Random groups for a drill: [groups] groups of 1..5 chars, weighted so the newest
     * character appears about twice as often as the others.
     */
    fun groups(level: Int, groups: Int = 8, random: Random = Random.Default): List<String> {
        val set = charset(level)
        val newest = set.last()
        val bag = set.toList() + List(maxOf(1, set.length / 3)) { newest }
        return List(groups) {
            val len = random.nextInt(2, 6)
            String(CharArray(len) { bag[random.nextInt(bag.size)] })
        }
    }

    /**
     * Character accuracy of [copied] against [sent], 0-100, by edit distance over the
     * whole text (spaces ignored), so one missed letter doesn't wreck the rest.
     */
    fun accuracy(sent: String, copied: String): Int {
        val a = sent.uppercase().filterNot { it.isWhitespace() }
        val b = copied.uppercase().filterNot { it.isWhitespace() }
        if (a.isEmpty()) return 100
        val d = levenshtein(a, b)
        return (100 * (a.length - d).coerceAtLeast(0) / a.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            prev = cur
        }
        return prev[b.length]
    }
}
