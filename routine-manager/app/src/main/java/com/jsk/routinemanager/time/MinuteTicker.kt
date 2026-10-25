package com.jsk.routinemanager.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/**
 * Now, then once at every minute boundary of [clock], and at once whenever [changes] emits (the
 * phone's time or time zone was set; later, a debug clock jump). This is the app's only timer: one
 * wake-up per minute, and none while nothing collects it (ARCHITECTURE §5).
 *
 * The wait is worked out again from [AppClock.now] every minute, so drift can't build up. A second
 * value within the same minute (a time change that keeps the minute) is dropped, so it can't cause
 * an extra redraw.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun minuteTicks(clock: AppClock, changes: Flow<Unit>): Flow<LocalDateTime> =
    changes
        .onStart { emit(Unit) }
        .flatMapLatest {
            flow {
                while (true) {
                    val now = clock.now()
                    emit(now)
                    val speed = clock.speed
                    if (speed <= 0.0) awaitCancellation() // Paused debug clock: wait for a change.
                    delay(realMillisToNextMinute(now, speed))
                }
            }
        }
        .distinctUntilChangedBy { it.truncatedTo(ChronoUnit.MINUTES) }

/** Real milliseconds until a clock running at [speed] reaches the next whole minute. At least 1. */
internal fun realMillisToNextMinute(now: LocalDateTime, speed: Double): Long {
    val clockMillisLeft = 60_000 - (now.second * 1_000 + now.nano / 1_000_000)
    return ceil(clockMillisLeft / speed).toLong().coerceAtLeast(1)
}

/**
 * Emits whenever the phone's time, time zone or date is changed. The receiver is registered in code
 * only while this flow is collected, which is while the app is visible (ARCHITECTURE §5).
 */
fun Context.timeChanges(): Flow<Unit> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(Unit)
        }
    }
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
        addAction(Intent.ACTION_DATE_CHANGED)
    }
    // System broadcasts still reach a receiver that isn't exported.
    ContextCompat.registerReceiver(this@timeChanges, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    awaitClose { unregisterReceiver(receiver) }
}
