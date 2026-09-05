package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Checkerboard cell edge, in whole pixels.
 *
 * Rounding to an integer is what keeps the cells crisp — an edge landing on a half pixel gets
 * blurred.
 */
internal fun checkerCellSizePx(thicknessPx: Float, rows: Int): Int =
    (thicknessPx / rows.coerceAtLeast(1)).roundToInt().coerceAtLeast(1)

/**
 * Draws the checkerboard background inside a rounded rectangle.
 *
 * Draws each cell inside a `clipPath` rather than using a shader tile: it doesn't depend on the
 * `ImageShader` signature, which changes across Compose versions, and cell coordinates are whole
 * pixels so the result is perfectly crisp.
 *
 * The cell loop runs against a floored origin — pinning cells to whole pixels — while the clip
 * path itself is built from the true, unfloored rect so the capsule edge stays smooth. The loop
 * bounds are extended so that flooring the origin can never leave an uncovered sliver at the
 * trailing edge.
 */
internal fun DrawScope.drawCheckerboard(
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    cellSize: Int,
    light: Color,
    dark: Color,
) {
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = topLeft.x,
                top = topLeft.y,
                right = topLeft.x + size.width,
                bottom = topLeft.y + size.height,
                cornerRadius = cornerRadius,
            )
        )
    }

    clipPath(path) {
        drawRect(color = light, topLeft = topLeft, size = size)

        val cell = cellSize.toFloat()
        val originX = floor(topLeft.x)
        val originY = floor(topLeft.y)
        // Extend the covered span by the amount flooring shifted the origin, so the last cell
        // still reaches the true trailing edge.
        val columns = ceil((topLeft.x + size.width - originX) / cell).toInt()
        val rows = ceil((topLeft.y + size.height - originY) / cell).toInt()

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                if ((row + column) % 2 != 0) continue

                drawRect(
                    color = dark,
                    topLeft = Offset(originX + column * cell, originY + row * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}
