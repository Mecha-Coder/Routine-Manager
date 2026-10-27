package com.jsk.routinemanager.sound

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager

/** Three pulses of 400 ms, 200 ms apart: long enough to notice on a desk, short enough not to buzz on. */
private val PATTERN = longArrayOf(0, 400, 200, 400, 200, 400)

/**
 * The block change in Vibrate mode (SPEC D26, §3.1): the same trigger as the tone, but the phone
 * vibrates instead. Uses the alarm vibration usage, like the tone uses the alarm stream, so it is
 * felt whatever the phone's sound mode is.
 *
 * Main thread only.
 */
class BlockVibrator(context: Context) {

    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

    fun vibrate() {
        val effect = VibrationEffect.createWaveform(PATTERN, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}
