package com.github.sarwaruddin.telegraph.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sarwaruddin.telegraph.signal.Transmitter
import com.github.sarwaruddin.telegraph.ui.theme.Mono
import com.github.sarwaruddin.telegraph.ui.theme.Office
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val PanelShape = RoundedCornerShape(6.dp)
private val TapeShape = RoundedCornerShape(2.dp)

/** A wood panel with a hairline border. */
@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Office.wood, PanelShape)
            .border(1.dp, Office.rule, PanelShape)
            .padding(14.dp),
        content = content,
    )
}

/** Small brass caps label above a section. */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = Office.brassDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.5.sp,
    )
}

/** Cream paper: a telegram form or a strip of ticker tape. */
@Composable
internal fun Tape(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Office.tape, TapeShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content,
    )
}

@Composable
internal fun BrassButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Office.brass, contentColor = Office.mahogany,
            disabledContainerColor = Office.woodHi, disabledContentColor = Office.creamSoft,
        ),
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text)
    }
}

@Composable
internal fun BrassOutlineButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) Office.brassDim else Office.rule),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent, contentColor = Office.brass,
            disabledContainerColor = Color.Transparent, disabledContentColor = Office.creamSoft,
        ),
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text)
    }
}

@Composable
internal fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = Office.creamSoft),
    ) { Text(text, fontSize = 12.sp) }
}

/** A brass type slug: one learned character. */
@Composable
internal fun Slug(c: Char, newest: Boolean) {
    Box(
        Modifier
            .size(width = 24.dp, height = 28.dp)
            .background(if (newest) Office.lamp else Office.brass, RoundedCornerShape(3.dp))
            .border(1.dp, if (newest) Office.lamp else Office.brassDim, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(c.toString(), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Office.mahogany)
    }
}

/** Dots and dashes drawn as round dots and bars. */
@Composable
internal fun MorseGlyph(code: String, color: Color, unit: Dp = 12.dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(unit), verticalAlignment = Alignment.CenterVertically) {
        code.forEach { e ->
            if (e == '.') Box(Modifier.size(unit).background(color, CircleShape))
            else Box(Modifier.size(width = unit * 3, height = unit).background(color, RoundedCornerShape(unit / 2)))
        }
    }
}

/** A little signal lamp that glows while the transmitter keys. Collects state itself, so only it recomposes. */
@Composable
internal fun SignalLamp(modifier: Modifier = Modifier, size: Dp = 14.dp) {
    val flow = remember { Transmitter.state.map { it.on }.distinctUntilChanged() }
    val on by flow.collectAsState(initial = false)
    Box(
        modifier
            .size(size)
            .background(if (on) Office.lamp else Office.woodHi, CircleShape)
            .border(1.dp, if (on) Office.lamp else Office.rule, CircleShape),
    )
}

/** Flow layout: children left to right, wrapping onto new rows. */
@Composable
internal fun Wrap(
    modifier: Modifier = Modifier,
    hSpace: Dp = 4.dp,
    vSpace: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val hp = hSpace.roundToPx()
        val vp = vSpace.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val maxW = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val xs = IntArray(placeables.size)
        val ys = IntArray(placeables.size)
        var x = 0
        var y = 0
        var rowH = 0
        var widest = 0
        placeables.forEachIndexed { i, p ->
            if (x > 0 && x + p.width > maxW) { x = 0; y += rowH + vp; rowH = 0 }
            xs[i] = x; ys[i] = y
            x += p.width
            widest = maxOf(widest, x)
            x += hp
            rowH = maxOf(rowH, p.height)
        }
        val w = (if (constraints.hasBoundedWidth) constraints.maxWidth else widest).coerceAtLeast(constraints.minWidth)
        val h = (y + rowH).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(w, h) { placeables.forEachIndexed { i, p -> p.place(xs[i], ys[i]) } }
    }
}

/** On-screen keys for just the characters in play, plus space, backspace and clear. */
@Composable
internal fun KeyGrid(
    chars: String,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    perRow: Int = 8,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        chars.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { c ->
                    Key(Modifier.weight(1f), onClick = { onKey(c) }) {
                        Text(c.toString(), fontFamily = Mono, fontSize = 18.sp, color = Office.cream)
                    }
                }
                repeat(perRow - row.length) { Spacer(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Key(Modifier.weight(1f), onClick = onClear) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Office.creamSoft, modifier = Modifier.size(18.dp))
            }
            Key(Modifier.weight(4f), onClick = { onKey(' ') }) {
                Text("SPACE", fontSize = 12.sp, letterSpacing = 2.sp, color = Office.creamSoft)
            }
            Key(Modifier.weight(2f), onClick = onBackspace) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Backspace", tint = Office.brass, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Key(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .heightIn(min = 42.dp)
            .background(Office.woodHi, shape)
            .border(1.dp, Office.rule, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A labelled slider in Office colours. */
@Composable
internal fun SettingSlider(
    label: String,
    value: Int,
    range: IntRange,
    step: Int,
    unit: String,
    onChange: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Office.cream, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("$value $unit", color = Office.brass, fontFamily = Mono, fontSize = 13.sp)
        }
        val span = (range.last - range.first).coerceAtLeast(1)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(Math.round(it)) },
            valueRange = range.first.toFloat()..range.last.toFloat().coerceAtLeast(range.first + 1f),
            steps = (span / step - 1).coerceAtLeast(0),
            modifier = Modifier.height(32.dp),
            colors = SliderDefaults.colors(
                thumbColor = Office.brass,
                activeTrackColor = Office.brass,
                inactiveTrackColor = Office.woodHi,
                activeTickColor = Office.brassDim,
                inactiveTickColor = Office.rule,
            ),
        )
    }
}

/** The checked copy, group by group: sent above, copied below, green right, red wrong or missing. */
@Composable
internal fun CheckedCopy(groups: List<List<Cell>>) {
    Wrap(hSpace = 10.dp, vSpace = 8.dp) {
        groups.forEach { g ->
            Row {
                g.forEach { cell -> CheckedCell(cell) }
            }
        }
    }
}

@Composable
private fun CheckedCell(cell: Cell) {
    val bg = when (cell.mark) {
        Mark.RIGHT -> Office.good.copy(alpha = 0.45f)
        Mark.WRONG, Mark.MISSING, Mark.EXTRA -> Office.signal
    }
    val ink = if (cell.mark == Mark.RIGHT) Office.tapeInk else Office.tape
    val style = TextStyle(fontFamily = Mono, fontSize = 17.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    Column(
        Modifier.width(17.dp).background(bg).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text((cell.sent ?: '+').toString(), style = style, color = ink)
        Box(Modifier.fillMaxWidth().height(1.dp).background(ink.copy(alpha = 0.35f)))
        Text((cell.copied ?: '_').toString(), style = style.copy(fontWeight = FontWeight.Normal), color = ink)
    }
}
