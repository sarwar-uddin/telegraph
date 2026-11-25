package com.github.sarwaruddin.telegraph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.github.sarwaruddin.telegraph.signal.Settings
import com.github.sarwaruddin.telegraph.signal.Sidetone
import com.github.sarwaruddin.telegraph.signal.Transmitter
import com.github.sarwaruddin.telegraph.ui.chart.ChartScreen
import com.github.sarwaruddin.telegraph.ui.key.KeyScreen
import com.github.sarwaruddin.telegraph.ui.learn.LearnScreen
import com.github.sarwaruddin.telegraph.ui.signal.SignalScreen
import com.github.sarwaruddin.telegraph.ui.theme.Office
import com.github.sarwaruddin.telegraph.ui.theme.TelegraphTheme

enum class Tab(val label: String) { LEARN("Learn"), SIGNAL("Signal"), KEY("Key"), CHART("Chart") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Office.wood.toArgb()),
        )
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        Settings.init(this)
        Transmitter.init(this)
        setContent { TelegraphTheme { TelegraphApp() } }
    }

    override fun onStop() {
        super.onStop()
        Transmitter.stop()   // never leave the torch or tone running in the background
        Sidetone.up()
    }
}

/**
 * Screens (exact signatures, each in its own package):
 *  ui/learn:  LearnScreen()   Koch course: listen and copy, level up at 90%
 *  ui/signal: SignalScreen()  type a message, send as sound / signal lamp / vibration
 *  ui/key:    KeyScreen()     tap a straight key, live decoding
 *  ui/chart:  ChartScreen()   the code chart; tap a letter to hear it
 */
@Composable
fun TelegraphApp() {
    var tab by rememberSaveable { mutableStateOf(Tab.LEARN) }
    val o = Office
    Scaffold(
        containerColor = o.mahogany,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar(containerColor = o.wood) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { if (tab != t) { Transmitter.stop(); tab = t } },
                        icon = {
                            Icon(when (t) {
                                Tab.LEARN -> Icons.Filled.Star
                                Tab.SIGNAL -> Icons.Filled.Send
                                Tab.KEY -> Icons.Filled.Edit
                                Tab.CHART -> Icons.Filled.List
                            }, contentDescription = null)
                        },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = o.brass, selectedTextColor = o.brass,
                            unselectedIconColor = o.creamSoft, unselectedTextColor = o.creamSoft,
                            indicatorColor = o.woodHi,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(bottom = pad.calculateBottomPadding()).statusBarsPadding()) {
            when (tab) {
                Tab.LEARN -> LearnScreen()
                Tab.SIGNAL -> SignalScreen()
                Tab.KEY -> KeyScreen()
                Tab.CHART -> ChartScreen()
            }
        }
    }
}
