package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackGeometryTest {

    // 300 x 48 px slider, 12 px thick track, 12 px thumb radius
    private val geometry = trackGeometry(
        size = Size(300f, 48f),
        thicknessPx = 12f,
        thumbRadiusPx = 12f,
    )

    @Test
    fun `track is inset by the thumb radius on both ends`() {
        assertEquals(12f, geometry.start, 0.01f)
        assertEquals(288f, geometry.end, 0.01f)
    }

    @Test
    fun `track is centred vertically`() {
        assertEquals(24f, geometry.centerY, 0.01f)
    }

    @Test
    fun `x at the start of the track is fraction zero`() {
        assertEquals(0f, fractionForX(12f, geometry), 0.001f)
    }

    @Test
    fun `x at the end of the track is fraction one`() {
        assertEquals(1f, fractionForX(288f, geometry), 0.001f)
    }

    @Test
    fun `x at the middle of the track is fraction half`() {
        assertEquals(0.5f, fractionForX(150f, geometry), 0.001f)
    }

    @Test
    fun `x outside the track is clamped`() {
        assertEquals(0f, fractionForX(-40f, geometry), 0.001f)
        assertEquals(1f, fractionForX(9999f, geometry), 0.001f)
    }

    @Test
    fun `fraction round trips back to x`() {
        assertEquals(150f, xForFraction(fractionForX(150f, geometry), geometry), 0.01f)
    }

    @Test
    fun `a track narrower than its insets does not divide by zero`() {
        val tiny = trackGeometry(Size(4f, 48f), thicknessPx = 12f, thumbRadiusPx = 12f)

        assertEquals(0f, fractionForX(0f, tiny), 0.001f)
        assertEquals(0f, fractionForX(4f, tiny), 0.001f)
    }
}
