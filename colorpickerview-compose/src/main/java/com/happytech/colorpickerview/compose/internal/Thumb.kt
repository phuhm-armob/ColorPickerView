package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.happytech.colorpickerview.compose.ColorPickerColors
import com.happytech.colorpickerview.compose.ThumbStyle

/**
 * Draws the thumb as four concentric circles, matching the order used by the XML version:
 * outline, stroke, a ring 10% darker as the inner border, then the true color.
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
