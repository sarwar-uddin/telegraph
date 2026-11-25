package com.github.sarwaruddin.telegraph.ui.key

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sarwaruddin.telegraph.morse.KeyDecoder
import com.github.sarwaruddin.telegraph.morse.Morse
import com.github.sarwaruddin.telegraph.signal.Sidetone
import com.github.sarwaruddin.telegraph.signal.Transmitter
import com.github.sarwaruddin.telegraph.ui.common.BrassOutlineButton
import com.github.sarwaruddin.telegraph.ui.common.CodeGlyph
import com.github.sarwaruddin.telegraph.ui.common.Eyebrow
import com.github.sarwaruddin.telegraph.ui.common.Hint
import com.github.sarwaruddin.telegraph.ui.common.LampToggle
import com.github.sarwaruddin.telegraph.ui.common.ScreenTitle
import com.github.sarwaruddin.telegraph.ui.common.SignalLamp
import com.github.sarwaruddin.telegraph.ui.common.Tape
import com.github.sarwaruddin.telegraph.ui.common.TapeCell
import com.github.sarwaruddin.telegraph.ui.common.tapeCells
import com.github.sarwaruddin.telegraph.ui.theme.Mono
import com.github.sarwaruddin.telegraph.ui.theme.Office
import kotlinx.coroutines.delay
import kotlin.math.sin

private const val PRESS_DEG = 4f

/** A straight key: press and hold, the decoder turns your timing into letters. */
@Composable
fun KeyScreen() {
    val decoder = remember { KeyDecoder(12) }
    var decoded by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf("") }
    var wpm by remember { mutableIntStateOf(12) }
    var keyed by remember { mutableStateOf(false) }   // has anything been keyed yet
    var pressed by remember { mutableStateOf(false) }
    var sidetone by rememberSaveable { mutableStateOf(true) }
    var flash by rememberSaveable { mutableStateOf(false) }

    fun refresh() {
        decoded = decoder.text.toString()
        pending = decoder.pending
        wpm = decoder.wpm
    }

    val onDown by rememberUpdatedState<(Long) -> Unit> { t ->
        if (!pressed) {
            pressed = true
            keyed = true
            decoder.down(t)
            if (sidetone) Sidetone.down(withLamp = flash)
            else if (flash) Transmitter.torch(true)
            refresh()
        }
    }
    val onUp by rememberUpdatedState<(Long) -> Unit> { t ->
        if (pressed) {
            pressed = false
            decoder.up(t)
            Sidetone.up()   // also switches the torch off
            refresh()
        }
    }

    // flush finished letters and words while the key is idle
    LaunchedEffect(decoder) {
        while (true) {
            delay(40)
            if (decoder.tick(SystemClock.uptimeMillis())) refresh()
        }
    }
    DisposableEffect(Unit) {
        Transmitter.stop()
        onDispose { Sidetone.up() }
    }

    val cells = remember(decoded) {
        tapeCells(Morse.tokenize(decoded)).map { if (it.label == "?") TapeCell("?", "") else it }
    }
    val press = animateFloatAsState(if (pressed) 1f else 0f, tween(durationMillis = 25), label = "key")

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { ScreenTitle("Straight key", "Key a message") }
            SignalLamp(on = pressed && flash, modifier = Modifier.size(48.dp))
        }

        Tape(
            cells = cells,
            scrollTo = cells.lastIndex,
            placeholder = "Your copy appears here",
        )

        // the letter being keyed, and the speed
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Keying", Modifier.width(72.dp))
            if (pending.isNotEmpty()) {
                CodeGlyph(pending, Office.lamp, 8.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    Morse.decodeChar(pending) ?: "?", color = Office.cream,
                    fontFamily = Mono, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                )
            } else {
                Text("—", color = Office.rule, fontFamily = Mono, fontSize = 20.sp)
            }
            Spacer(Modifier.weight(1f))
            if (keyed) Text("~$wpm wpm", color = Office.brass, style = MaterialTheme.typography.titleMedium)
        }

        // the key itself: a big touch target that reacts on pointer down
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 160.dp)
                .semantics { contentDescription = "Telegraph key. Press and hold to send." }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        onDown(down.uptimeMillis)
                        var upAt = -1L
                        try {
                            while (true) {
                                val ev = awaitPointerEvent()
                                ev.changes.forEach { it.consume() }
                                if (ev.changes.none { it.pressed }) {
                                    upAt = ev.changes.firstOrNull()?.uptimeMillis ?: SystemClock.uptimeMillis()
                                    break
                                }
                            }
                        } finally {
                            onUp(if (upAt >= 0) upAt else SystemClock.uptimeMillis())
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            StraightKey(press, Modifier.fillMaxSize())
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LampToggle("Sidetone", sidetone, { sidetone = it })
            LampToggle("Flash lamp", flash, { flash = it }, enabled = Transmitter.hasTorch)
            Spacer(Modifier.weight(1f))
            BrassOutlineButton("Clear", onClick = { decoder.clear(); keyed = false; refresh() })
        }
        Hint(
            if (Transmitter.hasTorch) "Short press = dot, hold = dash. Pause between letters. Flash lamp lights the flashlight while keying."
            else "Short press = dot, hold = dash. Pause between letters.",
        )
    }
}

private val KnobBlack = Color(0xFF15100D)
private val Silver = Color(0xFFD9D4CA)
private val BrassHi = Color(0xFFE8CC8C)

/** Side view of a brass straight key on a wooden base. [press] goes 0 (up) to 1 (down). */
@Composable
private fun StraightKey(press: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // keep the key's proportions on tall or wide boxes
        val kw = minOf(w, h * 2.0f)
        val kh = kw / 2.0f
        val ox = (w - kw) / 2
        val oy = (h - kh) / 2
        fun x(f: Float) = ox + kw * f
        fun y(f: Float) = oy + kh * f

        // wooden base, with a soft shadow
        drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(x(0.05f), y(0.86f)), Size(kw * 0.92f, kh * 0.08f), CornerRadius(kh * 0.04f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Office.woodHi, Office.wood), y(0.70f), y(0.90f)),
            Offset(x(0.04f), y(0.70f)), Size(kw * 0.92f, kh * 0.20f), CornerRadius(kh * 0.03f),
        )
        drawRoundRect(Office.rule, Offset(x(0.04f), y(0.70f)), Size(kw * 0.92f, kh * 0.20f), CornerRadius(kh * 0.03f), style = Stroke(1f))
        // brass base plate
        drawRect(
            Brush.verticalGradient(listOf(BrassHi, Office.brassDim), y(0.65f), y(0.71f)),
            Offset(x(0.10f), y(0.65f)), Size(kw * 0.80f, kh * 0.06f),
        )

        val pivot = Offset(x(0.84f), y(0.42f))
        val contactX = x(0.52f)
        val lowerTip = y(0.57f)
        val drop = (pivot.x - contactX) * sin(Math.toRadians(PRESS_DEG.toDouble())).toFloat()
        val lever = kh * 0.075f

        // trunnion (pivot posts)
        drawRect(Brush.horizontalGradient(listOf(Office.brassDim, BrassHi, Office.brassDim), x(0.80f), x(0.88f)),
            Offset(x(0.80f), y(0.36f)), Size(kw * 0.08f, y(0.65f) - y(0.36f)))
        // lower contact post
        drawRect(Office.brassDim, Offset(contactX - kw * 0.015f, lowerTip), Size(kw * 0.03f, y(0.65f) - lowerTip))
        drawCircle(Silver, kh * 0.018f, Offset(contactX, lowerTip))

        val p = press.value
        // spark at the contact when it closes
        if (p > 0.9f) {
            drawCircle(
                Brush.radialGradient(listOf(Office.lamp, Color.Transparent), Offset(contactX, lowerTip), kh * 0.12f),
                kh * 0.12f, Offset(contactX, lowerTip),
            )
        }

        rotate(degrees = -PRESS_DEG * p, pivot = pivot) {
            drawLever(pivot, lever, contactX, lowerTip - drop, kw, kh, ox)
        }

        // pivot screw head
        drawCircle(Silver, kh * 0.025f, pivot)
        drawCircle(Office.brassDim, kh * 0.025f, pivot, style = Stroke(1.5f))
    }
}

private fun DrawScope.drawLever(
    pivot: Offset, t: Float, contactX: Float, upperTipBottom: Float,
    kw: Float, kh: Float, ox: Float,
) {
    fun x(f: Float) = ox + kw * f
    val top = pivot.y - t / 2
    val bottom = pivot.y + t / 2

    // upper contact screw, hanging under the lever
    if (upperTipBottom > bottom) {
        drawRect(Silver, Offset(contactX - kw * 0.012f, bottom), Size(kw * 0.024f, upperTipBottom - bottom))
    }
    // tension screw and knurled nut on top, near the pivot
    drawRect(Silver, Offset(x(0.69f), top - kh * 0.10f), Size(kw * 0.016f, kh * 0.10f))
    drawRoundRect(
        Brush.horizontalGradient(listOf(Office.brassDim, BrassHi, Office.brassDim), x(0.67f), x(0.716f)),
        Offset(x(0.67f), top - kh * 0.16f), Size(kw * 0.046f, kh * 0.07f), CornerRadius(kh * 0.01f),
    )
    // the lever arm
    drawRoundRect(
        Brush.verticalGradient(listOf(BrassHi, Office.brass, Office.brassDim), top, bottom),
        Offset(x(0.15f), top), Size(pivot.x - x(0.15f) + kw * 0.03f, t), CornerRadius(t / 2),
    )
    // knob: stem, brass collar, black ebonite cap
    val stemX = x(0.20f)
    drawRect(Office.brassDim, Offset(stemX - kw * 0.012f, top - kh * 0.08f), Size(kw * 0.024f, kh * 0.08f))
    drawRoundRect(
        Brush.horizontalGradient(listOf(Office.brassDim, BrassHi, Office.brassDim), stemX - kw * 0.06f, stemX + kw * 0.06f),
        Offset(stemX - kw * 0.06f, top - kh * 0.11f), Size(kw * 0.12f, kh * 0.04f), CornerRadius(kh * 0.01f),
    )
    val capTop = top - kh * 0.27f
    drawOval(
        Brush.verticalGradient(listOf(Color(0xFF3A2C24), KnobBlack), capTop, top - kh * 0.10f),
        Offset(stemX - kw * 0.11f, capTop), Size(kw * 0.22f, kh * 0.17f),
    )
    drawOval(
        Color.White.copy(alpha = 0.10f),
        Offset(stemX - kw * 0.07f, capTop + kh * 0.02f), Size(kw * 0.09f, kh * 0.04f),
    )
}
