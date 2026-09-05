package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.happytech.colorpickerview.compose.ColorPickerColors
import com.happytech.colorpickerview.compose.ThumbStyle

/**
 * Vẽ thumb là bốn vòng tròn đồng tâm, khớp thứ tự của bản XML:
 * outline, stroke, một vòng màu tối hơn 10% làm viền trong, rồi màu thật.
 */
internal fun DrawScope.drawThumb(
    center: Offset,
    radius: Float,
    color: Color,
    colors: ColorPickerColors,
    thumb: ThumbStyle,
) {
    val outline = thumb.outlineSize.toPx()
    val stroke = thumb.strokeSize.toPx()
    val hairline = 1.dp.toPx()

    drawCircle(colors.thumbOutline, radius, center)
    drawCircle(colors.thumbStroke, radius - outline, center)
    drawCircle(lerp(color, Color.Black, 0.1f), radius - outline - stroke, center)
    drawCircle(color, radius - outline - stroke - hairline, center)
}
