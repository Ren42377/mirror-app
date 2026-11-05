package com.ren42377.mirror

import com.ren42377.mirror.camera.FpsRange
import com.ren42377.mirror.camera.FrameRatePlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameRatePlannerTest {

    private val typicalRanges = listOf(
        FpsRange(15, 30),
        FpsRange(30, 30),
        FpsRange(15, 60),
        FpsRange(60, 60)
    )

    @Test
    fun offersOnlyRatesTheCameraAdvertises() {
        val rates = FrameRatePlanner.supportedFrameRates(typicalRanges, null, null)
        assertEquals(listOf(30, 60), rates)
    }

    @Test
    fun streamLimitRemovesHigherRates() {
        val rates = FrameRatePlanner.supportedFrameRates(typicalRanges, 30, null)
        assertEquals(listOf(30), rates)
    }

    @Test
    fun encoderLimitRemovesHigherRates() {
        val rates = FrameRatePlanner.supportedFrameRates(typicalRanges, 60, 30)
        assertEquals(listOf(30), rates)
    }

    @Test
    fun emptyRangesFallBackToDefaultRate() {
        assertEquals(listOf(30), FrameRatePlanner.supportedFrameRates(emptyList(), null, null))
    }

    @Test
    fun aeRangeForThirtyPrefersLowLightFriendlyRange() {
        assertEquals(FpsRange(15, 30), FrameRatePlanner.selectAeRange(typicalRanges, 30))
    }

    @Test
    fun aeRangeForSixtyAvoidsDroppingBelowThirty() {
        assertEquals(FpsRange(60, 60), FrameRatePlanner.selectAeRange(typicalRanges, 60))
    }

    @Test
    fun aeRangeIsNullWhenRateIsMissing() {
        assertNull(FrameRatePlanner.selectAeRange(typicalRanges, 24))
    }

    @Test
    fun idleRangeFallsBackWhenThirtyIsMissing() {
        val ranges = listOf(FpsRange(10, 24), FpsRange(24, 24))
        assertEquals(FpsRange(10, 24), FrameRatePlanner.selectIdleRange(ranges))
    }

    @Test
    fun frameDurationConvertsToRate() {
        assertEquals(30, FrameRatePlanner.frameRateFromDuration(33_333_333L))
        assertEquals(60, FrameRatePlanner.frameRateFromDuration(16_666_666L))
        assertNull(FrameRatePlanner.frameRateFromDuration(0L))
    }
}
