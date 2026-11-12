package com.nuvio.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreHostsTest {
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
