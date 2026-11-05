package com.ren42377.mirror.model

import androidx.camera.video.Quality

enum class VideoQualityOption(
    val key: String,
    val label: String,
    val description: String
) {
    UHD("UHD", "UHD (4K)", "2160p Ultra HD"),
    FHD("FHD", "FHD (1080p)", "1080p Full HD"),
    HD("HD", "HD (720p)", "720p High Definition"),
    SD("SD", "SD (480p)", "480p Standard Definition");

    val cameraQuality: Quality
        get() = when (this) {
            UHD -> Quality.UHD
            FHD -> Quality.FHD
            HD -> Quality.HD
            SD -> Quality.SD
        }

    companion object {
        fun fromKey(key: String): VideoQualityOption {
            return entries.firstOrNull { it.key == key } ?: FHD
        }

        fun fromQuality(quality: Quality): VideoQualityOption? {
            return when (quality) {
                Quality.UHD -> UHD
                Quality.FHD -> FHD
                Quality.HD -> HD
                Quality.SD -> SD
                else -> null
            }
        }
    }
}
