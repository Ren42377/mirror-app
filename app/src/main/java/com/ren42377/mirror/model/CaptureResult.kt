package com.ren42377.mirror.model

import android.graphics.Bitmap
import java.io.File

data class FaceZoomSample(
    val timeMs: Long,
    val zoom: Float,
    val normX: Float,
    val normY: Float
)

sealed interface CaptureResult {
    data class Photo(
        val bitmap: Bitmap,
        val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3
    ) : CaptureResult

    data class Video(
        val file: File,
        val mirrored: Boolean,
        val zoomSamples: List<FaceZoomSample> = emptyList(),
        val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3
    ) : CaptureResult
}
