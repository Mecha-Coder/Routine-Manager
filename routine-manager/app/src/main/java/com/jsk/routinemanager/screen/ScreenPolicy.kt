package com.jsk.routinemanager.screen

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.jsk.routinemanager.domain.RoutineState
import com.jsk.routinemanager.domain.ScreenRules

/**
 * Window brightness (0..1) while Bedtime dims the screen (SPEC §3.2), confirmed by the owner in M4.
 * Never 0, which turns some screens fully off (ARCHITECTURE §8); the phone clamps anything below
 * 0.0118 (3/255) to 0.0118 anyway.
 */
const val BEDTIME_BRIGHTNESS = 0.05f

/** During Bedtime the screen stays bright this long after the last touch (SPEC §3.2). */
private const val BRIGHT_AFTER_TOUCH_MS = 30_000L

/**
 * Applies [ScreenRules] to the activity's window while it is visible (ARCHITECTURE §8):
 * keep-screen-on with the low-battery rule, the Bedtime dim, and tap-to-brighten. No receiver,
 * service or timer is left running while the app is hidden.
 *
 * Main thread only.
 */
class ScreenPolicy(private val activity: Activity) {

    private val window get() = activity.window
    private val handler = Handler(Looper.getMainLooper())

    /** The routine at the last minute tick; null until the first one. */
    private var routine: RoutineState? = null

    /** During Bedtime, a touch has brightened the screen and the 30 s timer runs. */
    private var brightened = false

    /** The current gesture woke the dimmed screen, so none of its events reach the UI. */
    private var swallowing = false

    private val dimAgain = Runnable {
        brightened = false
        applyBrightness()
    }

    /** At every minute tick and whenever the blocks change, while the app is visible (SPEC §3.3). */
    fun update(routine: RoutineState) {
        this.routine = routine
        if (routine !is RoutineState.Bedtime) {
            // Bedtime is over: normal brightness at once, and the next Bedtime starts dim.
            handler.removeCallbacks(dimAgain)
            brightened = false
        }
        setKeepScreenOn(keepScreenOn())
        applyBrightness()
    }

    /**
     * Sees every touch on the window first (MainActivity.dispatchTouchEvent). Returns true when the
     * event must not reach the UI: every event of a gesture that started while the screen was dim,
     * so the touch that wakes the screen ticks nothing and presses nothing.
     */
    fun swallows(event: MotionEvent): Boolean {
        val routine = routine ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(dimAgain)
                swallowing = ScreenRules.dim(routine, brightened)
                if (swallowing) {
                    brightened = true
                    applyBrightness()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Bright for 30 s after the finger lifts; a new touch restarts the wait.
                if (routine is RoutineState.Bedtime) handler.postDelayed(dimAgain, BRIGHT_AFTER_TOUCH_MS)
                return swallowing.also { swallowing = false }
            }
        }
        return swallowing
    }

    /** The app is hidden: stop the timer. If it is still Bedtime on return, the screen starts dim. */
    fun onHidden() {
        handler.removeCallbacks(dimAgain)
        brightened = false
        applyBrightness()
    }

    private fun applyBrightness() {
        val routine = routine ?: return
        val value = if (ScreenRules.dim(routine, brightened)) {
            BEDTIME_BRIGHTNESS
        } else {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        val attrs = window.attributes
        if (attrs.screenBrightness == value) return
        attrs.screenBrightness = value
        window.attributes = attrs
    }

    /** Changes the flag only when needed: every window change relayouts, which would cost a frame. */
    private fun setKeepScreenOn(on: Boolean) {
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if ((window.attributes.flags and flag != 0) == on) return
        if (on) window.addFlags(flag) else window.clearFlags(flag)
    }

    /**
     * Reads the last battery broadcast, which the system keeps: a null receiver registers nothing
     * (ARCHITECTURE §8), so nothing listens for battery changes.
     */
    private fun keepScreenOn(): Boolean {
        val battery = ContextCompat.registerReceiver(
            activity,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        ) ?: return true
        // Plugged into AC, USB or a wireless charger (SPEC §3.3), whether or not it is taking charge.
        val charging = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val percent = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) * 100 /
            battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return ScreenRules.keepScreenOn(charging, percent)
    }
}
