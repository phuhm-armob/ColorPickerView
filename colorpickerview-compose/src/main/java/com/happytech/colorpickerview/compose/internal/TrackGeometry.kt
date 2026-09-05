package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.Size

/**
 * Geometry of the slider bar, in pixels.
 *
 * [start] and [end] are the centers of the two rounded ends, i.e. the range the thumb travels
 * over. The bar itself extends an extra half of [thickness] past each end.
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
