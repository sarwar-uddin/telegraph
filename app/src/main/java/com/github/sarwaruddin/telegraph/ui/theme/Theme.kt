package com.github.sarwaruddin.telegraph.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A telegraph office at night: mahogany desk, brass instruments, cream paper tape,
 * an amber signal lamp. Always dark: it's an instrument, and it's kind to eyes at night.
 */
@Immutable
object Office {
    val mahogany = Color(0xFF1E110D)   // background
    val wood = Color(0xFF2C1913)       // cards / panels
    val woodHi = Color(0xFF3A2219)     // raised panels, pressed states
    val rule = Color(0xFF4A3328)       // hairlines
    val brass = Color(0xFFC9A15A)      // primary
    val brassDim = Color(0xFF8E7040)
    val tape = Color(0xFFEFE6CF)       // paper tape / telegram forms
    val tapeInk = Color(0xFF2A2420)
    val cream = Color(0xFFEDE3CC)      // body text
    val creamSoft = Color(0xFFA89880)  // secondary text
    val lamp = Color(0xFFFFC45C)       // signal lamp ON
    val signal = Color(0xFFC2412D)     // wrong / stop / record
    val good = Color(0xFF8DB36B)       // right answer
}

private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif
/** For dots and dashes and copied text. */
val Mono = FontFamily.Monospace

private val OfficeType = Typography(
    displayMedium = TextStyle(fontFamily = Serif, fontSize = 44.sp, lineHeight = 50.sp),
    displaySmall = TextStyle(fontFamily = Serif, fontSize = 36.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Serif, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.5.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.5.sp),
)

@Composable
fun TelegraphTheme(content: @Composable () -> Unit) {
    val o = Office
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = o.brass, onPrimary = o.mahogany,
            primaryContainer = o.woodHi, onPrimaryContainer = o.cream,
            secondary = o.lamp, onSecondary = o.mahogany,
            tertiary = o.signal,
            background = o.mahogany, onBackground = o.cream,
            surface = o.mahogany, onSurface = o.cream,
            surfaceVariant = o.wood, onSurfaceVariant = o.creamSoft,
            surfaceContainer = o.wood, surfaceContainerLow = o.wood, surfaceContainerHigh = o.woodHi,
            surfaceContainerHighest = o.woodHi, surfaceContainerLowest = o.mahogany,
            outline = o.rule, outlineVariant = o.rule, error = o.signal,
        ),
        typography = OfficeType,
        content = content,
    )
}
