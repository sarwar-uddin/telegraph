package com.github.sarwaruddin.telegraph.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sarwaruddin.telegraph.morse.Morse
import com.github.sarwaruddin.telegraph.ui.theme.Mono
import com.github.sarwaruddin.telegraph.ui.theme.Office

/** Width of a code in Morse units: dot 1, dash 3, 1 between elements. */
fun glyphUnits(code: String): Int =
    if (code.isEmpty()) 0 else code.sumOf { if (it == '-') 3 else 1 } + code.length - 1

/** Dots and dashes, drawn to proportion (dash = 3 dots, gaps = 1). Height is one [unit]. */
@Composable
fun CodeGlyph(code: String, color: Color, unit: Dp, modifier: Modifier = Modifier) {
    val units = glyphUnits(code)
    if (units == 0) return
    Canvas(modifier.size(width = unit * units, height = unit)) {
        val u = size.height
        var x = 0f
        code.forEachIndexed { i, e ->
            if (i > 0) x += u
            if (e == '-') {
                drawRoundRect(
                    color, topLeft = Offset(x + u * 0.06f, u * 0.14f),
                    size = Size(3 * u - u * 0.12f, u * 0.72f), cornerRadius = CornerRadius(u * 0.36f),
                )
                x += 3 * u
            } else {
                drawCircle(color, radius = u * 0.42f, center = Offset(x + u / 2, u / 2))
                x += u
            }
        }
    }
}

// ---------------------------------------------------------------- tape

/** One cell on the ticker tape. A word space has an empty [code] and a blank [label]. */
@Immutable
data class TapeCell(val label: String, val code: String) {
    val isSpace: Boolean get() = code.isEmpty() && label.isBlank()
}

fun tapeCells(tokens: List<Morse.Token>): List<TapeCell> = tokens.map {
    when (it) {
        is Morse.Token.Char -> TapeCell(it.text, it.code)
        Morse.Token.WordSpace -> TapeCell(" ", "")
    }
}

/**
 * Cream paper ticker tape: each character's dots and dashes with the character under it.
 * [highlight] is drawn in lamp amber; the tape scrolls to keep [scrollTo] in view.
 */
@Composable
fun Tape(
    cells: List<TapeCell>,
    modifier: Modifier = Modifier,
    highlight: Int = -1,
    scrollTo: Int = highlight,
    placeholder: String = "",
    unit: Dp = 4.dp,
    state: LazyListState = rememberLazyListState(),
) {
    LaunchedEffect(scrollTo, cells.size) {
        if (scrollTo >= 0 && scrollTo < cells.size) state.animateScrollToItem((scrollTo - 3).coerceAtLeast(0))
    }
    val ink = Office.tapeInk
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Office.tape)
            .drawBehind {
                // the paper's edges and a faint centre line where the inker runs
                val edge = ink.copy(alpha = 0.18f)
                drawLine(edge, Offset(0f, 3.dp.toPx()), Offset(size.width, 3.dp.toPx()), 1f)
                drawLine(edge, Offset(0f, size.height - 3.dp.toPx()), Offset(size.width, size.height - 3.dp.toPx()), 1f)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (cells.isEmpty()) {
            Text(
                placeholder, Modifier.padding(horizontal = 16.dp),
                color = ink.copy(alpha = 0.45f), fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyRow(
                state = state,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(cells) { i, cell -> TapeCellView(cell, i == highlight, unit) }
            }
        }
    }
}

@Composable
private fun TapeCellView(cell: TapeCell, lit: Boolean, unit: Dp) {
    val ink = Office.tapeInk
    if (cell.isSpace) {
        Box(Modifier.width(18.dp).height(unit * 2 + 26.dp), contentAlignment = Alignment.Center) {
            Text("/", color = ink.copy(alpha = 0.25f), fontFamily = Mono, fontSize = 14.sp)
        }
        return
    }
    Column(
        Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (lit) Office.lamp else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(unit * 2), contentAlignment = Alignment.Center) {
            CodeGlyph(cell.code, ink, unit)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            cell.label, color = ink, fontFamily = Mono, fontSize = 16.sp,
            fontWeight = if (lit) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ---------------------------------------------------------------- office furniture

private val PanelShape = RoundedCornerShape(6.dp)

/** A wood panel with a hairline border. */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(PanelShape).background(Office.wood).border(1.dp, Office.rule, PanelShape).padding(padding),
        content = content,
    )
}

/** Small letter-spaced label above a heading. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Office.brassDim) {
    Text(text.uppercase(), modifier, color = color, style = MaterialTheme.typography.labelMedium)
}

@Composable
fun ScreenTitle(eyebrow: String, title: String, subtitle: String? = null) {
    Column {
        Eyebrow(eyebrow)
        Text(title, color = Office.cream, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) Text(subtitle, color = Office.creamSoft, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A pill switch with a little indicator lamp. */
@Composable
fun LampToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(if (checked) Office.woodHi else Office.wood)
            .border(1.dp, if (checked) Office.brass else Office.rule, shape)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(if (checked) Office.lamp else Office.rule))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (checked) Office.cream else Office.creamSoft, style = MaterialTheme.typography.labelLarge)
    }
}

/** A slip of cream paper with Mono text; for quick messages. */
@Composable
fun PaperChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier.clip(shape).background(Office.tape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, color = Office.tapeInk, fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** A quiet outlined brass button. */
@Composable
fun BrassOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .border(1.dp, Office.brassDim, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text.uppercase(), color = Office.brass, style = MaterialTheme.typography.labelMedium)
    }
}

private val BrassHi = Color(0xFFE8CC8C)

/** The big round brass SEND / STOP button. */
@Composable
fun BrassRoundButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    size: Dp = 116.dp,
) {
    Box(
        modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(BrassHi, Office.brass, Office.brassDim)))
            .border(if (active) 4.dp else 2.dp, if (active) Office.signal else Office.brassDim, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = Office.mahogany, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
        )
    }
}

/** A brass-coloured slider. */
@Composable
fun BrassSlider(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(Math.round(it)) },
        onValueChangeFinished = onDone,
        valueRange = range.first.toFloat()..range.last.toFloat(),
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = Office.brass,
            activeTrackColor = Office.brass,
            inactiveTrackColor = Office.rule,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
    )
}

/**
 * A signal lamp: brass bezel, amber glass that glows when [on].
 * Fills the given size; keep it square.
 */
@Composable
fun SignalLamp(on: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        if (on) {
            drawCircle(
                Brush.radialGradient(listOf(Office.lamp.copy(alpha = 0.55f), Color.Transparent), c, r),
                radius = r, center = c,
            )
        }
        val bezel = r * 0.78f
        drawCircle(Brush.radialGradient(listOf(BrassHi, Office.brass, Office.brassDim), c, bezel), bezel, c)
        drawCircle(Office.mahogany, bezel * 0.86f, c)
        val glass = bezel * 0.80f
        if (on) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4D6), Office.lamp, Color(0xFFE39A2E)), c, glass), glass, c)
        } else {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF4A3220), Color(0xFF1A0F0A)), c, glass), glass, c)
        }
        drawCircle(
            Color.White.copy(alpha = if (on) 0.55f else 0.10f), glass * 0.16f,
            Offset(c.x - glass * 0.42f, c.y - glass * 0.42f),
        )
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(text, modifier, color = Office.creamSoft, style = MaterialTheme.typography.bodySmall, textAlign = textAlign)
}

@Composable
fun RowSpacer(width: Dp = 8.dp) = Spacer(Modifier.width(width))

/** Arrangement helper so screens read the same. */
val Gap = Arrangement.spacedBy(14.dp)

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
