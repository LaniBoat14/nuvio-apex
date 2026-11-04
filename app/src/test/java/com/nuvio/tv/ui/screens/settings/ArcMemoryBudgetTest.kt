@file:OptIn(UnstableApi::class)

package com.nuvio.tv.ui.screens.settings

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcMemoryBudgetTest {

    @Test
    fun testTotalUsageMb() {
        // totalUsageMb(bufferMb, connectionCount, chunkSizeMb, parallelEnabled)
        // case 1: parallel disabled
        assertEquals(50, ArcMemoryBudget.totalUsageMb(50, 4, 32, false))
        
        // case 2: parallel enabled
        // bufferCount(4) = 4 + 2 = 6
        // overhead = 6 * 32 = 192
        // total = 50 + 192 = 242
        assertEquals(242, ArcMemoryBudget.totalUsageMb(50, 4, 32, true))
    }

    @Test
    fun testParallelOverheadMb() {
        // 3 parallel connections with 8 MB chunk size:
        // bufferCount(3) = 3 + 2 = 5 chunks * 8 MB = 40 MB
        assertEquals(40, ArcMemoryBudget.parallelOverheadMb(connectionCount = 3, chunkSizeMb = 8))

        // 4 parallel connections with 8 MB chunks:
        // bufferCount(4) = 4 + 2 = 6 chunks * 8 MB = 48 MB
        assertEquals(48, ArcMemoryBudget.parallelOverheadMb(connectionCount = 4, chunkSizeMb = 8))
    }

    @Test
    fun testGetUsageStatusNativeAutoMode() {
        // When safeLimitMb = 1000, warningLimitMb = 1250
        // 1. totalUsageMb <= safeLimitMb should be SAFE
        assertEquals(MemoryUsageStatus.SAFE, ArcMemoryBudget.getUsageStatus(500, 1000, 1250))
        assertEquals(MemoryUsageStatus.SAFE, ArcMemoryBudget.getUsageStatus(1000, 1000, 1250))
        
        // 2. totalUsageMb > safeLimitMb && totalUsageMb <= warningLimitMb should be WARNING
        assertEquals(MemoryUsageStatus.WARNING, ArcMemoryBudget.getUsageStatus(1050, 1000, 1250))
        assertEquals(MemoryUsageStatus.WARNING, ArcMemoryBudget.getUsageStatus(1200, 1000, 1250))
        assertEquals(MemoryUsageStatus.WARNING, ArcMemoryBudget.getUsageStatus(1250, 1000, 1250))
        
        // 3. totalUsageMb > warningLimitMb should be DANGER
        assertEquals(MemoryUsageStatus.DANGER, ArcMemoryBudget.getUsageStatus(1260, 1000, 1250))
        assertEquals(MemoryUsageStatus.DANGER, ArcMemoryBudget.getUsageStatus(1500, 1000, 1250))
    }

    @Test
    fun testGetUsageStatusManualMode() {
        // When safeLimitMb = 1000, warningLimitMb = 1250
        // 1. totalUsageMb <= safeLimitMb should be SAFE
        assertEquals(MemoryUsageStatus.SAFE, ArcMemoryBudget.getUsageStatus(500, 1000, 1250))
        assertEquals(MemoryUsageStatus.SAFE, ArcMemoryBudget.getUsageStatus(1000, 1000, 1250))
        
        // 2. totalUsageMb > safeLimitMb && totalUsageMb <= warningLimitMb should be WARNING
        assertEquals(MemoryUsageStatus.WARNING, ArcMemoryBudget.getUsageStatus(1050, 1000, 1250))
        assertEquals(MemoryUsageStatus.WARNING, ArcMemoryBudget.getUsageStatus(1200, 1000, 1250))
        assertEquals(MemoryUsageStatus.WARNING, ArcMemoryBudget.getUsageStatus(1250, 1000, 1250))
        
        // 3. totalUsageMb > warningLimitMb should be DANGER
        assertEquals(MemoryUsageStatus.DANGER, ArcMemoryBudget.getUsageStatus(1260, 1000, 1250))
        assertEquals(MemoryUsageStatus.DANGER, ArcMemoryBudget.getUsageStatus(1500, 1000, 1250))
    }

    @Test
    fun testMemoryBudgetEnforce() {
        // If we choose values well within budget, enforce should return them unchanged
        val withinBuffer = ArcMemoryBudget.MIN_BUFFER_MB
        val withinChunk = ArcMemoryBudget.MIN_CHUNK_MB
        val connections = 2
        val (adjBuf, adjChunk) = ArcMemoryBudget.enforce(withinBuffer, withinChunk, connections)
        assertEquals(withinBuffer, adjBuf)
        assertEquals(withinChunk, adjChunk)
    }

    @Test
    fun testBudgetFollowsDeviceRamWhenBuffersAreOffHeap() {
        val gb = 1024L * 1024L * 1024L
        // A 4 GB box reports about 3.8 GB; its 384 MB heap no longer decides the budget.
        assertEquals(1000, ArcMemoryBudget.budgetMbFor(true, (3.8 * gb).toLong(), 384))
        assertEquals(250, ArcMemoryBudget.budgetMbFor(true, (1.9 * gb).toLong(), 256))
    }

    @Test
    fun testManagedBudgetUsesTheBoxSize() {
        val gb = 1024L * 1024L * 1024L
        assertEquals(200, ArcMemoryBudget.autoBudgetMbFor(true, (1.9 * gb).toLong(), 250))
        assertEquals(1000, ArcMemoryBudget.autoBudgetMbFor(true, (3.8 * gb).toLong(), 1000))
        // Without native memory the heap budget stays.
        assertEquals(174, ArcMemoryBudget.autoBudgetMbFor(false, (1.9 * gb).toLong(), 174))
    }

    @Test
    fun testConversionBudgetFollowsTheSizedBudget() {
        assertTrue(ArcMemoryBudget.conversionBudgetMb(150) <= 150)
        assertEquals(ArcMemoryBudget.MIN_BUFFER_MB, ArcMemoryBudget.conversionBudgetMb(10))
    }

    @Test
    fun testHeapBudgetWhenNativeMemoryIsMissing() {
        assertEquals(174, ArcMemoryBudget.budgetMbFor(false, 0L, 384))
        assertEquals(435, ArcMemoryBudget.budgetMbFor(false, 0L, 512))
    }

    @Test
    fun connectionCeilingsFollowNativeMemory() {
        assertEquals(16, ArcMemoryBudget.maxConnections(nativeMemory = true))
        assertEquals(8, ArcMemoryBudget.maxConnections(nativeMemory = false))
        assertEquals(128, ArcMemoryBudget.MAX_CHUNK_MB)
    }

    @Test
    fun nativeChunkCapKeepsTheDownloadWindowUnderTheLimit() {
        val headroom = if (ArcMemoryBudget.isLowRamTier) 2 else 4
        // An automatic buffer leaves the window at its floor of twice the connections.
        val cap = ArcMemoryBudget.nativeMaxChunkMb(16, limitMb = 1200, safeNativeLimitMb = 1000, reserveBufferMb = 1000, tierMaxMb = 128)
        assertTrue(cap in ArcMemoryBudget.CHUNK_STEPS_MB)
        assertTrue((32 + headroom) * cap <= 1200)
        val next = ArcMemoryBudget.CHUNK_STEPS_MB.first { it > cap }
        assertTrue((32 + headroom) * next > 1200)
        assertEquals(128, ArcMemoryBudget.nativeMaxChunkMb(2, limitMb = 2500, safeNativeLimitMb = 1000, reserveBufferMb = 1000, tierMaxMb = 128))
    }

    @Test
    fun nativeChunkCapNeverGoesAboveTheTierOrBelowEight() {
        assertEquals(16, ArcMemoryBudget.nativeMaxChunkMb(2, limitMb = 4000, safeNativeLimitMb = 150, reserveBufferMb = 150, tierMaxMb = 16))
        assertEquals(8, ArcMemoryBudget.nativeMaxChunkMb(16, limitMb = 100, safeNativeLimitMb = 150, reserveBufferMb = 150, tierMaxMb = 16))
    }

    @Test
    fun savedNativeSettingsUpToFourConnectionsOf32MbStillFit() {
        assertTrue(ArcMemoryBudget.nativeMaxChunkMb(4, limitMb = 1200, safeNativeLimitMb = 1000, reserveBufferMb = 1000, tierMaxMb = 128) >= 32)
        assertTrue(ArcMemoryBudget.nativeMaxChunkMb(4, limitMb = 325, safeNativeLimitMb = 150, reserveBufferMb = 150, tierMaxMb = 16) >= 16)
    }

    @Test
    fun chunkStepsSnapDown() {
        assertEquals(32, ArcMemoryBudget.chunkStepAtOrBelow(33))
        assertEquals(64, ArcMemoryBudget.chunkStepAtOrBelow(95))
        assertEquals(8, ArcMemoryBudget.chunkStepAtOrBelow(3))
        assertEquals(12, ArcMemoryBudget.chunkStepAtOrBelow(15))
        assertEquals(128, ArcMemoryBudget.chunkStepAtOrBelow(500))
    }

    @Test
    fun customChunkCapWithNativeMemoryFollowsTheWindow() {
        val native = com.nuvio.tv.data.local.PlayerSettings(nuvioPerformanceModeEnabled = true, parallelConnectionCount = 16)
        val cap = ArcMemoryBudget.customMaxChunkMb(native, autoBufferMb = 1000, safeNativeLimitMb = 1000, warningLimitMb = 1200, tierMaxMb = 128)
        assertEquals(ArcMemoryBudget.nativeMaxChunkMb(16, 1200, 1000, 1000, 128), cap)
        assertTrue(cap < 128)
        val two = native.copy(parallelConnectionCount = 2)
        assertEquals(128, ArcMemoryBudget.customMaxChunkMb(two, autoBufferMb = 1000, safeNativeLimitMb = 1000, warningLimitMb = 2500, tierMaxMb = 128))
    }

    @Test
    fun customChunkCapWithoutNativeMemoryFitsTheBudget() {
        val plain = com.nuvio.tv.data.local.PlayerSettings(parallelConnectionCount = 8)
        val bufferMb = ArcMemoryBudget.effectiveBufferMb(plain.bufferSettings.targetBufferSizeMb)
        assertEquals(
            ArcMemoryBudget.chunkStepAtOrBelow(ArcMemoryBudget.maxChunkMb(bufferMb, 8)),
            ArcMemoryBudget.customMaxChunkMb(plain, 1000, 1000, 1200)
        )
    }

    private val mb = 1024L * 1024L
    private val am9RamBytes = 3712L * mb
    private val fireStickRamBytes = 1900L * mb

    private fun tierCap(lowHeap: Boolean, offHeap: Boolean, ramBytes: Long, heapMb: Long, native: Boolean, connections: Int) =
        ArcMemoryBudget.tierMaxChunkMbFor(lowHeap, offHeap, ramBytes, heapMb, native, connections)

    @Test
    fun fourGbBoxWithSmallHeapAndNativeMemoryGetsTheFullChunkCeiling() {
        for (connections in listOf(2, 4, 8, 16)) {
            assertEquals(128, tierCap(true, true, am9RamBytes, 384, true, connections))
        }
        val native = com.nuvio.tv.data.local.PlayerSettings(nuvioPerformanceModeEnabled = true, parallelConnectionCount = 4)
        fun cap(settings: com.nuvio.tv.data.local.PlayerSettings) = ArcMemoryBudget.customMaxChunkMb(
            settings, autoBufferMb = 1000, safeNativeLimitMb = 1000, warningLimitMb = 1200,
            tierMaxMb = tierCap(true, true, am9RamBytes, 384, true, settings.parallelConnectionCount)
        )
        assertEquals(96, cap(native))
        assertEquals(32, cap(native.copy(parallelConnectionCount = 16)))
    }

    @Test
    fun fourGbBoxWithoutNativeMemoryKeepsChunksWithinAQuarterOfTheHeap() {
        assertEquals(48, tierCap(true, true, am9RamBytes, 384, false, 2))
        assertEquals(32, tierCap(true, true, am9RamBytes, 384, false, 3))
        assertEquals(24, tierCap(true, true, am9RamBytes, 384, false, 4))
        assertEquals(16, tierCap(true, true, am9RamBytes, 384, false, 5))
        assertEquals(16, tierCap(true, true, am9RamBytes, 384, false, 8))
        assertEquals(16, tierCap(true, false, am9RamBytes, 384, true, 4))
        assertEquals(16, tierCap(true, false, am9RamBytes, 384, false, 2))
    }

    @Test
    fun twoGbBoxesKeepTheSixteenMbCeiling() {
        for (connections in 2..16) {
            for (native in listOf(true, false)) {
                assertEquals(16, tierCap(true, true, fireStickRamBytes, 384, native, connections))
                assertEquals(16, tierCap(true, false, fireStickRamBytes, 256, native, connections))
                assertEquals(16, tierCap(true, true, 0L, 384, native, connections))
            }
        }
    }

    @Test
    fun largeHeapBoxesKeepTheFullChunkCeiling() {
        assertEquals(128, tierCap(false, true, am9RamBytes, 512, true, 16))
        assertEquals(128, tierCap(false, false, fireStickRamBytes, 768, false, 8))
    }
}
