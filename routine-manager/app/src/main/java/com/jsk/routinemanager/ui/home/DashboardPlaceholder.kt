package com.jsk.routinemanager.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsk.routinemanager.domain.RoutineState
import com.jsk.routinemanager.ui.DashboardUi
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

// Temporary M2 screen: the dashboard state as plain text, to check the data and the minute clock on
// the phone. M3 replaces it with the real dashboard.

@Composable
fun DashboardPlaceholder(dashboard: StateFlow<DashboardUi?>) {
    val c = RmTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            // Design padding or the notch, whichever is larger (SPEC D18).
            .windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets(42.dp, 28.dp, 42.dp, 22.dp))),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Only this composable reads the minute state (ARCHITECTURE §3).
        val ui by dashboard.collectAsStateWithLifecycle()
        val d = ui ?: return@Column
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(d.clock.time, style = RmType.clock)
            Text(d.clock.amPm, style = RmType.clockAmPm, color = c.textSecondary)
        }
        Text("${d.day} ${d.date}", style = RmType.day)
        val r = d.routine
        val bar = d.bar
        if (r is RoutineState.Running && bar != null) {
            Text(
                "${r.title}  ${bar.start.time} ${bar.start.amPm} – ${bar.end.time} ${bar.end.amPm}",
                style = RmType.routineValue,
            )
            Text("LEFT ${bar.left}   progress ${(bar.progress * 100).roundToInt()}%", style = RmType.body)
            Text("NEXT ${r.next}", style = RmType.body)
            Text("Subs: " + r.subs.joinToString(" · ").ifEmpty { "none" }, style = RmType.body)
        } else {
            Text("Bedtime, see you tomorrow", style = RmType.bedtime)
        }
        Text("key ${r.key}", style = RmType.label, color = c.textSecondary)
    }
}
