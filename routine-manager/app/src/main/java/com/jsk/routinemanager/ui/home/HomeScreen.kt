package com.jsk.routinemanager.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.jsk.routinemanager.ui.DashboardUi
import com.jsk.routinemanager.ui.theme.RmTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Landscape: the clock panel (60 %) beside the list panel (40 %). Portrait: the list panel only
 * (README › Screens › 1 and 2). [dashboard] is handed down unread, so only the clock panel
 * recomposes on the minute.
 */
@Composable
fun HomeScreen(dashboard: StateFlow<DashboardUi?>, onSettings: () -> Unit, onDebug: (() -> Unit)?) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Row(
        Modifier
            .fillMaxSize()
            .background(RmTheme.colors.bg)
    ) {
        if (landscape) {
            ClockPanel(
                dashboard,
                Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
            )
        }
        ListMenu(
            onSettings,
            onDebug,
            notchSides = if (landscape) WindowInsetsSides.Right else WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
        )
    }
}
