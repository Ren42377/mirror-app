package com.ren42377.mirror.model

import android.util.Size

data class AppSettings(
    val photoResolution: String = "",
    val videoQuality: String = "FHD",
    val videoFrameRate: Int = 30,
    val videoStabilization: Boolean = false,
    val opticalStabilization: Boolean = false
) {
    fun parsePhotoSize(): Size? {
        if (photoResolution.isBlank()) return null
        val parts = photoResolution.split("x")
        if (parts.size != 2) return null
        val width = parts[0].toIntOrNull() ?: return null
        val height = parts[1].toIntOrNull() ?: return null
        if (width <= 0 || height <= 0) return null
        return Size(width, height)
    }

    companion object {
        fun formatSize(size: Size): String = "${size.width}x${size.height}"
    }
}
