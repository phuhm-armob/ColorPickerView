package com.happytech.colorpickerview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import com.happytech.colorpickerview.compose.internal.rgbToHsv

/**
 * Màu đang chọn, chia sẻ giữa [ColorPicker], [HueSlider] và [ColorAlphaSlider].
 *
 * Thay cho cơ chế gán chéo `alphaSliderView` / `hueSliderView` của bản XML.
 *
 * Mọi setter clamp giá trị thay vì ném exception: setter bị gọi trong lúc recomposition
 * nên ném ra sẽ làm sập UI.
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

    /** Hue theo độ, 0..360. */
    var hue: Float
        get() = hueState.floatValue
        set(newValue) { hueState.floatValue = newValue.coerceIn(0f, 360f) }

    /** Saturation, 0..1. */
    var saturation: Float
        get() = saturationState.floatValue
        set(newValue) { saturationState.floatValue = newValue.coerceIn(0f, 1f) }

    /** Value (độ sáng) trong mô hình HSV, 0..1. */
    var value: Float
        get() = valueState.floatValue
        set(newValue) { valueState.floatValue = newValue.coerceIn(0f, 1f) }

    /** Alpha, 0..1. */
    var alpha: Float
        get() = alphaState.floatValue
        set(newValue) { alphaState.floatValue = newValue.coerceIn(0f, 1f) }

    /** Màu kết quả. Gán vào đây sẽ tách ngược ra bốn thành phần trên. */
    var color: Color
        get() = Color.hsv(hue, saturation, value, alpha)
        set(newValue) {
            val hsv = rgbToHsv(newValue.red, newValue.green, newValue.blue)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            alpha = newValue.alpha
        }

    /** Màu đầy alpha, dùng làm nền gradient cho [ColorAlphaSlider]. */
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
 * Tạo một [ColorPickerState] sống qua config change.
 *
 * @param initialColor màu ban đầu; đổi giá trị này sẽ dựng lại state.
 */
@Composable
fun rememberColorPickerState(initialColor: Color = Color.Red): ColorPickerState =
    rememberSaveable(initialColor, saver = ColorPickerStateSaver) {
        ColorPickerState(initialColor)
    }
