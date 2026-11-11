package com.nuvio.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The runway: a buffer that would run out within [AutoSpeedupPolicy.RUNWAY_ACT_MS] at the pace it is falling, while the
 * loader runs, is refilled at once, not once it has dropped under the low mark.
 */
class AutoSpeedupRunwayTest {
    private var now = 0L
    private var bytes = 0L
    private var ahead = 0L
    private var position = 0L
    private var requestMs = 0L
    private var playerBytes = 0L
    private val log = mutableListOf<String>()

    /** What the buffer held when each logged change happened. */
    private val aheadAtLog = mutableListOf<Long>()

    private fun AutoSpeedupPolicy.play(
        seconds: Int,
        kbps: Long,
        aheadPerSecondMs: Long = 0L,
        loading: Boolean = true,
        fileMbps: Long = 80L,
        targetMs: Long = 60_000L,
        bufferFull: Boolean = false
    ): Int {
        val requests = downloads
        repeat(seconds * 2) {
            now += 500L
            bytes += kbps * 1000L / 8L / 2L
            requestMs += requests * 500L
            position += 500L
            ahead = (ahead + aheadPerSecondMs / 2L).coerceAtLeast(0L)
            val mediaMs = (aheadPerSecondMs / 2L + 500L).coerceAtLeast(0L)
            playerBytes += fileMbps * 1_000_000L / 8L * mediaMs / 1000L
            onSample(
                AutoSpeedupPolicy.Sample(
                    now, ahead, loading, 0, bytes, bufferFull = bufferFull,
                    fileBitrate = fileMbps * 1_000_000L, requestMs = requestMs, requests = requests,
                    playerBytes = playerBytes, bufferedMs = position + ahead, bufferTargetMs = targetMs
                )
            )
            lastChange?.let {
                log += it
                aheadAtLog += ahead
            }
        }
        return downloads
    }

    /** A film running on [n] downloads with a healthy buffer of [aheadMs]. */
    private fun steady(n: Int, aheadMs: Long = 40_000L, targetMs: Long = 60_000L): AutoSpeedupPolicy {
        val p = AutoSpeedupPolicy(8)
        p.preset(n)
        ahead = aheadMs
        p.play(12, 120_000, loading = false, targetMs = targetMs)
        assertEquals(n, p.downloads)
        log.clear()
        aheadAtLog.clear()
        return p
    }

    @Test
    fun `a buffer falling steadily is boosted before the low mark`() {
        val p = steady(2)
        // 60 s target: low mark 10 s. Falls 0.8 s per second while the loader runs.
        p.play(40, 70_000, aheadPerSecondMs = -800)
        val boost = log.indexOfFirst { it.contains("boost runway") }
        assertTrue("no runway boost in $log", boost >= 0)
        assertTrue("boosted too late, buffer ${aheadAtLog[boost]}", aheadAtLog[boost] > 10_000L)
        assertTrue(log.none { it.contains("boost dip") })
        assertTrue(p.downloads > 2)
    }

    @Test
    fun `a slow fall far from empty waits`() {
        val p = steady(2)
        // 0.15 s per second from 40 s: 40 s of film gone in 4 minutes, nothing to do yet.
        p.play(60, 70_000, aheadPerSecondMs = -150)
        assertTrue(log.none { it.contains("boost") })
        assertEquals(2, p.downloads)
    }

    @Test
    fun `a flat or growing buffer never triggers it`() {
        val p = steady(2)
        p.play(120, 70_000, aheadPerSecondMs = 0)
        p.play(60, 200_000, aheadPerSecondMs = 300)
        assertTrue(log.none { it.contains("boost") })
        assertEquals(-1L, p.runwayNowMs)
    }

    @Test
    fun `a buffer falling while the loader rests is the top-up cycle, not a shortfall`() {
        val p = steady(2)
        // Loader off and falling, then on and refilling, over and over.
        repeat(8) {
            p.play(6, 0, aheadPerSecondMs = -500, loading = false)
            p.play(4, 200_000, aheadPerSecondMs = 750, loading = true)
        }
        assertTrue("unexpected $log", log.none { it.contains("boost") })
        assertEquals(2, p.downloads)
    }

    @Test
    fun `the row reads the seconds left at the pace the buffer is falling`() {
        val p = steady(2, aheadMs = 20_000L)
        p.play(8, 70_000, aheadPerSecondMs = -500, loading = true)
        // About 16 s ahead falling 0.5 s per second: some 30 s left.
        val left = p.runwayNowMs
        assertTrue("runway $left", left in 20_000L..45_000L)
    }

    @Test
    fun `a small buffer that cannot hold more is boosted from where it is`() {
        // 90 Mbit film on the 2 GB box: the buffer tops out near 25 s, low mark 10 s.
        val p = steady(2, aheadMs = 25_000L, targetMs = 25_000L)
        p.play(40, 70_000, aheadPerSecondMs = -400, targetMs = 25_000L)
        val boost = log.indexOfFirst { it.contains("boost runway") }
        assertTrue("no runway boost in $log", boost >= 0)
        assertTrue("boosted too late, buffer ${aheadAtLog[boost]}", aheadAtLog[boost] >= 12_000L)
    }

    @Test
    fun `falling again soon after the boost ended keeps one more, as a dip does`() {
        val p = steady(2)
        p.play(40, 70_000, aheadPerSecondMs = -800)
        assertTrue(p.downloads > 2)
        // Refill, then the hand-back one download a minute until the count from before is back.
        p.play(30, 300_000, aheadPerSecondMs = 2_000)
        p.play(62 * 6, 120_000, loading = false)
        assertEquals(2, p.downloads)
        log.clear()
        p.play(70, 70_000, aheadPerSecondMs = -800)
        assertTrue("no runway boost in $log", log.any { it.contains("boost runway") })
        p.play(20, 300_000, aheadPerSecondMs = 2_000)
        p.play(62 * 5, 120_000, loading = false)
        assertEquals(3, p.downloads)
    }

    @Test
    fun `the state tag says what the policy is doing`() {
        val p = steady(2)
        assertEquals("steady", p.stateTag(now))
        p.play(40, 70_000, aheadPerSecondMs = -800)
        assertTrue(p.stateTag(now), p.stateTag(now).startsWith("boost to"))
    }

    @Test
    fun `a store server's pieces run at least sixteen megabytes where the piece budget fits them`() {
        val mb = 1024L * 1024L
        val p = AutoSpeedupPolicy(8, pieceBytes = 8L * mb)
        p.pieceBudgetBytes = 64L * mb
        p.preferredPieceBytes = StoreHosts.STORE_PIECE_BYTES
        assertEquals(16L * mb, p.rememberedStartPiece(2))
        assertEquals(16L * mb, p.rememberedStartPiece(3))
        // Four downloads and the one in hand would need 80 MB: it keeps to what fits.
        assertTrue(p.rememberedStartPiece(4) < 16L * mb)
    }

    @Test
    fun `only TorBox store hosts count as store servers`() {
        assertTrue(StoreHosts.isStoreUrl("https://store-12.weur.tb-cdn.st/file?token=1"))
        assertTrue(StoreHosts.isStoreUrl("https://store.weur.tb-cdn.io/x"))
        assertTrue(!StoreHosts.isStoreUrl("https://example.com/file"))
        assertTrue(!StoreHosts.isStoreUrl(null))
        assertTrue(!StoreHosts.isStoreUrl("not a url"))
    }
}
