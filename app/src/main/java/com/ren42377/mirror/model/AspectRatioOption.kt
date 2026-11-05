package com.ren42377.mirror.model

enum class AspectRatioOption(
    val key: String,
    val label: String,
    val portraitRatio: Float?
) {
    RATIO_4_3(key = "4:3", label = "4:3", portraitRatio = 3f / 4f),
    RATIO_16_9(key = "16:9", label = "16:9", portraitRatio = 9f / 16f),
    RATIO_1_1(key = "1:1", label = "1:1", portraitRatio = 1f),
    FULLSCREEN(key = "FULL", label = "Full", portraitRatio = null);

    fun resolvePortraitRatio(screenWidth: Int, screenHeight: Int): Float {
        portraitRatio?.let { return it }
        if (screenWidth > 0 && screenHeight > 0) {
            return (screenWidth.toFloat() / screenHeight.toFloat()).coerceIn(0.3f, 1.2f)
        }
        return 9f / 16f
    }

    companion object {
        fun fromKey(key: String): AspectRatioOption {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: RATIO_4_3
        }
    }
}
