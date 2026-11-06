package com.nuvio.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSpeedupFloorTest {

    @Test
    fun lowMarkWithoutFloorIsFortyPercentOfTheTargetBetweenFourAndTenSeconds() {
        assertEquals(10_000L, AutoSpeedupPolicy.lowMarkMs(45_000L))
        assertEquals(10_000L, AutoSpeedupPolicy.lowMarkMs(120_000L))
        assertEquals(4_000L, AutoSpeedupPolicy.lowMarkMs(10_000L))
        assertEquals(4_000L, AutoSpeedupPolicy.lowMarkMs(0L))
    }

    @Test
    fun lowMarkFollowsTheMinimumBufferWhenItIsHigher() {
        assertEquals(30_000L, AutoSpeedupPolicy.lowMarkMs(45_000L, 30_000L))
        assertEquals(60_000L, AutoSpeedupPolicy.lowMarkMs(120_000L, 60_000L))
    }

    @Test
    fun lowMarkNeverSitsAboveThreeQuartersOfTheTarget() {
        // A floor at the target would count as a dip all the time: the loader tops up to within seconds of it.
        assertEquals(33_750L, AutoSpeedupPolicy.lowMarkMs(45_000L, 45_000L))
        assertEquals(90_000L, AutoSpeedupPolicy.lowMarkMs(120_000L, 600_000L))
    }

    @Test
    fun aLowMinimumNeverWeakensTheAutomaticLowMark() {
        assertEquals(10_000L, AutoSpeedupPolicy.lowMarkMs(120_000L, 5_000L))
        assertEquals(10_000L, AutoSpeedupPolicy.lowMarkMs(45_000L, 0L))
    }

    @Test
    fun lowMarkIgnoresTheFloorWhenTheTargetIsUnknown() {
        assertEquals(4_000L, AutoSpeedupPolicy.lowMarkMs(0L, 30_000L))
    }

    @Test
    fun refillGoalKeepsThirtySecondsUntilTheLowMarkIsRaised() {
        assertEquals(30_000L, AutoSpeedupPolicy.refillGoalMs(45_000L))
        assertEquals(40_000L, AutoSpeedupPolicy.refillGoalMs(45_000L, 30_000L))
        assertEquals(100_000L, AutoSpeedupPolicy.refillGoalMs(120_000L, 90_000L))
    }

    @Test
    fun refillGoalAlwaysEndsAboveTheLowMark() {
        for (target in listOf(15_000L, 30_000L, 45_000L, 60_000L, 90_000L, 120_000L, 180_000L, 600_000L)) {
            for (floor in listOf(0L, 5_000L, 15_000L, 30_000L, 60_000L, 120_000L, 600_000L)) {
                val low = AutoSpeedupPolicy.lowMarkMs(target, floor)
                val goal = AutoSpeedupPolicy.refillGoalMs(target, low)
                assertTrue("target=$target floor=$floor low=$low goal=$goal", goal > low)
                assertTrue("target=$target floor=$floor low=$low", low <= maxOf(10_000L, target * 75 / 100))
            }
        }
    }

    @Test
    fun runwayIsTheTimeToEmptyAtThePaceOfTheFall() {
        // 4 s lost over 8 s with 10 s left: empty in 20 s.
        assertEquals(20_000L, AutoSpeedupPolicy.runwayMs(10_000L, 14_000L, 8_000L))
        // A dead line loses a second per second: the runway is what is left.
        assertEquals(5_000L, AutoSpeedupPolicy.runwayMs(5_000L, 13_000L, 8_000L))
    }

    @Test
    fun runwayIsUnknownWhenTheDownloadsKeepUpOrTheBufferGrows() {
        assertEquals(-1L, AutoSpeedupPolicy.runwayMs(10_000L, 10_500L, 8_000L))
        assertEquals(-1L, AutoSpeedupPolicy.runwayMs(10_000L, 9_000L, 8_000L))
        assertEquals(-1L, AutoSpeedupPolicy.runwayMs(10_000L, 14_000L, 0L))
    }
}
