package com.ren42377.mirror.camera

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.video.QualitySelector
import com.ren42377.mirror.model.VideoModeCapability
import com.ren42377.mirror.model.VideoQualityOption
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Everything the app needs to know about what the active camera can really do. */
data class CameraCapabilities(
    val photoSizes: List<Size>,
    val highResolutionPhotoSizes: Set<Size>,
    val videoModes: List<VideoModeCapability>,
    val aeFpsRanges: List<FpsRange>,
    val isVideoStabilizationSupported: Boolean,
    val isOpticalStabilizationSupported: Boolean,
    val activeArray: Rect?,
    val sensorOrientation: Int,
    val maxDigitalZoom: Float,
    val faceDetectMode: Int,
    val isFreeformCropSupported: Boolean
) {
    val isFaceTrackingSupported: Boolean
        get() = activeArray != null &&
            faceDetectMode != CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF

    companion object {
        val EMPTY = CameraCapabilities(
            photoSizes = emptyList(),
            highResolutionPhotoSizes = emptySet(),
            videoModes = emptyList(),
            aeFpsRanges = emptyList(),
            isVideoStabilizationSupported = false,
            isOpticalStabilizationSupported = false,
            activeArray = null,
            sensorOrientation = 90,
            maxDigitalZoom = 1f,
            faceDetectMode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF,
            isFreeformCropSupported = false
        )
    }
}

private const val MIN_PHOTO_PIXELS = 640L * 480L
private const val MAX_PHOTO_GRID_OPTIONS = 6

/**
 * Curates raw camera JPEG sizes into a concise list of distinct megapixel options suitable
 * for a multi-column grid, preferring 4:3 sizes that match the viewfinder aspect ratio.
 */
fun curatePhotoSizes(
    rawSizes: List<Size>,
    maxCount: Int = MAX_PHOTO_GRID_OPTIONS
): List<Size> {
    val validSizes = rawSizes
        .filter { it.width.toLong() * it.height.toLong() >= MIN_PHOTO_PIXELS }
        .distinct()
        .sortedByDescending { it.width.toLong() * it.height.toLong() }

    if (validSizes.isEmpty()) return emptyList()

    val fourByThreeSizes = validSizes.filter { size ->
        val longSide = max(size.width, size.height).toDouble()
        val shortSide = min(size.width, size.height).toDouble().coerceAtLeast(1.0)
        abs((longSide / shortSide) - (4.0 / 3.0)) < 0.06
    }

    val candidatePool = if (fourByThreeSizes.isNotEmpty()) fourByThreeSizes else validSizes

    val distinctByMegapixel = candidatePool
        .groupBy { size ->
            ((size.width.toLong() * size.height.toLong()) / 1_000_000.0)
                .roundToInt()
                .coerceAtLeast(1)
        }
        .values
        .map { group -> group.first() }
        .sortedByDescending { it.width.toLong() * it.height.toLong() }

    if (distinctByMegapixel.size >= 3 || distinctByMegapixel.size == candidatePool.size) {
        return distinctByMegapixel.take(maxCount)
    }

    val combined = (distinctByMegapixel + candidatePool)
        .distinct()
        .sortedByDescending { it.width.toLong() * it.height.toLong() }
    return combined.take(maxCount)
}

/** Reads the real capabilities of a camera from Camera2 characteristics and CameraX. */
@OptIn(ExperimentalCamera2Interop::class)
fun queryCameraCapabilities(cameraInfo: CameraInfo): CameraCapabilities {
    val camera2Info = runCatching { Camera2CameraInfo.from(cameraInfo) }.getOrNull()

    fun <T> characteristic(key: CameraCharacteristics.Key<T>): T? {
        return camera2Info?.let { info ->
            runCatching { info.getCameraCharacteristic(key) }.getOrNull()
        }
    }

    val streamMap = characteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    val aeRanges = characteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        ?.map { FpsRange(it.lower, it.upper) }
        .orEmpty()

    val normalSizes = runCatching {
        streamMap?.getOutputSizes(ImageFormat.JPEG)?.toList()
    }.getOrNull().orEmpty()
    val highResSizes = runCatching {
        streamMap?.getHighResolutionOutputSizes(ImageFormat.JPEG)?.toList()
    }.getOrNull().orEmpty()
    val photoSizes = curatePhotoSizes(normalSizes + highResSizes)

    val videoStabilizationModes =
        characteristic(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
    val opticalStabilizationModes =
        characteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
    val faceModes = characteristic(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)

    return CameraCapabilities(
        photoSizes = photoSizes,
        highResolutionPhotoSizes = highResSizes.toSet(),
        videoModes = queryVideoModes(cameraInfo, streamMap, aeRanges),
        aeFpsRanges = aeRanges,
        isVideoStabilizationSupported = videoStabilizationModes
            ?.contains(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true,
        isOpticalStabilizationSupported = opticalStabilizationModes
            ?.contains(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON) == true,
        activeArray = characteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE),
        sensorOrientation = characteristic(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90,
        maxDigitalZoom = characteristic(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f,
        faceDetectMode = faceModes?.maxOrNull() ?: CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF,
        isFreeformCropSupported = characteristic(CameraCharacteristics.SCALER_CROPPING_TYPE) ==
            CameraCharacteristics.SCALER_CROPPING_TYPE_FREEFORM
    )
}

private fun queryVideoModes(
    cameraInfo: CameraInfo,
    streamMap: StreamConfigurationMap?,
    aeRanges: List<FpsRange>
): List<VideoModeCapability> {
    val supportedQualities = runCatching {
        QualitySelector.getSupportedQualities(cameraInfo)
    }.getOrNull().orEmpty()

    return supportedQualities
        .mapNotNull { quality ->
            val option = VideoQualityOption.fromQuality(quality) ?: return@mapNotNull null
            val resolution = runCatching { QualitySelector.getResolution(cameraInfo, quality) }
                .getOrNull()
            val streamFps = resolution?.let { maxStreamFrameRate(streamMap, it) }
            val encoderFps = resolution?.let { maxEncoderFrameRate(it) }
            VideoModeCapability(
                quality = option,
                resolution = resolution,
                frameRates = FrameRatePlanner.supportedFrameRates(aeRanges, streamFps, encoderFps)
            )
        }
        .distinctBy { it.quality }
        .sortedBy { it.quality.ordinal }
}

private fun maxStreamFrameRate(streamMap: StreamConfigurationMap?, size: Size): Int? {
    if (streamMap == null) return null
    val durationNanos = runCatching {
        streamMap.getOutputMinFrameDuration(MediaRecorder::class.java, size)
    }.getOrNull()?.takeIf { it > 0L }
        ?: runCatching {
            streamMap.getOutputMinFrameDuration(SurfaceTexture::class.java, size)
        }.getOrNull()?.takeIf { it > 0L }
        ?: return null
    return FrameRatePlanner.frameRateFromDuration(durationNanos)
}

private fun maxEncoderFrameRate(size: Size): Int? {
    return runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .filter { codec ->
                codec.isEncoder && codec.supportedTypes.any {
                    it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)
                }
            }
            .mapNotNull { codec ->
                runCatching {
                    codec.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                        .videoCapabilities
                        .getSupportedFrameRatesFor(size.width, size.height)
                        .upper
                        .roundToInt()
                }.getOrNull()
            }
            .maxOrNull()
    }.getOrNull()
}
