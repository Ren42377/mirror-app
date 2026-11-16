package com.ren42377.mirror.ui

import android.util.Size
import androidx.camera.core.CameraSelector
import com.ren42377.mirror.model.AppSettings
import com.ren42377.mirror.model.AspectRatioOption
import com.ren42377.mirror.model.VideoModeCapability
import com.ren42377.mirror.model.VideoQualityOption
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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
    val isAspectRatioBarVisible: Boolean = false,
    val exposureIndex: Int = 0,
    val minExposureIndex: Int = 0,
    val maxExposureIndex: Int = 0,
    val exposureStepEv: Float = 0f,
    val isExposureSliderVisible: Boolean = false,
    val isRingLightEnabled: Boolean = false,
    val isFaceAutoZoomEnabled: Boolean = false,
    val isFaceTrackingSupported: Boolean = true,
    val isFaceDetected: Boolean = false
) {
    val aspectRatio: AspectRatioOption
        get() = AspectRatioOption.fromKey(settings.aspectRatio)

    val isMirroredOnScreen: Boolean
        get() = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            !isFlipped
        } else {
            isFlipped
        }

    val hasExposureControl: Boolean
        get() = minExposureIndex < maxExposureIndex

    fun withExposureRange(
        min: Int,
        max: Int,
        current: Int,
        stepEv: Float = exposureStepEv
    ): MirrorUiState {
        val safeMin = min.coerceAtMost(max)
        val safeMax = max.coerceAtLeast(min)
        val supportsExposure = safeMin < safeMax
        val validStep = if (stepEv.isFinite() && stepEv > 0f) stepEv else 0f
        return copy(
            minExposureIndex = safeMin,
            maxExposureIndex = safeMax,
            exposureIndex = if (supportsExposure) current.coerceIn(safeMin, safeMax) else 0,
            exposureStepEv = if (supportsExposure) validStep else 0f,
            isExposureSliderVisible = if (supportsExposure) isExposureSliderVisible else false
        )
    }

    fun withExposureIndex(index: Int): MirrorUiState {
        if (!hasExposureControl) return this
        return copy(
            exposureIndex = index.coerceIn(minExposureIndex, maxExposureIndex)
        )
    }

    fun withToggledExposureSlider(): MirrorUiState {
        if (!hasExposureControl) return this
        val nextVisible = !isExposureSliderVisible
        return copy(
            isExposureSliderVisible = nextVisible,
            isAspectRatioBarVisible = if (nextVisible) false else isAspectRatioBarVisible
        )
    }

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

fun formatExposureEvLabel(index: Int, stepEv: Float): String {
    if (!stepEv.isFinite() || stepEv <= 0f) {
        val sign = if (index > 0) "+" else ""
        return "$sign$index EV"
    }
    val rawEv = index * stepEv
    val roundedTenths = (rawEv * 10f).roundToInt() / 10f
    val normalized = if (abs(roundedTenths) < 0.05f) 0f else roundedTenths
    val sign = if (normalized > 0f) "+" else ""
    return String.format(Locale.US, "%s%.1f EV", sign, normalized)
}
