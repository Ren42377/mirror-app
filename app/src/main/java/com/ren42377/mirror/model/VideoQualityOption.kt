package com.ren42377.mirror.model

import androidx.camera.video.Quality

enum class VideoQualityOption(
    val key: String,
    val label: String,
    val description: String,
    val cameraQuality: Quality
) {
    UHD("UHD", "UHD (4K)", "2160p Ultra HD", Quality.UHD),
    FHD("FHD", "FHD (1080p)", "1080p Full HD", Quality.FHD),
    HD("HD", "HD (720p)", "720p High Definition", Quality.HD),
    SD("SD", "SD (480p)", "480p Standard Definition", Quality.SD);

    companion object {
        fun fromKey(key: String): VideoQualityOption {
            return entries.firstOrNull { it.key == key } ?: FHD
        }

        fun fromQuality(quality: Quality): VideoQualityOption? {
            return entries.firstOrNull { it.cameraQuality == quality }
        }
    }
}
