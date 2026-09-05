package com.happytech.colorpickerview.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.happytech.colorpickerview.compose.internal.checkerCellSizePx
import com.happytech.colorpickerview.compose.internal.drawCheckerboard
import com.happytech.colorpickerview.compose.internal.drawThumb
import com.happytech.colorpickerview.compose.internal.sliderDrag
import com.happytech.colorpickerview.compose.internal.sliderHeight
import com.happytech.colorpickerview.compose.internal.sliderThumbRadius
import com.happytech.colorpickerview.compose.internal.trackGeometry
import com.happytech.colorpickerview.compose.internal.xForFraction

/**
 * Slider for picking alpha, sharing [state] with [ColorPicker] and [HueSlider].
 */
@Composable
fun ColorAlphaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    showChecker: Boolean = true,
    checkerRows: Int = ColorPickerDefaults.CheckerRows,
    onAlphaChangeFinished: ((Float) -> Unit)? = null,
) {
    ColorAlphaSlider(
        color = state.opaqueColor,
        alpha = state.alpha,
        onAlphaChange = { state.alpha = it },
        modifier = modifier,
        colors = colors,
        thumb = thumb,
        trackThickness = trackThickness,
        showChecker = showChecker,
        checkerRows = checkerRows,
        onAlphaChangeFinished = onAlphaChangeFinished,
    )
}

/**
 * Slider for picking alpha, stateless version.
 *
 * @param color fully opaque color, used to build the background gradient.
 * @param alpha current alpha, 0..1.
 * @param showChecker whether to draw the checkerboard background that signals transparency.
 * @param checkerRows number of cell rows across the thickness of the bar; cell edge is derived
 *   from this so cells are always square.
 */
@Composable
fun ColorAlphaSlider(
    color: Color,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    showChecker: Boolean = true,
    checkerRows: Int = ColorPickerDefaults.CheckerRows,
    onAlphaChangeFinished: ((Float) -> Unit)? = null,
) {
    val currentAlpha = alpha.coerceIn(0f, 1f)
    val opaqueColor = color.copy(alpha = 1f)
    val thumbRadius = sliderThumbRadius(thumb.radius, trackThickness)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(sliderHeight(thumbRadius))
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(currentAlpha, 0f..1f)
            }
            .sliderDrag(
                trackThickness = trackThickness,
                thumbRadius = thumbRadius,
                onFraction = onAlphaChange,
                onFinished = { fraction -> onAlphaChangeFinished?.invoke(fraction) },
            )
    ) {
        val thicknessPx = trackThickness.toPx()
        val geometry = trackGeometry(size, thicknessPx, thumbRadius.toPx())
        val half = geometry.thickness / 2f

        val trackTopLeft = Offset(geometry.start - half, geometry.centerY - half)
        val trackSize = Size(
            geometry.end - geometry.start + geometry.thickness,
            geometry.thickness,
        )
        val corner = CornerRadius(half)

        if (showChecker) {
            drawCheckerboard(
                topLeft = trackTopLeft,
                size = trackSize,
                cornerRadius = corner,
                cellSize = checkerCellSizePx(geometry.thickness, checkerRows),
                light = colors.checkerLight,
                dark = colors.checkerDark,
            )
        }

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(opaqueColor.copy(alpha = 0f), opaqueColor),
                startX = geometry.start,
                endX = geometry.end,
            ),
            topLeft = trackTopLeft,
            size = trackSize,
            cornerRadius = corner,
        )

        drawThumb(
            center = Offset(xForFraction(currentAlpha, geometry), geometry.centerY),
            radius = thumbRadius.toPx(),
            color = opaqueColor.copy(alpha = currentAlpha),
            colors = colors,
            thumb = thumb,
        )
    }
}

@Preview(widthDp = 320, backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun ColorAlphaSliderPreview() {
    ColorAlphaSlider(color = Color.Red, alpha = 0.6f, onAlphaChange = {})
}
