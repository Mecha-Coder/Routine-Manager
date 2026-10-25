package com.jsk.routinemanager.sound

import com.jsk.routinemanager.domain.RoutineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The wait between a block starting and its tone (SPEC §3.1). */
const val CHIME_DELAY_MS = 350L

/**
 * When the block-change tone plays (SPEC §3.1; the prototype's `tick()`): the routine key changes
 * to a real block while the app is visible, and is still the same [CHIME_DELAY_MS] later. Free
 * time and Bedtime are silent. The first routine after creation or [rebaseline] is the baseline and
 * never plays, so becoming visible is not a change.
 *
 * Main thread only.
 */
class BlockChime(private val scope: CoroutineScope, private val play: () -> Unit) {

    /** Null until the first routine since creation or [rebaseline]. */
    private var lastKey: String? = null
    private var pending: Job? = null

    /** At every minute tick and whenever the blocks change, while the app is visible. */
    fun onRoutine(routine: RoutineState) {
        val previous = lastKey
        lastKey = routine.key
        if (previous == null || routine.key == previous) return
        // Any change cancels a tone still waiting, so it plays only if the key stayed the same.
        pending?.cancel()
        pending = if (routine is RoutineState.Running && !routine.isFreeTime) {
            scope.launch {
                delay(CHIME_DELAY_MS)
                play()
            }
        } else {
            null
        }
    }

    /**
     * The next routine becomes the baseline: when the app is hidden (block starts missed meanwhile
     * are never replayed) and on a debug data reset (new block ids would otherwise count as a change).
     */
    fun rebaseline() {
        pending?.cancel()
        pending = null
        lastKey = null
    }
}
