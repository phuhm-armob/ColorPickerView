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
import kotlin.math.roundToInt

/**
 * Cạnh ô checkerboard, tính bằng pixel nguyên.
 *
 * Làm tròn về số nguyên là thứ giữ cho ô sắc nét — cạnh rơi vào nửa pixel sẽ bị nhoè.
 */
internal fun checkerCellSizePx(thicknessPx: Float, rows: Int): Int =
    (thicknessPx / rows.coerceAtLeast(1)).roundToInt().coerceAtLeast(1)

/**
 * Vẽ nền checkerboard trong một hình chữ nhật bo tròn.
 *
 * Vẽ từng ô trong `clipPath` thay vì dùng shader tile: không phụ thuộc chữ ký
 * `ImageShader` vốn đổi theo phiên bản Compose, và toạ độ ô là pixel nguyên nên sắc nét
 * tuyệt đối.
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
        val columns = ceil(size.width / cell).toInt()
        val rows = ceil(size.height / cell).toInt()

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                if ((row + column) % 2 != 0) continue

                drawRect(
                    color = dark,
                    topLeft = Offset(topLeft.x + column * cell, topLeft.y + row * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}
