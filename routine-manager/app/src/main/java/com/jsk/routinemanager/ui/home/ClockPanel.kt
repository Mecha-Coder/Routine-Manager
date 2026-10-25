package com.jsk.routinemanager.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsk.routinemanager.R
import com.jsk.routinemanager.domain.ClockText
import com.jsk.routinemanager.domain.RoutineState
import com.jsk.routinemanager.ui.DashboardUi
import com.jsk.routinemanager.ui.RoutineBar
import com.jsk.routinemanager.ui.theme.RmTheme
import com.jsk.routinemanager.ui.theme.RmType
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

/** The prototype always lays out 5 sub-routine rows, so 2 sub-routines sit at the top, not spread out. */
private const val SUB_ROUTINE_ROWS = 5

/**
 * The landscape dashboard's left panel (README › Screens › 1): clock, day and date, then the
 * current block or Free time, or the Bedtime message. Sizes follow the prototype's markup.
 */
@Composable
fun ClockPanel(dashboard: StateFlow<DashboardUi?>, modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    Box(modifier) {
        // 1 dp divider on the panel's right edge, inset 28 dp top and bottom.
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(vertical = 28.dp)
                .fillMaxHeight()
                .width(1.dp)
                .background(c.divider)
        )
        Column(
            Modifier
                .fillMaxSize()
                // Design padding, or the notch when it is on the left and deeper (SPEC D18).
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(WindowInsetsSides.Left)
                        .union(WindowInsets(left = 42.dp, top = 28.dp, right = 30.dp, bottom = 22.dp))
                )
        ) {
            // Only this panel reads the minute state, so only it redraws each minute (ARCHITECTURE §3).
            val ui by dashboard.collectAsStateWithLifecycle()
            val d = ui ?: return@Column
            ClockRow(d)
            val routine = d.routine
            val bar = d.bar
            if (routine is RoutineState.Running && bar != null) {
                RoutineHeader(bar.left, Modifier.padding(top = 16.dp))
                ProgressBar(bar.progress, Modifier.padding(top = 8.dp))
                BlockTimes(bar.start, bar.end, Modifier.padding(top = 6.dp))
                // Takes the height that is left instead of the design's fixed 132 dp (SPEC §3.4).
                RoutineBody(routine, Modifier.padding(top = 20.dp).weight(1f))
            } else {
                BedtimeMessage(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClockRow(d: DashboardUi) {
    val c = RmTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(d.clock.time, style = RmType.clock, maxLines = 1, softWrap = false)
            Text(
                d.clock.amPm,
                Modifier.padding(top = 2.dp),
                color = c.textSecondary,
                style = RmType.clockAmPm,
            )
        }
        Column(
            Modifier.padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(d.day, style = RmType.day)
            Text(d.date, color = c.textSecondary, style = RmType.date, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun RoutineHeader(left: String, modifier: Modifier) {
    val c = RmTheme.colors
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(22.dp))
            Text("Routine", style = RmType.routineHeading)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LEFT", Modifier.alignByBaseline(), color = c.textSecondary, style = RmType.label)
            Text(left, Modifier.alignByBaseline(), style = RmType.leftValue, maxLines = 1)
        }
    }
}

/**
 * The accent fill and the remaining track are separate 10 dp pills with a 4 dp gap; the track ends
 * in a 4 dp dot inset 3 dp. As in the prototype the track is never under 10 dp, so near the end the
 * bar is clipped on the right rather than squeezed.
 */
@Composable
private fun ProgressBar(progress: Float, modifier: Modifier) {
    val c = RmTheme.colors
    Canvas(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .clipToBounds()
    ) {
        val radius = CornerRadius(5.dp.toPx())
        val filled = size.width * progress
        if (filled > 0f) drawRoundRect(c.accent, size = Size(filled, size.height), cornerRadius = radius)
        val trackStart = filled + 4.dp.toPx()
        val trackWidth = max(10.dp.toPx(), size.width - trackStart)
        drawRoundRect(
            c.divider,
            topLeft = Offset(trackStart, 0f),
            size = Size(trackWidth, size.height),
            cornerRadius = radius,
        )
        val dotRadius = 2.dp.toPx()
        drawCircle(
            c.textPrimary,
            radius = dotRadius,
            center = Offset(trackStart + trackWidth - 3.dp.toPx() - dotRadius, size.height / 2),
        )
    }
}

@Composable
private fun BlockTimes(start: ClockText, end: ClockText, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        BlockTime(start)
        BlockTime(end)
    }
}

@Composable
private fun BlockTime(time: ClockText) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(time.time, Modifier.alignByBaseline(), style = RmType.blockTime)
        Text(time.amPm, Modifier.alignByBaseline(), color = RmTheme.colors.textSecondary, style = RmType.blockTimeAmPm)
    }
}

@Composable
private fun RoutineBody(routine: RoutineState.Running, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(SUB_ROUTINE_ROWS) { SubRoutine(routine.subs.getOrNull(it)) }
        }
        Column(
            Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(RmTheme.colors.accent, CircleShape)
                    )
                    Text("CURRENT", color = RmTheme.colors.textSecondary, style = RmType.label)
                }
                BlockTitle(routine.title)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.End) {
                Text("NEXT", color = RmTheme.colors.textSecondary, style = RmType.label)
                BlockTitle(routine.next)
            }
        }
    }
}

/** One of the 5 sub-routine rows; an empty row keeps its height so the rows stay evenly spaced. */
@Composable
private fun SubRoutine(text: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (text != null) {
            Box(
                Modifier
                    .size(5.dp)
                    .background(RmTheme.colors.textPrimary, CircleShape)
            )
        }
        Text(text.orEmpty(), style = RmType.subRoutine, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BlockTitle(title: String) {
    Text(
        title,
        Modifier.widthIn(max = 190.dp),
        style = RmType.routineValue,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** No block now and no Free-time gap: the message is centred in the space under the clock. */
@Composable
private fun BedtimeMessage(modifier: Modifier) {
    val c = RmTheme.colors
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(20.dp), tint = c.textSecondary)
            Text("Routine", color = c.textSecondary, style = RmType.bedtimeLabel)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_bedtime), null, Modifier.size(26.dp))
            Text("Bedtime, see you tomorrow", style = RmType.bedtime, textAlign = TextAlign.Center)
        }
    }
}
