package com.happytech.colorpickerview.compose.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorMathTest {

    private fun assertHsv(expected: Triple<Float, Float, Float>, actual: FloatArray) {
        assertEquals(expected.first, actual[0], 0.5f)
        assertEquals(expected.second, actual[1], 0.01f)
        assertEquals(expected.third, actual[2], 0.01f)
    }

    @Test
    fun `pure red is hue zero`() {
        assertHsv(Triple(0f, 1f, 1f), rgbToHsv(1f, 0f, 0f))
    }

    @Test
    fun `pure green is hue 120`() {
        assertHsv(Triple(120f, 1f, 1f), rgbToHsv(0f, 1f, 0f))
    }

    @Test
    fun `pure blue is hue 240`() {
        assertHsv(Triple(240f, 1f, 1f), rgbToHsv(0f, 0f, 1f))
    }

    @Test
    fun `magenta wraps into positive hue`() {
        assertHsv(Triple(300f, 1f, 1f), rgbToHsv(1f, 0f, 1f))
    }

    @Test
    fun `white has no saturation`() {
        assertHsv(Triple(0f, 0f, 1f), rgbToHsv(1f, 1f, 1f))
    }

    @Test
    fun `black has no saturation and no value`() {
        assertHsv(Triple(0f, 0f, 0f), rgbToHsv(0f, 0f, 0f))
    }

    @Test
    fun `mid grey keeps value at half`() {
        assertHsv(Triple(0f, 0f, 0.5f), rgbToHsv(0.5f, 0.5f, 0.5f))
    }
}
