package com.nuvio.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSpeedupStartTest {

    private val mb = 1_000_000L

    @Test
    fun lightFilmsOpenOnTwoDownloads() {
        assertEquals(2, AutoSpeedupPolicy.startDownloads(15 * mb, 8, 8L * 1024 * 1024, false, 0L))
    }

    @Test
    fun normalFourKFilmsOpenOnThree() {
        assertEquals(3, AutoSpeedupPolicy.startDownloads(45 * mb, 8, 8L * 1024 * 1024, false, 0L))
    }

    @Test
    fun unknownBitrateStillOpensOnOne() {
        assertEquals(1, AutoSpeedupPolicy.startDownloads(0L, 8, 8L * 1024 * 1024, false, 0L))
    }

    @Test
    fun heavyFilmsKeepTheirLargerStart() {
        assertEquals(3, AutoSpeedupPolicy.startDownloads(70 * mb, 8, 8L * 1024 * 1024, false, 0L))
    }

    @Test
    fun floorHoldsThirtyPercentHeadroom() {
        // 25 Mbit/s film needs 32.5 Mbit/s: 18 Mbit/s per download -> 2, 40 -> 1, 9 -> 4.
        assertEquals(2, AutoSpeedupPolicy.floorFor(25 * mb, 18 * mb / 8))
        assertEquals(1, AutoSpeedupPolicy.floorFor(25 * mb, 40 * mb / 8))
        assertEquals(4, AutoSpeedupPolicy.floorFor(25 * mb, 9 * mb / 8))
    }

    @Test
    fun floorIsBoundedAndSafeWhenUnknown() {
        assertEquals(1, AutoSpeedupPolicy.floorFor(0L, 5 * mb))
        assertEquals(1, AutoSpeedupPolicy.floorFor(25 * mb, 0L))
        assertTrue(AutoSpeedupPolicy.floorFor(80 * mb, 1 * mb / 8) <= AutoSpeedupPolicy.MEASURED_FLOOR_MAX)
    }
}
