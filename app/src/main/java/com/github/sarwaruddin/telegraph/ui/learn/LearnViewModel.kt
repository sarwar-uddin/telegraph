package com.github.sarwaruddin.telegraph.ui.learn

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.sarwaruddin.telegraph.morse.Koch
import com.github.sarwaruddin.telegraph.morse.Timing
import com.github.sarwaruddin.telegraph.signal.Output
import com.github.sarwaruddin.telegraph.signal.Settings
import com.github.sarwaruddin.telegraph.signal.Transmitter

/** The Koch lesson: one drill at a time. Lives at activity scope, so it survives tab switches. */
class LearnViewModel : ViewModel() {
    enum class Phase { READY, COPYING, CHECKED }
    enum class Playing { NONE, DRILL, HEAR }

    var level by mutableIntStateOf(Settings.kochLevel.coerceIn(1, Koch.maxLevel)); private set
    var charWpm by mutableIntStateOf(Settings.charWpm.coerceIn(CHAR_MIN, CHAR_MAX)); private set
    var effWpm by mutableIntStateOf(Settings.effectiveWpm.coerceIn(EFF_MIN, charWpm)); private set
    var toneHz by mutableIntStateOf(Settings.toneHz.coerceIn(TONE_MIN, TONE_MAX)); private set

    var groups by mutableStateOf(Koch.groups(level, GROUPS)); private set
    var phase by mutableStateOf(Phase.READY); private set
    var copy by mutableStateOf(""); private set
    var playing by mutableStateOf(Playing.NONE); private set
    var replays by mutableIntStateOf(0); private set

    var accuracy by mutableIntStateOf(0); private set
    internal var cells by mutableStateOf<List<List<Cell>>>(emptyList()); private set
    val passed get() = accuracy >= Koch.PASS_PERCENT

    /** Session stats. [best] is -1 until the first check. */
    var attempts by mutableIntStateOf(0); private set
    var best by mutableIntStateOf(-1); private set
    var streak by mutableIntStateOf(0); private set
    var bestStreak by mutableIntStateOf(Settings.bestStreak); private set

    /** True right after levelling up: the new character "arrives" on a tape. */
    var arrived by mutableStateOf(false); private set
    var showSettings by mutableStateOf(false)

    val charset get() = Koch.charset(level)
    val newest get() = Koch.newest(level)
    val atTop get() = level >= Koch.maxLevel
    private val timing get() = Timing(charWpm, effWpm)

    // ---- sending ----
    fun hear() {
        Transmitter.send(newest.toString(), timing, setOf(Output.SOUND), toneHz)
        playing = Playing.HEAR
    }

    fun start() {
        copy = ""
        replays = 0
        phase = Phase.COPYING
        arrived = false
        sendDrill()
    }

    fun replay() {
        replays++
        sendDrill()
    }

    private fun sendDrill() {
        Transmitter.send(groups.joinToString(" "), timing, setOf(Output.SOUND), toneHz)
        playing = Playing.DRILL
    }

    // ---- copying ----
    fun type(c: Char) {
        if (phase != Phase.COPYING || copy.length >= MAX_COPY) return
        if (c == ' ' && (copy.isEmpty() || copy.endsWith(' '))) return
        copy += c
    }

    fun backspace() { if (phase == Phase.COPYING && copy.isNotEmpty()) copy = copy.dropLast(1) }
    fun clearCopy() { if (phase == Phase.COPYING) copy = "" }

    fun check() {
        if (phase != Phase.COPYING) return
        Transmitter.stop()
        playing = Playing.NONE
        accuracy = Koch.accuracy(groups.joinToString(" "), copy)
        cells = align(groups, copy)
        attempts++
        best = maxOf(best, accuracy)
        if (passed) {
            streak++
            if (streak > bestStreak) { bestStreak = streak; Settings.bestStreak = streak }
        } else streak = 0
        phase = Phase.CHECKED
    }

    fun tryAgain() {
        groups = Koch.groups(level, GROUPS)
        copy = ""
        phase = Phase.READY
        arrived = false
    }

    fun nextLesson() {
        if (atTop) { tryAgain(); return }
        level++
        Settings.kochLevel = level
        groups = Koch.groups(level, GROUPS)
        copy = ""
        phase = Phase.READY
        arrived = true
        hear()
    }

    // ---- settings ----
    fun updateCharWpm(v: Int) {
        val c = v.coerceIn(CHAR_MIN, CHAR_MAX)
        if (c == charWpm) return
        charWpm = c
        Settings.charWpm = c
        if (effWpm > c) updateEffWpm(c)
    }

    fun updateEffWpm(v: Int) {
        val e = v.coerceIn(EFF_MIN, charWpm)
        if (e == effWpm) return
        effWpm = e
        Settings.effectiveWpm = e
    }

    fun updateToneHz(v: Int) {
        val t = (v.coerceIn(TONE_MIN, TONE_MAX) / 10) * 10
        if (t == toneHz) return
        toneHz = t
        Settings.toneHz = t
    }

    companion object {
        const val GROUPS = 7
        const val MAX_COPY = 80
        const val CHAR_MIN = 15
        const val CHAR_MAX = 30
        const val EFF_MIN = 5
        const val TONE_MIN = 400
        const val TONE_MAX = 900
    }
}
