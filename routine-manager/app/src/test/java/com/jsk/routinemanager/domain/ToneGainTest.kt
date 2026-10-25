package com.jsk.routinemanager.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The squared curve makes the slider feel even to the ear (half the slider is a quarter of the
 * power), and the 0.9 ceiling leaves headroom so the tone never clips at full volume.
 */
class ToneGainTest {

    @Test
    fun gainFollowsTheSquaredCurveWithA09Ceiling() {
        assertEquals(0f, toneGain(0), 1e-6f)
        assertEquals(0.225f, toneGain(50), 1e-6f)
        assertEquals(0.324f, toneGain(60), 1e-6f) // the default volume
        assertEquals(0.9f, toneGain(100), 1e-6f)
    }
}
