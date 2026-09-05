package com.happytech.colorpickerview.compose.internal

/**
 * Đổi RGB (mỗi kênh 0..1) sang HSV thuần Kotlin.
 *
 * `android.graphics.Color.colorToHSV` không dùng được vì module này phải unit test được
 * trên JVM.
 *
 * @return `[hue 0..360, saturation 0..1, value 0..1]`
 */
internal fun rgbToHsv(red: Float, green: Float, blue: Float): FloatArray {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min

    val rawHue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }

    val hue = if (rawHue < 0f) rawHue + 360f else rawHue
    val saturation = if (max == 0f) 0f else delta / max

    return floatArrayOf(hue, saturation, max)
}
