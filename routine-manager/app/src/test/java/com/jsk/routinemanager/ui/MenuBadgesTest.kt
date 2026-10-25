package com.jsk.routinemanager.ui

import com.jsk.routinemanager.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuBadgesTest {

    @Test
    fun sampleMenuShowsFourOpenToDosAndOneGroceryItemToBuy() {
        // The two counts differ in the sample, so a swapped badge would fail here.
        assertEquals(MenuBadges(todo = 4, grocery = 1), menuBadges(SampleData.create()))
    }
}
