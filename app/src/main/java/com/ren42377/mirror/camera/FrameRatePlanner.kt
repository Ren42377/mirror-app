package com.ren42377.mirror.camera

import kotlin.math.roundToInt

/** Plain frame rate range, independent from android.util.Range so it can be unit tested. */
data class FpsRange(val lower: Int, val upper: Int)

/**
 * Pure helpers that decide which frame rates a camera can offer and which
 * auto exposure target range must be requested for a chosen frame rate.
 */
object FrameRatePlanner {

    const val DEFAULT_FRAME_RATE = 30

    private val STANDARD_FRAME_RATES = listOf(24, 25, 30, 50, 60)

    /**
     * Returns the standard frame rates the device can sustain.
     *
     * A rate is offered only when the camera advertises an auto exposure range ending at
     * that rate and neither the camera stream nor the video encoder limits it lower.
     * Unknown limits (null) are ignored.
     */
    fun supportedFrameRates(
        aeRanges: List<FpsRange>,
        maxStreamFps: Int?,
        maxEncoderFps: Int?
    ): List<Int> {
        val limit = listOfNotNull(maxStreamFps, maxEncoderFps).minOrNull() ?: Int.MAX_VALUE
        val rates = STANDARD_FRAME_RATES.filter { rate ->
            rate <= limit && aeRanges.any { it.upper == rate && it.lower <= rate }
        }
        return rates.ifEmpty { listOf(DEFAULT_FRAME_RATE) }
    }

    /** Picks the camera range that ends at [fps]; low light friendly (lowest lower bound) when possible. */
    fun selectAeRange(ranges: List<FpsRange>, fps: Int): FpsRange? {
        val candidates = ranges.filter { it.upper == fps && it.lower <= fps }
        if (candidates.isEmpty()) return null
        if (fps >= 50) {
            val smooth = candidates.filter { it.lower >= 30 }.minByOrNull { it.lower }
            if (smooth != null) return smooth
        }
        return candidates.minByOrNull { it.lower }
    }

    /** Range used while the camera is idle or taking photos. */
    fun selectIdleRange(ranges: List<FpsRange>): FpsRange? {
        if (ranges.isEmpty()) return null
        selectAeRange(ranges, DEFAULT_FRAME_RATE)?.let { return it }
        val atLeast24 = ranges.filter { it.upper >= 24 }
        if (atLeast24.isNotEmpty()) {
            return atLeast24.minWithOrNull(compareBy<FpsRange> { it.lower }.thenBy { it.upper })
        }
        return ranges.minByOrNull { it.lower }
    }

    /** Converts a minimum frame duration in nanoseconds into a whole frame rate. */
    fun frameRateFromDuration(durationNanos: Long): Int? {
        if (durationNanos <= 0L) return null
        return (1_000_000_000.0 / durationNanos.toDouble()).roundToInt()
    }
}
