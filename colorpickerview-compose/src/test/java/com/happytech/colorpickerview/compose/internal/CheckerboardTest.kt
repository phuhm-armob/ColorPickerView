package com.happytech.colorpickerview.compose.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckerboardTest {

    @Test
    fun `cells divide the thickness evenly when it is a clean multiple`() {
        assertEquals(12, checkerCellSizePx(thicknessPx = 36f, rows = 3))
    }

    @Test
    fun `cell size is rounded to a whole pixel`() {
        // 35 / 3 = 11.67 -> 12
        assertEquals(12, checkerCellSizePx(thicknessPx = 35f, rows = 3))
    }

    @Test
    fun `a very thin track still gets a one pixel cell`() {
        assertEquals(1, checkerCellSizePx(thicknessPx = 2f, rows = 8))
    }

    @Test
    fun `a row count below one is treated as one row`() {
        assertEquals(36, checkerCellSizePx(thicknessPx = 36f, rows = 0))
        assertEquals(36, checkerCellSizePx(thicknessPx = 36f, rows = -5))
    }
}
