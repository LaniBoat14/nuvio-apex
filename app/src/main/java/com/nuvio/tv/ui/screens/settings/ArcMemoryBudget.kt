package com.nuvio.tv.ui.screens.settings

import android.content.Context
import androidx.media3.exoplayer.upstream.DefaultAllocatorNative
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.core.player.BufferSizing
import com.nuvio.tv.ui.screens.player.NuvioExoPlayerPerformanceHelper

/**
 * Arc network-engine additions to the memory budget. Kept apart from [MemoryBudget] so stock's
 * low-RAM tiering and init() stay untouched.
 */
object ArcMemoryBudget {
    private const val NATIVE_PROBE_BYTES = 4096

    /** Whether the engine's native allocator loads (chunks and buffers then live off-heap). */
    val offHeapBuffers: Boolean by lazy {
        runCatching {
            DefaultAllocatorNative.createAllocation(NATIVE_PROBE_BYTES)
                ?.also(DefaultAllocatorNative::freeAllocation) != null
        }.getOrDefault(false)
    }

    /** The budget the managed buffer uses: the box's automatic size, within stock's budget. */
    val autoBudgetMb: Int by lazy {
        if (offHeapBuffers) {
            BufferSizing.autoBufferMbForRam(NuvioExoPlayerPerformanceHelper.getRamFromMemInfo())
                .coerceAtMost(MemoryBudget.budgetMb)
        } else {
            MemoryBudget.budgetMb
        }
    }

    /**
     * How many chunks playback keeps in flight from the reader: connections + 1, or the
     * [MemoryBudget.prefetchDepthChunks] window in performance mode.
     */
    data class DownloadWindow(
        val performanceMode: Boolean,
        val safeNativeLimitMb: Int,
        val reserveBufferMb: Int,
    ) {
        fun prefetchDepth(connections: Int, chunkSizeMb: Int): Int =
            if (performanceMode) {
                MemoryBudget.prefetchDepthChunks(connections, chunkSizeMb, safeNativeLimitMb, reserveBufferMb)
            } else {
                connections + 1
            }
    }

    /** The chunk sizes from 8 MB up that the Chunk size setting offers. */
    val CHUNK_STEPS_MB = listOf(8, 12, 16, 24, 32, 48, 64, 96, 128)

    private const val LOW_RAM_MAX_CHUNK_MB = 16
    private const val HEAP_CHUNK_SHARE = 4
    private const val LOW_RAM_CONVERSION_RATIO = 0.95f
    private const val HIGH_RAM_CONVERSION_RATIO = 0.75f

    /** The largest offered chunk size at or below [mb], never below [MemoryBudget.MIN_CHUNK_MB]. */
    fun chunkStepAtOrBelow(mb: Int): Int = CHUNK_STEPS_MB.lastOrNull { it <= mb } ?: MemoryBudget.MIN_CHUNK_MB

    fun conversionBudgetMb(budgetMb: Int): Int =
        (budgetMb * (if (MemoryBudget.isLowRamTier) LOW_RAM_CONVERSION_RATIO else HIGH_RAM_CONVERSION_RATIO)).toInt()
            .coerceAtMost(budgetMb).coerceAtLeast(MemoryBudget.MIN_BUFFER_MB)

    /** Hard chunk-size ceiling for this device; binds everywhere, including performance mode. */
    fun tierMaxChunkMb(nativeMemory: Boolean, connections: Int): Int =
        tierMaxChunkMbFor(
            MemoryBudget.isLowRamTier,
            offHeapBuffers,
            NuvioExoPlayerPerformanceHelper.getRamFromMemInfo(),
            MemoryBudget.heapMb,
            nativeMemory,
            connections,
        )

    /**
     * Under 2.3 GB of RAM, or with chunks on the heap, a small heap keeps 16 MB; above it the ceiling follows RAM with
     * native memory and a quarter of the heap per connection without it.
     */
    internal fun tierMaxChunkMbFor(
        lowHeap: Boolean,
        offHeap: Boolean,
        deviceRamBytes: Long,
        maxHeapMb: Long,
        nativeMemory: Boolean,
        connections: Int,
    ): Int {
        if (!lowHeap) return MemoryBudget.MAX_CHUNK_MB
        if (BufferSizing.isSmallBox(deviceRamBytes) || !offHeap) return LOW_RAM_MAX_CHUNK_MB
        if (offHeap && nativeMemory) return MemoryBudget.MAX_CHUNK_MB
        val heapShareMb = (maxHeapMb / HEAP_CHUNK_SHARE).toInt()
        return chunkStepAtOrBelow(heapShareMb / connections.coerceAtLeast(1)).coerceAtLeast(LOW_RAM_MAX_CHUNK_MB)
    }

    /** Parallel overhead for the DISPLAYED memory-usage estimate and the buffer sizing. */
    fun displayParallelOverheadMb(
        connectionCount: Int,
        chunkSizeMb: Int,
        safeNativeLimitMb: Int,
        reserveBufferMb: Int,
        deepPathActive: Boolean,
    ): Int {
        val depth = DownloadWindow(deepPathActive, safeNativeLimitMb, reserveBufferMb)
            .prefetchDepth(connectionCount, chunkSizeMb)
        val sessionHeadroom = if (MemoryBudget.isLowRamTier) 2 else 4
        return (depth + sessionHeadroom) * chunkSizeMb
    }

    /**
     * With native memory: the largest offered chunk size whose download window stays within [limitMb].
     * Never above [tierMaxMb], never below [MemoryBudget.MIN_CHUNK_MB].
     */
    fun nativeMaxChunkMb(
        connections: Int,
        limitMb: Int,
        safeNativeLimitMb: Int,
        reserveBufferMb: Int,
        tierMaxMb: Int = tierMaxChunkMb(nativeMemory = true, connections),
    ): Int = CHUNK_STEPS_MB.filter { it <= tierMaxMb }.lastOrNull {
        displayParallelOverheadMb(connections, it, safeNativeLimitMb, reserveBufferMb, deepPathActive = true) <= limitMb
    } ?: MemoryBudget.MIN_CHUNK_MB

    /**
     * The largest chunk size Custom network uses with [settings]. With native memory the download window stays within
     * the box's warning limit; without it the chunks fit the budget beside the buffer. A larger saved size is kept and
     * used again once it fits.
     */
    fun customMaxChunkMb(
        settings: PlayerSettings,
        autoBufferMb: Int,
        safeNativeLimitMb: Int,
        warningLimitMb: Int,
        tierMaxMb: Int = tierMaxChunkMb(settings.nuvioPerformanceModeEnabled, settings.parallelConnectionCount),
    ): Int {
        if (!settings.nuvioPerformanceModeEnabled) {
            val bufferMb = MemoryBudget.effectiveBufferMb(settings.bufferSettings.targetBufferSizeMb)
            return chunkStepAtOrBelow(
                ((MemoryBudget.budgetMb - bufferMb) / MemoryBudget.bufferCount(settings.parallelConnectionCount))
                    .coerceIn(MemoryBudget.MIN_CHUNK_MB, tierMaxMb)
            )
        }
        val userTarget = settings.bufferEngineEnabled && !settings.bufferBudgetManaged
        if (!userTarget) {
            return nativeMaxChunkMb(settings.parallelConnectionCount, warningLimitMb, autoBufferMb, autoBufferMb, tierMaxMb)
        }
        val storedMb = settings.bufferSettings.targetBufferSizeMb
        val targetMb = if (!settings.allowLargeTargetBuffer && storedMb > safeNativeLimitMb) safeNativeLimitMb else storedMb
        return nativeMaxChunkMb(settings.parallelConnectionCount, warningLimitMb, safeNativeLimitMb, targetMb, tierMaxMb)
    }

    fun customMaxChunkMb(context: Context, settings: PlayerSettings): Int = customMaxChunkMb(
        settings,
        NuvioExoPlayerPerformanceHelper.getAutoBufferMb(context),
        NuvioExoPlayerPerformanceHelper.getSafeNativeMemoryLimitMb(context),
        NuvioExoPlayerPerformanceHelper.getWarningNativeMemoryLimitMb(context),
    )

    /** The chunk size playback uses: the saved one, no larger than [customMaxChunkMb]. */
    fun customChunkKb(context: Context, settings: PlayerSettings): Int =
        settings.parallelChunkSizeKb.coerceAtMost(customMaxChunkMb(context, settings) * 1024)

    /** Whether the box shows a readable video memory pool (CmaTotal at least 64 MB). */
    fun videoPoolReadable(): Boolean = runCatching {
        var free = -1L
        var total = -1L
        java.io.File("/proc/meminfo").forEachLine { line ->
            val kb = line.substringAfter(':', "").trim().substringBefore(' ').toLongOrNull() ?: return@forEachLine
            if (line.startsWith("CmaFree:")) free = kb else if (line.startsWith("CmaTotal:")) total = kb
        }
        free >= 0 && total >= 64L * 1024
    }.getOrDefault(false)
}
