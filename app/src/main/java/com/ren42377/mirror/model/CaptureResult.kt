package com.ren42377.mirror.model

import android.graphics.Bitmap
import java.io.File

sealed interface CaptureResult {
    data class Photo(val bitmap: Bitmap) : CaptureResult
    data class Video(val file: File, val mirrored: Boolean) : CaptureResult
}
