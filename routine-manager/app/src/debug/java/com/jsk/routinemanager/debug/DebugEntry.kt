package com.jsk.routinemanager.debug

import android.os.SystemClock
import androidx.compose.runtime.Composable
import com.jsk.routinemanager.AppContainer
import com.jsk.routinemanager.data.SampleData
import com.jsk.routinemanager.time.AppClock
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * The main code's only way into debug code (SPEC §3.6). The release build has a stub with the same
 * members that does nothing, so the release APK contains no debug screen or fake clock.
 */
object DebugEntry {
    /** Whether a long press on the Settings gear opens the debug screen. */
    const val AVAILABLE = true

    private val fakeClock = FakeAppClock(LocalDateTime::now, SystemClock::elapsedRealtime)

    /** The app's clock: the phone's clock until the debug screen moves it. */
    val clock: AppClock = fakeClock

    /** Emits when the debug screen moves the clock, so the dashboard updates at once. */
    val clockChanges: Flow<Unit> = fakeClock.changes

    @Composable
    fun Screen(container: AppContainer, onBack: () -> Unit) {
        DebugScreen(
            clock = fakeClock,
            ticks = container.minuteTicks,
            onReset = {
                // As the prototype's reset: stop the tone and take a new baseline. The sample data
                // gives every block a new id, which would otherwise count as a block change and play.
                container.tonePlayer.stop()
                container.chime.rebaseline()
                container.repository.update { SampleData.create() }
            },
            onBack = onBack,
        )
    }
}
