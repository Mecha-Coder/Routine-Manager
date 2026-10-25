package com.jsk.routinemanager.debug

import androidx.compose.runtime.Composable
import com.jsk.routinemanager.AppContainer
import com.jsk.routinemanager.time.AppClock
import com.jsk.routinemanager.time.SystemAppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Release stub of the debug build's DebugEntry: the phone's clock and no debug screen (SPEC §3.6).
 * It keeps the same members so the main code compiles in both builds.
 */
object DebugEntry {
    const val AVAILABLE = false

    val clock: AppClock = SystemAppClock

    val clockChanges: Flow<Unit> = emptyFlow()

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun Screen(container: AppContainer, onBack: () -> Unit) = Unit
}
