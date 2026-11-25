package com.github.sarwaruddin.telegraph.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sarwaruddin.telegraph.morse.Morse
import com.github.sarwaruddin.telegraph.signal.Output
import com.github.sarwaruddin.telegraph.signal.Settings
import com.github.sarwaruddin.telegraph.signal.Transmitter
import com.github.sarwaruddin.telegraph.ui.common.CodeGlyph
import com.github.sarwaruddin.telegraph.ui.common.Eyebrow
import com.github.sarwaruddin.telegraph.ui.common.Hint
import com.github.sarwaruddin.telegraph.ui.common.Panel
import com.github.sarwaruddin.telegraph.ui.common.ScreenTitle
import com.github.sarwaruddin.telegraph.ui.common.glyphUnits
import com.github.sarwaruddin.telegraph.ui.theme.Mono
import com.github.sarwaruddin.telegraph.ui.theme.Office

private data class Sign(val key: String, val label: String, val code: String, val sendAs: String, val meaning: String? = null)

private val MEANINGS = mapOf(
    "SOS" to "Distress",
    "AR" to "End of message",
    "SK" to "End of contact",
    "BT" to "Break, new paragraph",
    "KN" to "Go ahead, named station only",
    "AS" to "Wait",
    "CT" to "Start of transmission",
    "HH" to "Error, correction follows",
)

private val LETTERS = Morse.CODE.filterKeys { it.isLetter() }.map { (c, code) -> Sign("c$c", c.toString(), code, c.toString()) }
private val DIGITS = Morse.CODE.filterKeys { it.isDigit() }.map { (c, code) -> Sign("c$c", c.toString(), code, c.toString()) }
private val PUNCT = Morse.CODE.filterKeys { !it.isLetterOrDigit() }.map { (c, code) -> Sign("c$c", c.toString(), code, c.toString()) }
private val PROSIGNS = Morse.PROSIGNS.map { (name, code) -> Sign("p$name", name, code, "<$name>", MEANINGS[name]) }

/** The code chart: every sign, tap to hear it. */
@Composable
fun ChartScreen() {
    val state by Transmitter.state.collectAsState()
    var playing by remember { mutableStateOf<String?>(null) }
    val active = if (state.sending) playing else null
    val play: (Sign) -> Unit = {
        playing = it.key
        Transmitter.send(it.sendAs, Settings.timing, setOf(Output.SOUND))
    }
    DisposableEffect(Unit) { onDispose { Transmitter.stop() } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 74.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            ScreenTitle("The code", "International Morse", "Tap any sign to hear it.")
        }
        section("Letters")
        items(LETTERS, key = { it.key }) { SignCell(it, it.key == active, play) }
        section("Figures")
        items(DIGITS, key = { it.key }) { SignCell(it, it.key == active, play) }
        section("Punctuation")
        items(PUNCT, key = { it.key }) { SignCell(it, it.key == active, play) }
        section("Prosigns", "Sent as one run, no gap between the letters. Written with a bar over them.")
        items(PROSIGNS, key = { it.key }, span = { GridItemSpan((maxLineSpan / 2).coerceAtLeast(1)) }) {
            ProsignCell(it, it.key == active, play)
        }
        item(key = "tips", span = { GridItemSpan(maxLineSpan) }) { Tips() }
    }
}

private fun LazyGridScope.section(title: String, note: String? = null) {
    item(key = "s$title", span = { GridItemSpan(maxLineSpan) }) {
        Column(Modifier.padding(top = 14.dp, bottom = 2.dp)) {
            Text(title, color = Office.brass, style = MaterialTheme.typography.titleLarge)
            Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(1.dp).background(Office.rule))
            if (note != null) Hint(note, Modifier.padding(top = 6.dp))
        }
    }
}

private val CellShape = RoundedCornerShape(6.dp)

@Composable
private fun SignCell(s: Sign, lit: Boolean, onPlay: (Sign) -> Unit) {
    Column(
        Modifier
            .clip(CellShape)
            .background(if (lit) Office.woodHi else Office.wood)
            .border(1.dp, if (lit) Office.lamp else Office.rule, CellShape)
            .clickable(role = Role.Button, onClickLabel = "Play") { onPlay(s) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(s.label, color = if (lit) Office.lamp else Office.cream, fontFamily = FontFamily.Serif, fontSize = 26.sp)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.height(10.dp), contentAlignment = Alignment.Center) {
            CodeGlyph(s.code, if (lit) Office.lamp else Office.brass, if (glyphUnits(s.code) > 14) 3.dp else 4.dp)
        }
    }
}

@Composable
private fun ProsignCell(s: Sign, lit: Boolean, onPlay: (Sign) -> Unit) {
    val bar = if (lit) Office.lamp else Office.cream
    Column(
        Modifier
            .clip(CellShape)
            .background(if (lit) Office.woodHi else Office.wood)
            .border(1.dp, if (lit) Office.lamp else Office.rule, CellShape)
            .clickable(role = Role.Button, onClickLabel = "Play") { onPlay(s) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            s.label, color = bar, fontFamily = Mono, fontSize = 20.sp,
            modifier = Modifier.padding(top = 3.dp).drawBehind {
                drawLine(bar, Offset(0f, 0f), Offset(size.width, 0f), 1.5.dp.toPx())
            },
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.height(10.dp), contentAlignment = Alignment.CenterStart) {
            CodeGlyph(s.code, if (lit) Office.lamp else Office.brass, 3.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(s.meaning ?: "", color = Office.creamSoft, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Tips() {
    Panel(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Eyebrow("Tips")
        Spacer(Modifier.height(6.dp))
        Text("Short for the common letters", color = Office.cream, style = MaterialTheme.typography.titleMedium)
        Hint(
            "E is one dot and T one dash: the two most common letters in English. Alfred Vail counted the type " +
                "in a printer's case and gave the busiest letters the shortest codes.",
        )
        Spacer(Modifier.height(10.dp))
        Text("Learn the sound, not the picture", color = Office.cream, style = MaterialTheme.typography.titleMedium)
        Hint("Hear A as \"di-dah\", a rhythm, rather than counting a dot and a dash. The Learn tab trains exactly that.")
        Spacer(Modifier.height(10.dp))
        Text("Timing", color = Office.cream, style = MaterialTheme.typography.titleMedium)
        Text(
            "dot           1\n" +
                "dash          3\n" +
                "inside a sign 1\n" +
                "between signs 3\n" +
                "between words 7",
            color = Office.creamSoft, fontFamily = Mono, fontSize = 13.sp, lineHeight = 19.sp,
            textAlign = TextAlign.Start,
        )
        Hint("In units. At 20 wpm one unit is 60 ms.", Modifier.padding(top = 4.dp))
    }
}
