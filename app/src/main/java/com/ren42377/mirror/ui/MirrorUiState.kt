package com.ren42377.mirror.ui

import android.graphics.RectF
import android.util.Size
import androidx.camera.core.CameraSelector
import com.ren42377.mirror.model.AppSettings
import com.ren42377.mirror.model.VideoQualityOption
import kotlin.math.abs

data class MirrorUiState(
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val isFlipped: Boolean = false,
    val hasMultipleCameras: Boolean = true,
    val supportedPhotoSizes: List<Size> = emptyList(),
    val supportedVideoQualities: List<VideoQualityOption> = listOf(
        VideoQualityOption.FHD,
        VideoQualityOption.HD,
        VideoQualityOption.SD
    ),
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
    val detectedFaceNormalizedBounds: RectF? = null
) {
    val isMirroredOnScreen: Boolean
        get() = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            !isFlipped
        } else {
            isFlipped
        }

    val hasExposureControl: Boolean
        get() = minExposureIndex < maxExposureIndex

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

    val effectiveVideoQuality: VideoQualityOption
        get() {
            val preferred = VideoQualityOption.fromKey(settings.videoQuality)
            if (supportedVideoQualities.contains(preferred)) {
                return preferred
            }
            return supportedVideoQualities.firstOrNull() ?: VideoQualityOption.FHD
        }
}
