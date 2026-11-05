package com.ren42377.mirror.model

import android.util.Size

/**
 * A video quality that the active camera can really record, together with the
 * frame rates that are valid for that quality on this device.
 */
data class VideoModeCapability(
    val quality: VideoQualityOption,
    val resolution: Size?,
    val frameRates: List<Int>
)
