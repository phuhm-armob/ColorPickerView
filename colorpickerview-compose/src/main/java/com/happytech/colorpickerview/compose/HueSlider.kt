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
import com.happytech.colorpickerview.compose.internal.drawThumb
import com.happytech.colorpickerview.compose.internal.sliderDrag
import com.happytech.colorpickerview.compose.internal.trackGeometry
import com.happytech.colorpickerview.compose.internal.xForFraction

/**
 * Seven hue stops. The XML version uses a 360 px bitmap, one pixel per degree; here we
 * linearly interpolate between seven stops — the difference isn't noticeable, and this avoids
 * shipping a PNG file.
 */
private val HueStops = listOf(
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Cyan,
    Color.Blue,
    Color.Magenta,
    Color.Red,
)

/**
 * Slider for picking hue, sharing [state] with [ColorPicker] and [ColorAlphaSlider].
 */
@Composable
fun HueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    onHueChangeFinished: ((Float) -> Unit)? = null,
) {
    HueSlider(
        hue = state.hue,
        onHueChange = { state.hue = it },
        modifier = modifier,
        colors = colors,
        thumb = thumb,
        trackThickness = trackThickness,
        onHueChangeFinished = onHueChangeFinished,
    )
}

/**
 * Slider for picking hue, stateless version.
 *
 * @param hue current hue, 0..360.
 * @param onHueChange called continuously while dragging.
 * @param onHueChangeFinished called once when the finger is lifted.
 */
@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    onHueChangeFinished: ((Float) -> Unit)? = null,
) {
    val currentHue = hue.coerceIn(0f, 360f)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(ColorPickerDefaults.SliderHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(currentHue, 0f..360f)
            }
            .sliderDrag(
                trackThickness = trackThickness,
                onFraction = { onHueChange(it * 360f) },
                onFinished = { fraction -> onHueChangeFinished?.invoke(fraction * 360f) },
            )
    ) {
        val thicknessPx = trackThickness.toPx()
        val geometry = trackGeometry(size, thicknessPx, thicknessPx)
        val half = geometry.thickness / 2f

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = HueStops,
                startX = geometry.start,
                endX = geometry.end,
            ),
            topLeft = Offset(geometry.start - half, geometry.centerY - half),
            size = Size(geometry.end - geometry.start + geometry.thickness, geometry.thickness),
            cornerRadius = CornerRadius(half),
        )

        drawThumb(
            center = Offset(xForFraction(currentHue / 360f, geometry), geometry.centerY),
            radius = geometry.thickness,
            color = Color.hsv(currentHue, 1f, 1f),
            colors = colors,
            thumb = thumb,
        )
    }
}

@Preview(widthDp = 320, backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun HueSliderPreview() {
    HueSlider(hue = 200f, onHueChange = {})
}
