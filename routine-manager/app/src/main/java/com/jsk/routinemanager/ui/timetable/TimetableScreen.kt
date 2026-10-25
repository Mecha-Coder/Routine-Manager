package com.jsk.routinemanager.ui.timetable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jsk.routinemanager.R
import com.jsk.routinemanager.ui.components.RmIconButton
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType

/** Placeholder until M9 builds the weekly timetable (README › Screens › 5). Back returns to Settings. */
@Composable
fun TimetableScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val c = RmTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets(left = 14.dp, top = 12.dp, right = 22.dp)))
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RmIconButton(R.drawable.ic_arrow_back, "Back", onBack)
            Text("Weekly routine", style = RmType.screenTitle)
        }
        Text("The timetable comes in M9.", Modifier.padding(start = 18.dp, top = 16.dp), color = c.textSecondary, style = RmType.body)
    }
}
