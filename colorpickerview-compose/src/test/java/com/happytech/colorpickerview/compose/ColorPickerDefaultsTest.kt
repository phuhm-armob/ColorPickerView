package com.happytech.colorpickerview.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPickerDefaultsTest {

    @Test
    fun `default colors match the xml view defaults`() {
        val colors = ColorPickerDefaults.colors()

        assertEquals(Color.White, colors.thumbStroke)
        assertEquals(Color.White, colors.thumbOutline)
        assertEquals(Color(0x0D000000), colors.pickerOutline)
        assertEquals(Color.White, colors.checkerLight)
        assertEquals(Color(0xFFD7D7E1), colors.checkerDark)
    }

    @Test
    fun `default thumb matches the xml view defaults`() {
        val thumb = ColorPickerDefaults.thumb()

        assertEquals(12.dp, thumb.radius)
        assertEquals(2.dp, thumb.strokeSize)
        assertEquals(1.dp, thumb.outlineSize)
    }

    @Test
    fun `overriding one value leaves the others at their defaults`() {
        val colors = ColorPickerDefaults.colors(checkerDark = Color.Gray)

        assertEquals(Color.Gray, colors.checkerDark)
        assertEquals(Color.White, colors.checkerLight)
    }

    @Test
    fun `layout defaults match the xml view defaults`() {
        assertEquals(8.dp, ColorPickerDefaults.PickerCornerRadius)
        assertEquals(1.dp, ColorPickerDefaults.PickerOutlineWidth)
        assertEquals(12.dp, ColorPickerDefaults.TrackThickness)
        assertEquals(48.dp, ColorPickerDefaults.SliderHeight)
        assertEquals(3, ColorPickerDefaults.CheckerRows)
    }
}
