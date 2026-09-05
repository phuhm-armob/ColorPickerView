package com.happytech.colorpickerview.compose.internal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp

/**
 * Chạm là nhảy tới vị trí đó luôn rồi kéo tiếp, giống `ACTION_DOWN` của bản XML.
 *
 * Hai callback đi qua [rememberUpdatedState] vì khối `pointerInput` chỉ chạy lại khi key
 * đổi — không có nó, lambda bị giữ lại từ lần composition đầu và ghi vào state cũ.
 */
@Composable
internal fun Modifier.sliderDrag(
    trackThickness: Dp,
    onFraction: (Float) -> Unit,
    onFinished: () -> Unit,
): Modifier {
    val currentOnFraction by rememberUpdatedState(onFraction)
    val currentOnFinished by rememberUpdatedState(onFinished)

    return pointerInput(trackThickness) {
        val thicknessPx = trackThickness.toPx()

        awaitEachGesture {
            val geometry = trackGeometry(
                size = Size(size.width.toFloat(), size.height.toFloat()),
                thicknessPx = thicknessPx,
                thumbRadiusPx = thicknessPx,
            )

            val down = awaitFirstDown(requireUnconsumed = false)
            currentOnFraction(fractionForX(down.position.x, geometry))
            down.consume()

            drag(down.id) { change ->
                currentOnFraction(fractionForX(change.position.x, geometry))
                change.consume()
            }

            currentOnFinished()
        }
    }
}

/**
 * Bản hai chiều cho mặt phẳng saturation/value. Phát ra `(fractionX, fractionY)` đã clamp
 * 0..1, gốc ở góc trên-trái của vùng vẽ (đã trừ [inset] mỗi cạnh).
 */
@Composable
internal fun Modifier.planeDrag(
    inset: Dp,
    onFraction: (Float, Float) -> Unit,
    onFinished: () -> Unit,
): Modifier {
    val currentOnFraction by rememberUpdatedState(onFraction)
    val currentOnFinished by rememberUpdatedState(onFinished)

    return pointerInput(inset) {
        val insetPx = inset.toPx()

        awaitEachGesture {
            val spanX = (size.width - insetPx * 2f).coerceAtLeast(1f)
            val spanY = (size.height - insetPx * 2f).coerceAtLeast(1f)

            fun emit(x: Float, y: Float) = currentOnFraction(
                ((x - insetPx) / spanX).coerceIn(0f, 1f),
                ((y - insetPx) / spanY).coerceIn(0f, 1f),
            )

            val down = awaitFirstDown(requireUnconsumed = false)
            emit(down.position.x, down.position.y)
            down.consume()

            drag(down.id) { change ->
                emit(change.position.x, change.position.y)
                change.consume()
            }

            currentOnFinished()
        }
    }
}
