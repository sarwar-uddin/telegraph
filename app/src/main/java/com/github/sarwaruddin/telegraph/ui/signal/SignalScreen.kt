package com.github.sarwaruddin.telegraph.ui.signal

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sarwaruddin.telegraph.morse.Morse
import com.github.sarwaruddin.telegraph.morse.Timing
import com.github.sarwaruddin.telegraph.signal.Output
import com.github.sarwaruddin.telegraph.signal.Settings
import com.github.sarwaruddin.telegraph.signal.Transmitter
import com.github.sarwaruddin.telegraph.ui.common.BrassRoundButton
import com.github.sarwaruddin.telegraph.ui.common.BrassSlider
import com.github.sarwaruddin.telegraph.ui.common.Eyebrow
import com.github.sarwaruddin.telegraph.ui.common.Hint
import com.github.sarwaruddin.telegraph.ui.common.LampToggle
import com.github.sarwaruddin.telegraph.ui.common.Panel
import com.github.sarwaruddin.telegraph.ui.common.PaperChip
import com.github.sarwaruddin.telegraph.ui.common.ScreenTitle
import com.github.sarwaruddin.telegraph.ui.common.SignalLamp
import com.github.sarwaruddin.telegraph.ui.common.Tape
import com.github.sarwaruddin.telegraph.ui.common.TapeCell
import com.github.sarwaruddin.telegraph.ui.common.findActivity
import com.github.sarwaruddin.telegraph.ui.common.tapeCells
import com.github.sarwaruddin.telegraph.ui.theme.Mono
import com.github.sarwaruddin.telegraph.ui.theme.Office

private val QUICK = listOf("SOS", "HELLO", "CQ CQ", "73", "I SEE YOU", "<SOS>")
private const val LAMP_MAX_WPM = 12

/** Send a message by sound, flashlight, vibration and an on-screen lamp. */
@Composable
fun SignalScreen() {
    val state by Transmitter.state.collectAsState()
    val hasTorch = remember { Transmitter.hasTorch }

    var text by rememberSaveable { mutableStateOf("") }
    var sound by rememberSaveable { mutableStateOf(true) }
    var lamp by rememberSaveable { mutableStateOf(hasTorch) }
    var vibrate by rememberSaveable { mutableStateOf(false) }
    var repeat by rememberSaveable { mutableStateOf(false) }
    var fullLamp by rememberSaveable { mutableStateOf(false) }
    var lampWpm by remember { mutableIntStateOf(Settings.lampWpm.coerceIn(5, LAMP_MAX_WPM)) }
    var charWpm by remember { mutableIntStateOf(Settings.charWpm.coerceIn(5, 40)) }
    var sentCells by remember { mutableStateOf<List<TapeCell>>(emptyList()) }

    val lampOn = lamp && hasTorch
    val wpm = if (lampOn) lampWpm else charWpm
    val cells = remember(text) { tapeCells(Morse.tokenize(text)) }
    val skipped = remember(text) {
        text.uppercase().any { !it.isWhitespace() && it != '<' && it != '>' && it !in Morse.CODE }
    }
    val outputs = buildSet {
        if (sound) add(Output.SOUND)
        if (lampOn) add(Output.LAMP)
        if (vibrate) add(Output.VIBRATE)
    }
    val canSend = cells.isNotEmpty() && outputs.isNotEmpty()
    val sending = state.sending

    val send: () -> Unit = {
        if (canSend) {
            sentCells = cells
            Transmitter.send(text, Timing(wpm, wpm), outputs, repeat = repeat)
        }
    }
    val sendOrStop: () -> Unit = { if (sending) Transmitter.stop() else send() }

    // Never leave the torch or tone on after leaving the screen; keep the screen on while signalling.
    DisposableEffect(Unit) { onDispose { Transmitter.stop() } }
    val view = LocalView.current
    DisposableEffect(sending) {
        view.keepScreenOn = sending
        onDispose { view.keepScreenOn = false }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenTitle("Signal lamp", "Send a message")

            TelegramForm(text, { text = it.uppercase() }, onSend = send, skipped = skipped)

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QUICK.forEach { q -> PaperChip(q, onClick = { text = q }) }
            }

            Tape(
                cells = if (sending) sentCells else cells,
                highlight = if (sending) state.tokenIndex else -1,
                placeholder = "Your message appears here as tape",
            )

            // the lamp and the key
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    SignalLamp(
                        on = state.on,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { fullLamp = true },
                    )
                    Hint("Tap the lamp for a full-screen flash", textAlign = TextAlign.Center)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    BrassRoundButton(
                        label = if (sending) "STOP" else "SEND",
                        onClick = sendOrStop,
                        enabled = sending || canSend,
                        active = sending,
                    )
                    Spacer(Modifier.height(6.dp))
                    Hint(
                        when {
                            sending && repeat -> "Beacon running"
                            sending -> "Sending at $wpm wpm"
                            outputs.isEmpty() -> "Pick an output below"
                            else -> "$wpm wpm"
                        },
                    )
                }
            }

            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("Outputs")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LampToggle("Sound", sound, { sound = it })
                    LampToggle("Signal lamp", lampOn, { lamp = it }, enabled = hasTorch)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LampToggle("Vibrate", vibrate, { vibrate = it })
                    LampToggle("Repeat", repeat, { repeat = it })
                }
                Spacer(Modifier.height(8.dp))
                if (!hasTorch) {
                    Hint("This phone has no flashlight. The on-screen lamp still flashes: tap it to fill the screen.")
                } else {
                    Hint("Signal lamp uses the flashlight. Repeat loops the message like a beacon.")
                }
                if (sending) Hint("Changes apply to the next send.", Modifier.padding(top = 4.dp))
            }

            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Eyebrow(if (lampOn) "Lamp speed" else "Speed", Modifier.weight(1f))
                    Text("$wpm wpm", color = Office.brass, style = MaterialTheme.typography.titleMedium)
                }
                if (lampOn) {
                    BrassSlider(lampWpm, 5..LAMP_MAX_WPM, { lampWpm = it }, onDone = { Settings.lampWpm = lampWpm })
                    Hint("Lamp is limited to 12 wpm so eyes can read it.")
                } else {
                    BrassSlider(charWpm, 5..40, { charWpm = it }, onDone = { Settings.charWpm = charWpm })
                    Hint("Sent at an even speed, no Farnsworth spacing.")
                }
            }
        }

        if (fullLamp) FullScreenLamp(
            on = state.on,
            sending = sending,
            canSend = canSend,
            onSendOrStop = sendOrStop,
            onClose = { fullLamp = false },
        )
    }
}

/** A telegram form: cream paper, Mono, a printed header. */
@Composable
private fun TelegramForm(value: String, onChange: (String) -> Unit, onSend: () -> Unit, skipped: Boolean) {
    val ink = Office.tapeInk
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(Office.tape).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Telegram", Modifier.weight(1f), color = ink.copy(alpha = 0.6f))
            val words = value.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
            Text(
                if (words == 1) "1 word" else "$words words",
                color = ink.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(ink.copy(alpha = 0.25f)))
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(fontFamily = Mono, fontSize = 20.sp, lineHeight = 28.sp, color = ink),
            cursorBrush = SolidColor(ink),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            "Type your message…", color = ink.copy(alpha = 0.4f),
                            fontFamily = Mono, fontSize = 20.sp, lineHeight = 28.sp,
                        )
                    }
                    inner()
                }
            },
        )
        if (skipped) {
            Text(
                "Some characters have no Morse code and will be skipped.",
                color = Office.signal, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** The whole screen as a lamp, at full brightness: signal across a dark room or street. */
@Composable
private fun FullScreenLamp(
    on: Boolean,
    sending: Boolean,
    canSend: Boolean,
    onSendOrStop: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window) {
        window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = 1f } }
        onDispose {
            window?.let { w ->
                w.attributes = w.attributes.apply {
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(if (on) Office.lamp else Color(0xFF0B0605))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        if (!on) {
            Text(
                "Tap anywhere to close",
                Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                color = Office.creamSoft.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall,
            )
        }
        BrassRoundButton(
            label = if (sending) "STOP" else "SEND",
            onClick = onSendOrStop,
            enabled = sending || canSend,
            active = sending,
            size = 88.dp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        )
    }
}
