package com.happytech.colorpickerview.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.happytech.colorpickerview.compose.internal.drawThumb
import com.happytech.colorpickerview.compose.internal.planeDrag

/**
 * Plane for picking saturation (left→right) and value (top→bottom), sharing [state] with
 * [HueSlider] and [ColorAlphaSlider].
 *
 * Unlike the stateless overload, [onColorChangeFinished] here carries alpha: it reads
 * [ColorPickerState.color], which includes [ColorPickerState.alpha].
 */
@Composable
fun ColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    cornerRadius: Dp = ColorPickerDefaults.PickerCornerRadius,
    outlineWidth: Dp = ColorPickerDefaults.PickerOutlineWidth,
    onColorChangeFinished: ((Color) -> Unit)? = null,
) {
    ColorPicker(
        hue = state.hue,
        saturation = state.saturation,
        value = state.value,
        onChange = { saturation, value ->
            state.saturation = saturation
            state.value = value
        },
        modifier = modifier,
        colors = colors,
        thumb = thumb,
        cornerRadius = cornerRadius,
        outlineWidth = outlineWidth,
        onColorChangeFinished = onColorChangeFinished?.let { callback ->
            { _: Color -> callback(state.color) }
        },
    )
}

/**
 * Plane for picking saturation/value, stateless version.
 *
 * The drawing area is inset on every edge by exactly `thumb.radius` so the thumb isn't clipped
 * at the corners — matching how the XML version computes `drawingStart`.
 *
 * Because this overload has no alpha to give, [onColorChangeFinished] here always reports an
 * opaque color (`Color.hsv` defaults alpha to 1f). Use the state overload if you need the alpha
 * that was in effect.
 *
 * @param hue background hue, 0..360; this composable does not change hue.
 * @param onChange called continuously while dragging, with saturation and value clamped to 0..1.
 */
@Composable
fun ColorPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    cornerRadius: Dp = ColorPickerDefaults.PickerCornerRadius,
    outlineWidth: Dp = ColorPickerDefaults.PickerOutlineWidth,
    onColorChangeFinished: ((Color) -> Unit)? = null,
) {
    val currentHue = hue.coerceIn(0f, 360f)
    val currentSaturation = saturation.coerceIn(0f, 1f)
    val currentValue = value.coerceIn(0f, 1f)

    Canvas(
        modifier
            .fillMaxWidth()
            .planeDrag(
                inset = thumb.radius,
                onFraction = { fractionX, fractionY -> onChange(fractionX, 1f - fractionY) },
                onFinished = { fractionX, fractionY ->
                    onColorChangeFinished?.invoke(
                        Color.hsv(currentHue, fractionX, 1f - fractionY)
                    )
                },
            )
    ) {
        val inset = thumb.radius.toPx()
        val planeTopLeft = Offset(inset, inset)
        val planeSize = Size(
            (size.width - inset * 2f).coerceAtLeast(0f),
            (size.height - inset * 2f).coerceAtLeast(0f),
        )
        val corner = CornerRadius(cornerRadius.toPx())

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, Color.hsv(currentHue, 1f, 1f)),
                startX = planeTopLeft.x,
                endX = planeTopLeft.x + planeSize.width,
            ),
            topLeft = planeTopLeft,
            size = planeSize,
            cornerRadius = corner,
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = planeTopLeft.y,
                endY = planeTopLeft.y + planeSize.height,
            ),
            topLeft = planeTopLeft,
            size = planeSize,
            cornerRadius = corner,
        )

        // The XML version insets the outline by outlineSize / 2.5 and rounds it 0.5dp smaller —
        // kept as-is so both versions look the same.
        val outline = outlineWidth.toPx()
        val outlineInset = outline / 2.5f

        drawRoundRect(
            color = colors.pickerOutline,
            topLeft = Offset(planeTopLeft.x + outlineInset, planeTopLeft.y + outlineInset),
            size = Size(
                (planeSize.width - outlineInset * 2f).coerceAtLeast(0f),
                (planeSize.height - outlineInset * 2f).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius((cornerRadius - 0.5.dp).toPx().coerceAtLeast(0f)),
            style = Stroke(outline),
        )

        drawThumb(
            center = Offset(
                planeTopLeft.x + planeSize.width * currentSaturation,
                planeTopLeft.y + planeSize.height * (1f - currentValue),
            ),
            radius = inset,
            color = Color.hsv(currentHue, currentSaturation, currentValue),
            colors = colors,
            thumb = thumb,
        )
    }
}

@Preview(widthDp = 320, heightDp = 240, backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun ColorPickerPreview() {
    ColorPicker(
        hue = 200f,
        saturation = 0.7f,
        value = 0.8f,
        onChange = { _, _ -> },
    )
}
