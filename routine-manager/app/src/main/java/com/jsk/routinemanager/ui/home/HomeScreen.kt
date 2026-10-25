package com.jsk.routinemanager.ui.home

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsk.routinemanager.ui.MainViewModel
import com.jsk.routinemanager.ui.lists.ListDetail
import com.jsk.routinemanager.ui.theme.RmTheme

/**
 * Landscape: the clock panel (60 %) beside the list panel (40 %). Portrait: the list panel only
 * (README › Screens › 1 and 2). The dashboard flow is handed down unread, so only the clock panel
 * recomposes on the minute; the list panel never reads it.
 */
@Composable
fun HomeScreen(viewModel: MainViewModel, onSettings: () -> Unit, onDebug: (() -> Unit)?) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Row(
        Modifier
            .fillMaxSize()
            .background(RmTheme.colors.bg)
    ) {
        if (landscape) {
            ClockPanel(
                viewModel.dashboard,
                Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
            )
        }
        ListPanel(
            viewModel,
            onSettings,
            onDebug,
            notchSides = if (landscape) WindowInsetsSides.Right else WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
        )
    }
}

/**
 * The menu, or the open list in its place (README › "Tapping a row replaces only the right
 * panel"). The open list lives in the ViewModel, so it stays open through a rotation.
 */
@Composable
private fun ListPanel(
    viewModel: MainViewModel,
    onSettings: () -> Unit,
    onDebug: (() -> Unit)?,
    notchSides: WindowInsetsSides,
    modifier: Modifier,
) {
    val open by viewModel.openList.collectAsStateWithLifecycle()
    val list = open
    if (list == null) {
        val badges by viewModel.badges.collectAsStateWithLifecycle()
        ListMenu(badges, viewModel::showList, onSettings, onDebug, notchSides, modifier)
    } else {
        // System Back goes from a list to the menu.
        BackHandler(onBack = viewModel::showMenu)
        val data by viewModel.data.collectAsStateWithLifecycle()
        ListDetail(
            list,
            data,
            onBack = viewModel::showMenu,
            onToggleDone = viewModel::toggleDone,
            onToggleGroceryItem = viewModel::toggleGroceryItem,
            onToggleCollapsed = viewModel::toggleCollapsed,
            notchSides = notchSides,
            modifier = modifier,
        )
    }
}
