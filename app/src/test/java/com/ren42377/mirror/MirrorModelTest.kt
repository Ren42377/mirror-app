package com.ren42377.mirror

import androidx.camera.core.CameraSelector
import com.ren42377.mirror.model.AppSettings
import com.ren42377.mirror.model.VideoQualityOption
import com.ren42377.mirror.ui.MirrorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MirrorModelTest {

    @Test
    fun frontCameraDefaultStateIsMirrored() {
        val state = MirrorUiState(
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            isFlipped = false
        )
        assertTrue(state.isMirroredOnScreen)
    }

    @Test
    fun frontCameraFlippedStateIsNotMirrored() {
        val state = MirrorUiState(
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            isFlipped = true
        )
        assertFalse(state.isMirroredOnScreen)
    }

    @Test
    fun backCameraDefaultStateIsNotMirrored() {
        val state = MirrorUiState(
            lensFacing = CameraSelector.LENS_FACING_BACK,
            isFlipped = false
        )
        assertFalse(state.isMirroredOnScreen)
    }

    @Test
    fun backCameraFlippedStateIsMirrored() {
        val state = MirrorUiState(
            lensFacing = CameraSelector.LENS_FACING_BACK,
            isFlipped = true
        )
        assertTrue(state.isMirroredOnScreen)
    }

    @Test
    fun videoQualitySelectionFallsBackToFirstSupported() {
        val state = MirrorUiState(
            supportedVideoQualities = listOf(VideoQualityOption.HD, VideoQualityOption.SD),
            settings = AppSettings(videoQuality = "UHD")
        )
        assertEquals(VideoQualityOption.HD, state.effectiveVideoQuality)
    }

    @Test
    fun blankPhotoResolutionReturnsNullSize() {
        val settings = AppSettings(photoResolution = "")
        assertNull(settings.parsePhotoSize())
    }

    @Test
    fun exposureControlEnabledWhenRangeIsValid() {
        val disabledState = MirrorUiState(minExposureIndex = 0, maxExposureIndex = 0)
        assertFalse(disabledState.hasExposureControl)

        val enabledState = MirrorUiState(minExposureIndex = -12, maxExposureIndex = 12)
        assertTrue(enabledState.hasExposureControl)
    }

    @Test
    fun ringLightAndFaceZoomDefaultToFalse() {
        val state = MirrorUiState()
        assertFalse(state.isRingLightEnabled)
        assertFalse(state.isFaceAutoZoomEnabled)
        assertNull(state.detectedFaceNormalizedBounds)
    }
}
