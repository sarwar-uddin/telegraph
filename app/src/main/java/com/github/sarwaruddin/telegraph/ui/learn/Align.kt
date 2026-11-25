package com.github.sarwaruddin.telegraph.ui.learn

/** How one character of the copy compares with what was sent. */
internal enum class Mark { RIGHT, WRONG, MISSING, EXTRA }

/** One aligned position: [sent] is null for an extra copied character, [copied] null when missed. */
internal data class Cell(val sent: Char?, val copied: Char?, val mark: Mark)

/**
 * Aligns the copy against the sent groups by edit distance (spaces ignored, like
 * Koch.accuracy), then splits the result back into the sent groups so it reads
 * like the drill did. One dropped letter only marks that letter, not the rest.
 */
internal fun align(sentGroups: List<String>, copy: String): List<List<Cell>> {
    val a = sentGroups.joinToString("").uppercase()
    val b = copy.uppercase().filterNot { it.isWhitespace() }
    val n = a.length
    val m = b.length
    val d = Array(n + 1) { IntArray(m + 1) }
    for (i in 0..n) d[i][0] = i
    for (j in 0..m) d[0][j] = j
    for (i in 1..n) for (j in 1..m) {
        d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
    }

    // Backtrace; each cell remembers which sent index it belongs to (for regrouping).
    val cells = ArrayList<Pair<Int, Cell>>(n + m)
    var i = n
    var j = m
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && d[i][j] == d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1) {
            val ok = a[i - 1] == b[j - 1]
            cells += (i - 1) to Cell(a[i - 1], b[j - 1], if (ok) Mark.RIGHT else Mark.WRONG)
            i--; j--
        } else if (i > 0 && d[i][j] == d[i - 1][j] + 1) {
            cells += (i - 1) to Cell(a[i - 1], null, Mark.MISSING)
            i--
        } else {
            cells += (i - 1).coerceAtLeast(0) to Cell(null, b[j - 1], Mark.EXTRA)
            j--
        }
    }
    cells.reverse()

    // sent index -> group index
    val groupOf = IntArray(n.coerceAtLeast(1))
    var k = 0
    sentGroups.forEachIndexed { g, s -> repeat(s.length) { if (k < groupOf.size) groupOf[k++] = g } }

    val out = List(sentGroups.size.coerceAtLeast(1)) { ArrayList<Cell>() }
    for ((idx, cell) in cells) {
        val g = if (n == 0) 0 else groupOf[idx.coerceIn(0, n - 1)]
        out[g.coerceIn(0, out.lastIndex)] += cell
    }
    return out
}
