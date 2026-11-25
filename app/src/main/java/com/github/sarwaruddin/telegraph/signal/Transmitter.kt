package com.github.sarwaruddin.telegraph.signal

import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.github.sarwaruddin.telegraph.morse.Morse
import com.github.sarwaruddin.telegraph.morse.Step
import com.github.sarwaruddin.telegraph.morse.Timing
import com.github.sarwaruddin.telegraph.morse.schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Output { SOUND, LAMP, VIBRATE }

/**
 * Sends Morse through any mix of outputs:
 *  - SOUND: the whole message is rendered to one PCM buffer (sample-accurate timing, soft
 *    5 ms edges so there are no clicks) and played in one go.
 *  - VIBRATE: one VibrationEffect waveform, also timed by the system.
 *  - LAMP: the camera torch, switched step by step. Torches react in ~20-50 ms, so keep the
 *    lamp at 12 wpm or slower for a readable signal.
 * [state] follows along step by step, for highlighting the character being sent.
 */
object Transmitter {
    data class State(val sending: Boolean = false, val tokenIndex: Int = -1, val on: Boolean = false)

    private const val RATE = 24000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private lateinit var app: Context
    private var job: Job? = null
    private var track: AudioTrack? = null
    private var torchId: String? = null
    private var torchOn = false

    fun init(context: Context) {
        app = context.applicationContext
        val cm = app.getSystemService(CameraManager::class.java)
        torchId = runCatching {
            cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        }.getOrNull()
        Sidetone.init()
    }

    val hasTorch: Boolean get() = torchId != null

    /** Send [text]. If [repeat] is true, loop (with a long pause) until [stop]. */
    fun send(text: String, timing: Timing, outputs: Set<Output>, toneHz: Int = Settings.toneHz, repeat: Boolean = false) {
        stop()
        val tokens = Morse.tokenize(text)
        if (tokens.isEmpty() || outputs.isEmpty()) return
        val steps = schedule(tokens, timing)
        job = scope.launch {
            do {
                if (Output.SOUND in outputs) playTone(steps, toneHz)
                if (Output.VIBRATE in outputs) vibrate(steps)
                val start = SystemClock.uptimeMillis()
                var t = 0L
                for (s in steps) {
                    if (!isActive) break
                    _state.value = State(true, s.tokenIndex, s.on)
                    if (Output.LAMP in outputs) torch(s.on)
                    t += s.ms
                    delay((start + t - SystemClock.uptimeMillis()).coerceAtLeast(0))
                }
                torch(false)
                if (repeat && isActive) { _state.value = State(true, -1, false); delay(timing.wordGapMs * 3) }
            } while (repeat && isActive)
            finish()
        }
    }

    fun stop() {
        job?.cancel(); job = null
        finish()
    }

    private fun finish() {
        track?.let { runCatching { it.stop() }; it.release() }
        track = null
        vibrator()?.cancel()
        torch(false)
        _state.value = State()
    }

    // ---- sound ----
    private fun playTone(steps: List<Step>, hz: Int) {
        val total = steps.sumOf { it.ms }
        val pcm = ShortArray(((total + 50) * RATE / 1000).toInt())
        val ramp = RATE * 5 / 1000
        var pos = 0
        for (s in steps) {
            val n = (s.ms * RATE / 1000).toInt()
            if (s.on) for (i in 0 until n) {
                if (pos + i >= pcm.size) break
                val env = when {
                    i < ramp -> 0.5 * (1 - cos(PI * i / ramp))
                    i > n - ramp -> 0.5 * (1 - cos(PI * (n - i) / ramp))
                    else -> 1.0
                }
                pcm[pos + i] = (sin(2 * PI * hz * (pos + i) / RATE) * env * 0.6 * Short.MAX_VALUE).toInt().toShort()
            }
            pos += n
        }
        track = buildTrack(pcm.size).apply { write(pcm, 0, pcm.size); play() }
    }

    internal fun buildTrack(samples: Int): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(samples * 2)
        .build()

    internal val sampleRate get() = RATE

    // ---- vibration ----
    private fun vibrator(): Vibrator? = if (!::app.isInitialized) null else if (Build.VERSION.SDK_INT >= 31)
        app.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else @Suppress("DEPRECATION") app.getSystemService(Vibrator::class.java)

    private fun vibrate(steps: List<Step>) {
        // createWaveform timings alternate off, on, off, on... starting with "off"
        val timings = mutableListOf(0L)
        for (s in steps) {
            val wantOn = timings.size % 2 == 0
            if (s.on == wantOn) timings += s.ms else timings[timings.lastIndex] += s.ms
        }
        vibrator()?.vibrate(VibrationEffect.createWaveform(timings.toLongArray(), -1))
    }

    // ---- lamp ----
    internal fun torch(on: Boolean) {
        val id = torchId ?: return
        if (on == torchOn || !::app.isInitialized) return
        torchOn = on
        runCatching { app.getSystemService(CameraManager::class.java).setTorchMode(id, on) }
    }
}

/** A continuous tone while the key is held (practice oscillator). */
object Sidetone {
    private var track: AudioTrack? = null
    private var hz = 0

    fun init() {}

    fun down(withLamp: Boolean = false) {
        val want = Settings.toneHz
        if (track == null || hz != want) build(want)
        track?.let { runCatching { it.setPlaybackHeadPosition(0); it.play() } }
        if (withLamp) Transmitter.torch(true)
    }

    fun up() {
        track?.let { runCatching { it.pause(); it.setPlaybackHeadPosition(0) } }
        Transmitter.torch(false)
    }

    private fun build(freq: Int) {
        track?.release()
        val rate = Transmitter.sampleRate
        // a whole number of cycles so the loop is seamless; ~1 s
        val cycle = rate.toDouble() / freq
        val cycles = (rate / cycle).toInt().coerceAtLeast(1)
        val n = Math.round(cycles * cycle).toInt()
        val pcm = ShortArray(n) { i -> (sin(2 * PI * cycles * i / n) * 0.5 * Short.MAX_VALUE).toInt().toShort() }
        track = Transmitter.buildTrack(n).apply {
            write(pcm, 0, n)
            setLoopPoints(0, n, -1)   // whole cycles, so the loop is seamless
        }
        hz = freq
    }

    fun release() { track?.release(); track = null }
}

/** Small persisted settings. */
object Settings {
    private lateinit var p: SharedPreferences
    fun init(context: Context) { p = context.getSharedPreferences("telegraph", Context.MODE_PRIVATE) }

    var charWpm: Int
        get() = p.getInt("charWpm", 20); set(v) = p.edit().putInt("charWpm", v).apply()
    var effectiveWpm: Int
        get() = p.getInt("effWpm", 10); set(v) = p.edit().putInt("effWpm", v).apply()
    var toneHz: Int
        get() = if (::p.isInitialized) p.getInt("toneHz", 600) else 600; set(v) = p.edit().putInt("toneHz", v).apply()
    var kochLevel: Int
        get() = p.getInt("kochLevel", 1); set(v) = p.edit().putInt("kochLevel", v).apply()
    var lampWpm: Int
        get() = p.getInt("lampWpm", 8); set(v) = p.edit().putInt("lampWpm", v).apply()
    var bestStreak: Int
        get() = p.getInt("bestStreak", 0); set(v) = p.edit().putInt("bestStreak", v).apply()

    val timing get() = Timing(charWpm, effectiveWpm.coerceAtMost(charWpm))
}
