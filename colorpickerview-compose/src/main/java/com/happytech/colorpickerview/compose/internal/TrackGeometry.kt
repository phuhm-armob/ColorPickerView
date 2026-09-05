package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.Size

/**
 * Hình học của thanh slider, tính bằng pixel.
 *
 * [start] và [end] là tâm của hai đầu bo tròn, tức là khoảng chạy của thumb. Bản thân
 * thanh còn phủ thêm nửa [thickness] ra ngoài mỗi đầu.
 */
internal class TrackGeometry(
    val start: Float,
    val end: Float,
    val centerY: Float,
    val thickness: Float,
)

internal fun trackGeometry(
    size: Size,
    thicknessPx: Float,
    thumbRadiusPx: Float,
): TrackGeometry = TrackGeometry(
    start = thumbRadiusPx,
    end = (size.width - thumbRadiusPx).coerceAtLeast(thumbRadiusPx),
    centerY = size.height / 2f,
    thickness = thicknessPx,
)

internal fun fractionForX(x: Float, geometry: TrackGeometry): Float {
    val span = geometry.end - geometry.start
    if (span <= 0f) return 0f
    return ((x - geometry.start) / span).coerceIn(0f, 1f)
}

internal fun xForFraction(fraction: Float, geometry: TrackGeometry): Float =
    geometry.start + (geometry.end - geometry.start) * fraction.coerceIn(0f, 1f)
