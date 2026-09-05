package com.happytech.colorpickerview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import com.happytech.colorpickerview.compose.internal.rgbToHsv

/**
 * The currently selected color, shared between [ColorPicker], [HueSlider] and
 * [ColorAlphaSlider].
 *
 * Replaces the cross-wiring of `alphaSliderView` / `hueSliderView` in the XML version.
 *
 * Every setter clamps its value instead of throwing: setters get called during recomposition, so
 * throwing there would crash the UI.
 */
@Stable
class ColorPickerState internal constructor(
    hue: Float,
    saturation: Float,
    value: Float,
    alpha: Float,
) {
    private val hueState = mutableFloatStateOf(hue.coerceIn(0f, 360f))
    private val saturationState = mutableFloatStateOf(saturation.coerceIn(0f, 1f))
    private val valueState = mutableFloatStateOf(value.coerceIn(0f, 1f))
    private val alphaState = mutableFloatStateOf(alpha.coerceIn(0f, 1f))

    /** Hue in degrees, 0..360. */
    var hue: Float
        get() = hueState.floatValue
        set(newValue) { hueState.floatValue = newValue.coerceIn(0f, 360f) }

    /** Saturation, 0..1. */
    var saturation: Float
        get() = saturationState.floatValue
        set(newValue) { saturationState.floatValue = newValue.coerceIn(0f, 1f) }

    /** Value (brightness) in the HSV model, 0..1. */
    var value: Float
        get() = valueState.floatValue
        set(newValue) { valueState.floatValue = newValue.coerceIn(0f, 1f) }

    /** Alpha, 0..1. */
    var alpha: Float
        get() = alphaState.floatValue
        set(newValue) { alphaState.floatValue = newValue.coerceIn(0f, 1f) }

    /** The resulting color. Assigning to this splits it back into the four fields above. */
    var color: Color
        get() = Color.hsv(hue, saturation, value, alpha)
        set(newValue) {
            val hsv = rgbToHsv(newValue.red, newValue.green, newValue.blue)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            alpha = newValue.alpha
        }

    /** Fully opaque color, used as the gradient background for [ColorAlphaSlider]. */
    internal val opaqueColor: Color
        get() = Color.hsv(hue, saturation, value, alpha = 1f)
}

internal fun ColorPickerState(color: Color): ColorPickerState {
    val hsv = rgbToHsv(color.red, color.green, color.blue)
    return ColorPickerState(hsv[0], hsv[1], hsv[2], color.alpha)
}

internal val ColorPickerStateSaver: Saver<ColorPickerState, FloatArray> = Saver(
    save = { floatArrayOf(it.hue, it.saturation, it.value, it.alpha) },
    restore = { ColorPickerState(it[0], it[1], it[2], it[3]) },
)

/**
 * Creates a [ColorPickerState] that survives configuration changes.
 *
 * @param initialColor the initial color; changing this value rebuilds the state.
 */
@Composable
fun rememberColorPickerState(initialColor: Color = Color.Red): ColorPickerState =
    rememberSaveable(initialColor, saver = ColorPickerStateSaver) {
        ColorPickerState(initialColor)
    }
