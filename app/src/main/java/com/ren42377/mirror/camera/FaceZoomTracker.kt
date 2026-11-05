package com.ren42377.mirror.camera

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A sensor crop window (active array coordinates) that the camera should output.
 * [faceSensorX] and [faceSensorY] are only valid when [faceVisible] is true.
 */
data class CropWindow(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val zoom: Float,
    val faceVisible: Boolean,
    val faceSensorX: Float,
    val faceSensorY: Float
)

/** Normalized point in display orientation, (0,0) is the top left corner. */
data class NormalizedPoint(val x: Float, val y: Float)

/**
 * Turns face detections into a smooth zoom and pan of the real camera crop region.
 *
 * Everything is expressed in sensor active array coordinates, so no screen
 * orientation or mirroring math is needed. Face detections are fed from the camera
 * thread through [onFace], while the UI side calls [step] at a steady rate.
 */
class FaceZoomTracker {

    private var arrayLeft = 0
    private var arrayTop = 0
    private var arrayWidth = 0
    private var arrayHeight = 0
    private var maxZoom = 1f
    private var canPan = true
    private var configured = false

    private var zoom = 1f
    private var centerX = 0f
    private var centerY = 0f
    private var targetZoom = 1f
    private var targetCenterX = 0f
    private var targetCenterY = 0f
    private var hasTarget = false
    private var userZoomOverride: Float? = null

    private var faceX = 0f
    private var faceY = 0f
    private var lastFaceTimeMs = NO_FACE
    private var lastStepMs = 0L

    @Synchronized
    fun configure(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        maxZoom: Float,
        canPan: Boolean = true
    ) {
        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) {
            configured = false
            return
        }
        val safeMaxZoom = maxZoom.coerceIn(1f, MAX_FACE_ZOOM)
        val unchanged = configured &&
            left == arrayLeft && top == arrayTop &&
            width == arrayWidth && height == arrayHeight &&
            safeMaxZoom == this.maxZoom && canPan == this.canPan
        if (unchanged) return
        arrayLeft = left
        arrayTop = top
        arrayWidth = width
        arrayHeight = height
        this.maxZoom = safeMaxZoom
        this.canPan = canPan
        configured = true
        resetLocked()
    }

    @Synchronized
    fun reset() {
        resetLocked()
    }

    /**
     * Adjusts and locks the zoom factor in response to a user pinch gesture while
     * keeping face panning active.
     */
    @Synchronized
    fun onUserScale(scaleFactor: Float): Float {
        if (!configured) return 1f
        val baseZoom = userZoomOverride ?: zoom
        val updated = (baseZoom * scaleFactor).coerceIn(1f, maxZoom)
        userZoomOverride = updated
        targetZoom = updated
        zoom = updated
        return updated
    }

    /**
     * Registers a detected face.
     *
     * @param centerX face center x in sensor coordinates
     * @param centerY face center y in sensor coordinates
     * @param faceSize approximate face edge length in sensor pixels
     */
    @Synchronized
    fun onFace(centerX: Float, centerY: Float, faceSize: Float, nowMs: Long) {
        if (!configured) return
        lastFaceTimeMs = nowMs
        faceX = centerX
        faceY = centerY

        val shortSide = min(arrayWidth, arrayHeight).toFloat()
        val manualZoom = userZoomOverride
        val desiredZoom = (manualZoom ?: (TARGET_FACE_FRACTION * shortSide / max(faceSize, 1f)))
            .coerceIn(1f, maxZoom)

        if (!hasTarget) {
            targetZoom = desiredZoom
            targetCenterX = centerX
            targetCenterY = centerY
            hasTarget = true
            return
        }
        if (manualZoom != null) {
            targetZoom = manualZoom
        } else if (abs(desiredZoom - targetZoom) / targetZoom > ZOOM_DEAD_BAND) {
            targetZoom = desiredZoom
        }
        val windowWidth = arrayWidth / zoom
        val windowHeight = arrayHeight / zoom
        if (abs(centerX - targetCenterX) > PAN_DEAD_BAND * windowWidth ||
            abs(centerY - targetCenterY) > PAN_DEAD_BAND * windowHeight
        ) {
            targetCenterX = centerX
            targetCenterY = centerY
        }
    }

    /** Advances the smoothing and returns the crop window to apply, or null before configuration. */
    @Synchronized
    fun step(nowMs: Long): CropWindow? {
        if (!configured) return null

        val manualZoom = userZoomOverride
        val faceVisible = lastFaceTimeMs != NO_FACE && nowMs - lastFaceTimeMs <= FACE_LOST_TIMEOUT_MS
        if (!faceVisible) {
            hasTarget = false
            targetZoom = manualZoom ?: 1f
            targetCenterX = arrayCenterX()
            targetCenterY = arrayCenterY()
        }

        val dt = if (lastStepMs == 0L) {
            DEFAULT_STEP_SECONDS
        } else {
            (nowMs - lastStepMs).coerceIn(1L, 100L) / 1000f
        }
        lastStepMs = nowMs

        if (manualZoom != null) {
            targetZoom = manualZoom
            zoom = manualZoom.coerceIn(1f, maxZoom)
        } else {
            val zoomTau = if (targetZoom > zoom) ZOOM_IN_TAU else ZOOM_OUT_TAU
            zoom += (targetZoom - zoom) * (1f - exp(-dt / zoomTau))
            if (!faceVisible && abs(zoom - 1f) < 0.003f) zoom = 1f
            zoom = zoom.coerceIn(1f, maxZoom)
        }

        val panAlpha = 1f - exp(-dt / PAN_TAU)
        centerX += (targetCenterX - centerX) * panAlpha
        centerY += (targetCenterY - centerY) * panAlpha

        val width = (arrayWidth / zoom).roundToInt().coerceIn(1, arrayWidth)
        val height = (arrayHeight / zoom).roundToInt().coerceIn(1, arrayHeight)
        val windowCenterX = if (canPan) centerX else arrayCenterX()
        val windowCenterY = if (canPan) centerY else arrayCenterY()
        val left = (windowCenterX - width / 2f).roundToInt()
            .coerceIn(arrayLeft, arrayLeft + arrayWidth - width)
        val top = (windowCenterY - height / 2f).roundToInt()
            .coerceIn(arrayTop, arrayTop + arrayHeight - height)

        return CropWindow(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
            zoom = zoom,
            faceVisible = faceVisible,
            faceSensorX = faceX,
            faceSensorY = faceY
        )
    }

    private fun arrayCenterX(): Float = arrayLeft + arrayWidth / 2f

    private fun arrayCenterY(): Float = arrayTop + arrayHeight / 2f

    private fun resetLocked() {
        zoom = 1f
        targetZoom = 1f
        centerX = arrayCenterX()
        centerY = arrayCenterY()
        targetCenterX = centerX
        targetCenterY = centerY
        hasTarget = false
        userZoomOverride = null
        lastFaceTimeMs = NO_FACE
        lastStepMs = 0L
    }

    companion object {
        /** Share of the visible short side that the face should fill. */
        const val TARGET_FACE_FRACTION = 0.72f
        const val MAX_FACE_ZOOM = 5f

        private const val NO_FACE = Long.MIN_VALUE
        private const val FACE_LOST_TIMEOUT_MS = 1500L
        private const val ZOOM_DEAD_BAND = 0.07f
        private const val PAN_DEAD_BAND = 0.03f
        private const val ZOOM_IN_TAU = 0.12f
        private const val ZOOM_OUT_TAU = 0.30f
        private const val PAN_TAU = 0.08f
        private const val DEFAULT_STEP_SECONDS = 0.033f

        /** Approximate face edge length from a face rectangle. */
        fun faceSizeOf(width: Int, height: Int): Float {
            return sqrt(width.toFloat() * height.toFloat())
        }
    }
}

/**
 * Converts a point in sensor active array coordinates to a normalized point in the
 * upright, unmirrored display orientation (the space used by
 * SurfaceOrientedMeteringPointFactory).
 */
fun sensorPointToDisplayNormalized(
    x: Float,
    y: Float,
    arrayLeft: Int,
    arrayTop: Int,
    arrayWidth: Int,
    arrayHeight: Int,
    sensorOrientation: Int
): NormalizedPoint {
    val sx = ((x - arrayLeft) / arrayWidth.toFloat()).coerceIn(0f, 1f)
    val sy = ((y - arrayTop) / arrayHeight.toFloat()).coerceIn(0f, 1f)
    return when (((sensorOrientation % 360) + 360) % 360) {
        90 -> NormalizedPoint(1f - sy, sx)
        180 -> NormalizedPoint(1f - sx, 1f - sy)
        270 -> NormalizedPoint(sy, 1f - sx)
        else -> NormalizedPoint(sx, sy)
    }
}
