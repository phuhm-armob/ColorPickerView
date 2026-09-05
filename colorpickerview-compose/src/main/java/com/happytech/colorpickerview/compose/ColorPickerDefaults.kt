package com.happytech.colorpickerview.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colors for the thumb, outline and checkerboard cells. Built via [ColorPickerDefaults.colors]. */
@Immutable
class ColorPickerColors internal constructor(
    val thumbStroke: Color,
    val thumbOutline: Color,
    val pickerOutline: Color,
    val checkerLight: Color,
    val checkerDark: Color,
)

/**
 * Sizes of the thumb's rings. Built via [ColorPickerDefaults.thumb].
 *
 * [radius] only applies to [ColorPicker]; the two sliders always take their thumb radius from
 * their own `trackThickness`.
 */
@Immutable
class ThumbStyle internal constructor(
    val radius: Dp,
    val strokeSize: Dp,
    val outlineSize: Dp,
)

object ColorPickerDefaults {

    val PickerCornerRadius: Dp = 8.dp
    val PickerOutlineWidth: Dp = 1.dp
    val TrackThickness: Dp = 12.dp
    val SliderHeight: Dp = 48.dp
    const val CheckerRows: Int = 3

    fun colors(
        thumbStroke: Color = Color.White,
        thumbOutline: Color = Color.White,
        pickerOutline: Color = Color(0x0D000000),
        checkerLight: Color = Color.White,
        checkerDark: Color = Color(0xFFD7D7E1),
    ): ColorPickerColors = ColorPickerColors(
        thumbStroke = thumbStroke,
        thumbOutline = thumbOutline,
        pickerOutline = pickerOutline,
        checkerLight = checkerLight,
        checkerDark = checkerDark,
    )

    fun thumb(
        radius: Dp = 12.dp,
        strokeSize: Dp = 2.dp,
        outlineSize: Dp = 1.dp,
    ): ThumbStyle = ThumbStyle(
        radius = radius,
        strokeSize = strokeSize,
        outlineSize = outlineSize,
    )
}
