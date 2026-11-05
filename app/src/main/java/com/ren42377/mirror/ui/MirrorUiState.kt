package com.ren42377.mirror.ui

import android.util.Size
import androidx.camera.core.CameraSelector
import com.ren42377.mirror.model.AppSettings
import com.ren42377.mirror.model.VideoQualityOption

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
    val isSettingsOpen: Boolean = false
) {
    val isMirroredOnScreen: Boolean
        get() = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            !isFlipped
        } else {
            isFlipped
        }

    val effectivePhotoSize: Size?
        get() {
            if (supportedPhotoSizes.isEmpty()) return null
            val configured = settings.parsePhotoSize()
            if (configured != null && supportedPhotoSizes.contains(configured)) {
                return configured
            }
            return supportedPhotoSizes.first()
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
