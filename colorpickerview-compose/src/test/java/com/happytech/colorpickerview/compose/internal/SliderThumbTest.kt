package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.unit.dp
import com.happytech.colorpickerview.compose.ColorPickerDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class SliderThumbTest {

    @Test
    fun `thumb radius follows the configured thumb, not the track thickness`() {
        assertEquals(24.dp, sliderThumbRadius(thumbRadius = 24.dp, trackThickness = 12.dp))
    }

    @Test
    fun `a thin track does not shrink a large thumb`() {
        assertEquals(20.dp, sliderThumbRadius(thumbRadius = 20.dp, trackThickness = 4.dp))
    }

    @Test
    fun `a thumb thinner than the bar is grown to half the track thickness`() {
        assertEquals(15.dp, sliderThumbRadius(thumbRadius = 6.dp, trackThickness = 30.dp))
    }

    @Test
    fun `the defaults keep the historical thumb radius`() {
        assertEquals(
            12.dp,
            sliderThumbRadius(
                thumbRadius = ColorPickerDefaults.thumb().radius,
                trackThickness = ColorPickerDefaults.TrackThickness,
            ),
        )
    }

    @Test
    fun `the slider keeps its default height while the thumb fits`() {
        assertEquals(ColorPickerDefaults.SliderHeight, sliderHeight(thumbRadius = 12.dp))
    }

    @Test
    fun `the slider grows so a large thumb is not clipped`() {
        assertEquals(64.dp, sliderHeight(thumbRadius = 32.dp))
    }
}
