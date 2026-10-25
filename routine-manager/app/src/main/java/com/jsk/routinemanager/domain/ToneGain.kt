package com.jsk.routinemanager.domain

/** Player gain for the volume slider (0..100): (volume/100)² × 0.9 (README › Block-change sound). */
fun toneGain(volume: Int): Float {
    val v = volume / 100f
    return v * v * 0.9f
}
