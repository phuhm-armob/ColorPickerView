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
 * Mặt phẳng chọn saturation (trái→phải) và value (trên→dưới), dùng chung [state] với
 * [HueSlider] và [ColorAlphaSlider].
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
        onColorChangeFinished = onColorChangeFinished,
    )
}

/**
 * Mặt phẳng chọn saturation/value, bản stateless.
 *
 * Vùng vẽ thụt vào mỗi cạnh đúng bằng `thumb.radius` để thumb không bị cắt ở góc — khớp
 * cách bản XML tính `drawingStart`.
 *
 * @param hue hue nền, 0..360; composable này không đổi hue.
 * @param onChange gọi liên tục trong lúc kéo, với saturation và value đã clamp 0..1.
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
                onFinished = {
                    onColorChangeFinished?.invoke(
                        Color.hsv(currentHue, currentSaturation, currentValue)
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

        // Bản XML thụt outline vào outlineSize / 2.5 và bo nhỏ hơn 0.5dp — giữ nguyên
        // để hai bản trông giống nhau.
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
