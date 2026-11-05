package com.ren42377.mirror.camera

import android.graphics.Rect
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera

/**
 * Owns the Camera2 capture request overrides of the active camera.
 *
 * CameraX replaces the whole override set on every call, so every option is kept here and
 * written together by [apply].
 */
@OptIn(ExperimentalCamera2Interop::class)
class CameraOptionsController {

    private var camera: Camera? = null

    var fpsRange: FpsRange? = null
    var opticalStabilizationSupported: Boolean = false
    var opticalStabilization: Boolean = false
    var faceDetectMode: Int = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF
    var cropRegion: Rect? = null

    fun attach(camera: Camera) {
        this.camera = camera
        apply()
    }

    fun apply() {
        val activeCamera = camera ?: return
        val builder = CaptureRequestOptions.Builder()

        fpsRange?.let { range ->
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(range.lower, range.upper)
            )
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )
        }

        if (opticalStabilizationSupported) {
            builder.setCaptureRequestOption(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                if (opticalStabilization) {
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                } else {
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                }
            )
        }

        if (faceDetectMode != CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF) {
            builder.setCaptureRequestOption(
                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                faceDetectMode
            )
        }

        cropRegion?.let { region ->
            builder.setCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, region)
        }

        runCatching {
            Camera2CameraControl.from(activeCamera.cameraControl)
                .setCaptureRequestOptions(builder.build())
        }
    }
}
