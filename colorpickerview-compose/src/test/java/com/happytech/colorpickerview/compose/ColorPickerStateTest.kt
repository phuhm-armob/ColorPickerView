package com.happytech.colorpickerview.compose

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPickerStateTest {

    @Test
    fun `state built from red exposes hue zero and full saturation`() {
        val state = ColorPickerState(Color.Red)

        assertEquals(0f, state.hue, 0.5f)
        assertEquals(1f, state.saturation, 0.01f)
        assertEquals(1f, state.value, 0.01f)
        assertEquals(1f, state.alpha, 0.01f)
    }

    @Test
    fun `colour round trips through the state`() {
        val original = Color(0.2f, 0.6f, 0.9f, 1f)
        val state = ColorPickerState(original)

        assertEquals(original.red, state.color.red, 1f / 255f)
        assertEquals(original.green, state.color.green, 1f / 255f)
        assertEquals(original.blue, state.color.blue, 1f / 255f)
    }

    @Test
    fun `alpha survives the round trip`() {
        val state = ColorPickerState(Color(1f, 0f, 0f, 0.25f))

        assertEquals(0.25f, state.alpha, 0.01f)
        assertEquals(0.25f, state.color.alpha, 0.01f)
    }

    @Test
    fun `hue is clamped instead of throwing`() {
        val state = ColorPickerState(Color.Red)

        state.hue = 400f
        assertEquals(360f, state.hue, 0.01f)

        state.hue = -20f
        assertEquals(0f, state.hue, 0.01f)
    }

    @Test
    fun `saturation value and alpha are clamped to zero one`() {
        val state = ColorPickerState(Color.Red)

        state.saturation = 3f
        state.value = -1f
        state.alpha = 7f

        assertEquals(1f, state.saturation, 0.01f)
        assertEquals(0f, state.value, 0.01f)
        assertEquals(1f, state.alpha, 0.01f)
    }

    @Test
    fun `assigning colour updates every component`() {
        val state = ColorPickerState(Color.Red)

        state.color = Color(0f, 0f, 1f, 0.5f)

        assertEquals(240f, state.hue, 0.5f)
        assertEquals(1f, state.saturation, 0.01f)
        assertEquals(1f, state.value, 0.01f)
        assertEquals(0.5f, state.alpha, 0.01f)
    }

    @Test
    fun `saver restores an equivalent state`() {
        val state = ColorPickerState(Color(0.2f, 0.6f, 0.9f, 0.4f))

        val saved = with(ColorPickerStateSaver) {
            TestSaverScope.save(state)
        }
        val restored = ColorPickerStateSaver.restore(saved as FloatArray)!!

        assertEquals(state.hue, restored.hue, 0.01f)
        assertEquals(state.saturation, restored.saturation, 0.01f)
        assertEquals(state.value, restored.value, 0.01f)
        assertEquals(state.alpha, restored.alpha, 0.01f)
    }
}

private object TestSaverScope : androidx.compose.runtime.saveable.SaverScope {
    override fun canBeSaved(value: Any): Boolean = true
}
