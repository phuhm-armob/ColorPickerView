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
 * A touch jumps straight to that position and then keeps dragging, matching `ACTION_DOWN` in the
 * XML version.
 *
 * Both callbacks go through [rememberUpdatedState] because the `pointerInput` block only restarts
 * when its key changes — without this, the lambda would stay captured from the first composition
 * and write into stale state.
 */
@Composable
internal fun Modifier.sliderDrag(
    trackThickness: Dp,
    onFraction: (Float) -> Unit,
    onFinished: (Float) -> Unit,
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

            var lastFraction = 0f

            val down = awaitFirstDown(requireUnconsumed = false)
            lastFraction = fractionForX(down.position.x, geometry)
            currentOnFraction(lastFraction)
            down.consume()

            drag(down.id) { change ->
                lastFraction = fractionForX(change.position.x, geometry)
                currentOnFraction(lastFraction)
                change.consume()
            }

            currentOnFinished(lastFraction)
        }
    }
}

/**
 * Two-dimensional version for the saturation/value plane. Emits `(fractionX, fractionY)` clamped
 * to 0..1, with the origin at the top-left of the drawing area (after subtracting [inset] from
 * each edge).
 */
@Composable
internal fun Modifier.planeDrag(
    inset: Dp,
    onFraction: (Float, Float) -> Unit,
    onFinished: (Float, Float) -> Unit,
): Modifier {
    val currentOnFraction by rememberUpdatedState(onFraction)
    val currentOnFinished by rememberUpdatedState(onFinished)

    return pointerInput(inset) {
        val insetPx = inset.toPx()

        awaitEachGesture {
            val spanX = (size.width - insetPx * 2f).coerceAtLeast(1f)
            val spanY = (size.height - insetPx * 2f).coerceAtLeast(1f)

            var lastFractionX = 0f
            var lastFractionY = 0f

            fun emit(x: Float, y: Float) {
                lastFractionX = ((x - insetPx) / spanX).coerceIn(0f, 1f)
                lastFractionY = ((y - insetPx) / spanY).coerceIn(0f, 1f)
                currentOnFraction(lastFractionX, lastFractionY)
            }

            val down = awaitFirstDown(requireUnconsumed = false)
            emit(down.position.x, down.position.y)
            down.consume()

            drag(down.id) { change ->
                emit(change.position.x, change.position.y)
                change.consume()
            }

            currentOnFinished(lastFractionX, lastFractionY)
        }
    }
}
