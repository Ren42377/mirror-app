package com.ren42377.mirror.ui

import android.util.Size
import androidx.camera.core.CameraSelector
import com.ren42377.mirror.model.AppSettings
import com.ren42377.mirror.model.VideoModeCapability
import com.ren42377.mirror.model.VideoQualityOption
import kotlin.math.abs

data class MirrorUiState(
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val isFlipped: Boolean = false,
    val hasMultipleCameras: Boolean = true,
    val supportedPhotoSizes: List<Size> = emptyList(),
    val highResolutionPhotoSizes: Set<Size> = emptySet(),
    val videoCapabilities: List<VideoModeCapability> = emptyList(),
    val isVideoStabilizationSupported: Boolean = false,
    val isOpticalStabilizationSupported: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val isRecording: Boolean = false,
    val recordingSeconds: Long = 0L,
    val isCapturingPhoto: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val exposureIndex: Int = 0,
    val minExposureIndex: Int = 0,
    val maxExposureIndex: Int = 0,
    val isExposureSliderVisible: Boolean = false,
    val isRingLightEnabled: Boolean = false,
    val isFaceAutoZoomEnabled: Boolean = false,
    val isFaceTrackingSupported: Boolean = true,
    val isFaceDetected: Boolean = false
) {
    val isMirroredOnScreen: Boolean
        get() = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            !isFlipped
        } else {
            isFlipped
        }

    val hasExposureControl: Boolean
        get() = minExposureIndex < maxExposureIndex

    val supportedVideoQualities: List<VideoQualityOption>
        get() = videoCapabilities.map { it.quality }

    val effectivePhotoSize: Size?
        get() {
            if (supportedPhotoSizes.isEmpty()) return null
            val configured = settings.parsePhotoSize()
            if (configured != null && supportedPhotoSizes.contains(configured)) {
                return configured
            }
            val preferredBinnedFourByThree = supportedPhotoSizes.firstOrNull { size ->
                val area = size.width.toLong() * size.height.toLong()
                val ratio = size.width.toDouble() / size.height.toDouble()
                val isFourByThree = abs(ratio - (4.0 / 3.0)) < 0.06 || abs(ratio - (3.0 / 4.0)) < 0.06
                isFourByThree && area <= 13_000_000L
            }
            val preferredBinnedAny = supportedPhotoSizes.firstOrNull { size ->
                val area = size.width.toLong() * size.height.toLong()
                area <= 13_000_000L
            }
            return preferredBinnedFourByThree ?: preferredBinnedAny ?: supportedPhotoSizes.first()
        }

    /**
     * The saved quality when the camera supports it, otherwise the closest lower supported
     * quality, otherwise the best supported one.
     */
    val effectiveVideoQuality: VideoQualityOption
        get() {
            val preferred = VideoQualityOption.fromKey(settings.videoQuality)
            val supported = supportedVideoQualities
            if (supported.isEmpty() || supported.contains(preferred)) {
                return preferred
            }
            return supported.filter { it.ordinal >= preferred.ordinal }.minByOrNull { it.ordinal }
                ?: supported.minByOrNull { it.ordinal }
                ?: preferred
        }

    val currentVideoCapability: VideoModeCapability?
        get() = videoCapabilities.firstOrNull { it.quality == effectiveVideoQuality }

    val supportedFrameRates: List<Int>
        get() = currentVideoCapability?.frameRates ?: listOf(30)

    /** The saved frame rate when valid for the current quality, otherwise the closest lower one. */
    val effectiveFrameRate: Int
        get() {
            val rates = supportedFrameRates
            val preferred = settings.videoFrameRate
            if (rates.contains(preferred)) return preferred
            return rates.filter { it <= preferred }.maxOrNull() ?: rates.min()
        }

    val effectiveVideoStabilization: Boolean
        get() = settings.videoStabilization && isVideoStabilizationSupported

    val effectiveOpticalStabilization: Boolean
        get() = settings.opticalStabilization && isOpticalStabilizationSupported
}
