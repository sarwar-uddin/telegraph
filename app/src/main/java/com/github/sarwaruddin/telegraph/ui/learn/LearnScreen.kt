package com.github.sarwaruddin.telegraph.ui.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sarwaruddin.telegraph.morse.Koch
import com.github.sarwaruddin.telegraph.morse.Morse
import com.github.sarwaruddin.telegraph.signal.Transmitter
import com.github.sarwaruddin.telegraph.ui.theme.Mono
import com.github.sarwaruddin.telegraph.ui.theme.Office
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.github.sarwaruddin.telegraph.ui.learn.LearnViewModel.Phase
import com.github.sarwaruddin.telegraph.ui.learn.LearnViewModel.Playing

/** Koch course: listen and copy, level up at 90%. */
@Composable
fun LearnScreen() {
    val vm: LearnViewModel = viewModel()
    DisposableEffect(Unit) { onDispose { Transmitter.stop() } }

    val sendingFlow = remember { Transmitter.state.map { it.sending }.distinctUntilChanged() }
    val sending by sendingFlow.collectAsState(initial = false)

    Column(
        Modifier
            .fillMaxSize()
            .background(Office.mahogany)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(vm)
        AnimatedVisibility(
            visible = vm.showSettings,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) { SettingsPanel(vm) }
        MeetCard(vm, hearing = sending && vm.playing == Playing.HEAR)
        DrillCard(vm, drillSending = sending && vm.playing == Playing.DRILL)
        StatsLine(vm)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header(vm: LearnViewModel) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Learn", style = MaterialTheme.typography.headlineMedium, color = Office.cream)
                Text(
                    "Lesson ${vm.level} of ${Koch.maxLevel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Office.brass,
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clickable { vm.showSettings = !vm.showSettings },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Speed and tone",
                    tint = if (vm.showSettings) Office.lamp else Office.brassDim,
                )
            }
        }
        val set = vm.charset
        Wrap(hSpace = 4.dp, vSpace = 4.dp) {
            set.forEachIndexed { i, c -> Slug(c, newest = i == set.lastIndex) }
        }
        val farns = if (vm.effWpm < vm.charWpm) " (Farnsworth)" else ""
        Text(
            "${vm.charWpm} wpm characters · ${vm.effWpm} wpm spacing$farns · ${vm.toneHz} Hz",
            color = Office.creamSoft,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SettingsPanel(vm: LearnViewModel) {
    Panel {
        SectionLabel("Speed and tone")
        Spacer(Modifier.height(6.dp))
        SettingSlider("Character speed", vm.charWpm, LearnViewModel.CHAR_MIN..LearnViewModel.CHAR_MAX, 1, "wpm", vm::updateCharWpm)
        SettingSlider("Effective speed", vm.effWpm, LearnViewModel.EFF_MIN..vm.charWpm, 1, "wpm", vm::updateEffWpm)
        SettingSlider("Tone", vm.toneHz, LearnViewModel.TONE_MIN..LearnViewModel.TONE_MAX, 50, "Hz", vm::updateToneHz)
        Text(
            "Koch: keep characters fast (20+ wpm) so you hear each one as a sound, not a count of dots. Slow the spacing instead.",
            color = Office.creamSoft,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun MeetCard(vm: LearnViewModel, hearing: Boolean) {
    val c = vm.newest
    val code = Morse.CODE[c].orEmpty()
    Panel {
        AnimatedVisibility(
            visible = vm.arrived,
            enter = slideInHorizontally(tween(700)) { -it } + fadeIn(tween(700)),
            exit = fadeOut(),
        ) {
            Column {
                Tape {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Office.brassDim, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "NEW CHARACTER ARRIVED STOP  $c  STOP",
                            fontFamily = Mono,
                            fontSize = 14.sp,
                            color = Office.tapeInk,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        SectionLabel(if (vm.level == 1) "Meet your first characters" else "Meet the new character")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                c.toString(),
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp,
                color = if (hearing) Office.lamp else Office.brass,
                modifier = Modifier.width(72.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MorseGlyph(code, color = if (hearing) Office.lamp else Office.cream, unit = 11.dp)
                Text(
                    code.map { if (it == '.') "di" else "dah" }.joinToString("-").let { s ->
                        // "di-dah" reads better ending on "dit"
                        if (s.endsWith("di")) s + "t" else s
                    },
                    color = Office.creamSoft,
                    fontSize = 13.sp,
                )
            }
        }
        if (vm.level == 1) {
            Spacer(Modifier.height(4.dp))
            Text(
                "K is ${Morse.CODE['K']}  ·  M is ${Morse.CODE['M']}",
                fontFamily = Mono, color = Office.creamSoft, fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrassOutlineButton("Hear it", Icons.Filled.PlayArrow, onClick = vm::hear)
            Spacer(Modifier.width(12.dp))
            SignalLamp()
        }
    }
}

@Composable
private fun DrillCard(vm: LearnViewModel, drillSending: Boolean) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Drill · ${LearnViewModel.GROUPS} groups", Modifier.weight(1f))
            if (vm.phase == Phase.COPYING) {
                Text(
                    if (drillSending) "receiving…" else "copy complete?",
                    color = if (drillSending) Office.lamp else Office.creamSoft,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.width(8.dp))
                SignalLamp(size = 12.dp)
            }
        }
        Spacer(Modifier.height(10.dp))
        when (vm.phase) {
            Phase.READY -> ReadyView(vm)
            Phase.COPYING -> CopyView(vm)
            Phase.CHECKED -> CheckedView(vm)
        }
    }
}

@Composable
private fun ReadyView(vm: LearnViewModel) {
    Tape {
        Text("TELEGRAM FORM", fontSize = 10.sp, letterSpacing = 2.sp, color = Office.tapeInk.copy(alpha = 0.55f))
        Spacer(Modifier.height(6.dp))
        Text(
            "Press Start. Groups of ${vm.charset.length} characters will be sent. " +
                "Copy each character as you hear it; if you miss one, let it go and catch the next.",
            color = Office.tapeInk,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
    Spacer(Modifier.height(12.dp))
    BrassButton("Start", Icons.Filled.PlayArrow, onClick = vm::start, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun CopyView(vm: LearnViewModel) {
    TelegramForm(vm.copy)
    Spacer(Modifier.height(10.dp))
    KeyGrid(vm.charset, onKey = vm::type, onBackspace = vm::backspace, onClear = vm::clearCopy)
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrassButton(
            "Check",
            Icons.Filled.Check,
            onClick = vm::check,
            enabled = vm.copy.isNotBlank(),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        // Replaying is cheating in real Koch practice, so it stays small.
        QuietButton(if (vm.replays == 0) "replay (cheat)" else "replay ×${vm.replays}", onClick = vm::replay)
    }
}

@Composable
private fun TelegramForm(copy: String) {
    Tape {
        Row {
            Text("RECEIVED", fontSize = 10.sp, letterSpacing = 2.sp, color = Office.tapeInk.copy(alpha = 0.55f), modifier = Modifier.weight(1f))
            Text("${copy.count { !it.isWhitespace() }} chars", fontSize = 10.sp, color = Office.tapeInk.copy(alpha = 0.55f))
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Office.tapeInk.copy(alpha = 0.2f)))
        Spacer(Modifier.height(6.dp))
        Text(
            copy + "▁",
            fontFamily = Mono,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            letterSpacing = 1.sp,
            color = Office.tapeInk,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        )
    }
}

@Composable
private fun CheckedView(vm: LearnViewModel) {
    val passed = vm.passed
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "${vm.accuracy}%",
            style = MaterialTheme.typography.displaySmall,
            color = if (passed) Office.good else Office.signal,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (passed) "Passed. ${Koch.PASS_PERCENT}% to move on." else "Need ${Koch.PASS_PERCENT}% to move on.",
            color = Office.creamSoft,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
    Spacer(Modifier.height(8.dp))
    Tape {
        Text("SENT / COPIED", fontSize = 10.sp, letterSpacing = 2.sp, color = Office.tapeInk.copy(alpha = 0.55f))
        Spacer(Modifier.height(6.dp))
        CheckedCopy(vm.cells)
    }
    Spacer(Modifier.height(12.dp))
    if (passed) {
        if (vm.atTop) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Office.lamp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "All ${Koch.ORDER.length} characters. You're an operator.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Office.lamp,
                )
            }
            Spacer(Modifier.height(10.dp))
            BrassOutlineButton("Another drill", Icons.Filled.Refresh, onClick = vm::tryAgain, modifier = Modifier.fillMaxWidth())
        } else {
            val next = Koch.newest(vm.level + 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Office.lamp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Well copied. Next up: $next",
                    style = MaterialTheme.typography.titleMedium,
                    color = Office.lamp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                BrassOutlineButton("Again", Icons.Filled.Refresh, onClick = vm::tryAgain)
                Spacer(Modifier.width(8.dp))
                BrassButton("Next lesson", Icons.AutoMirrored.Filled.ArrowForward, onClick = vm::nextLesson, modifier = Modifier.weight(1f))
            }
        }
    } else {
        BrassButton("Try again", Icons.Filled.Refresh, onClick = vm::tryAgain, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatsLine(vm: LearnViewModel) {
    if (vm.attempts == 0 && vm.bestStreak == 0) return
    val parts = buildList {
        if (vm.attempts > 0) {
            add("${vm.attempts} attempt${if (vm.attempts == 1) "" else "s"} this session")
            add("best ${vm.best}%")
        }
        if (vm.streak > 1) add("${vm.streak} passes in a row")
        if (vm.bestStreak > 0) add("record streak ${vm.bestStreak}")
    }
    Text(
        parts.joinToString(" · "),
        color = Office.creamSoft,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    )
}
