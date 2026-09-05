package com.happytech.colorpickerview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import kotlin.math.floor
import kotlin.math.roundToInt

class ColorAlphaSlider(context: Context, attributeSet: AttributeSet?) :
    ColorSlider(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    private lateinit var alphaLinearGradient: LinearGradient

    private val checkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        isFilterBitmap = false
        isDither = false
    }

    private val checkerMatrix = Matrix()
    private var checkerCellSize = 0

    /**
     * Whether the transparency checkerboard is drawn underneath the alpha gradient.
     */
    var showAlphaChecker = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    /**
     * How many checkerboard cells fit across the thickness of the slider bar. Cell size is derived
     * from it, so a higher count means smaller cells. Values below 1 are clamped.
     */
    var alphaCheckerRows = DEFAULT_CHECKER_ROWS
        set(value) {
            val rows = value.coerceAtLeast(1)
            if (field != rows) {
                field = rows
                updateCheckerShader()
                invalidate()
            }
        }

    init {
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.ColorPickerView, 0, 0)
            .apply {
                try {
                    showAlphaChecker = getBoolean(
                        R.styleable.ColorPickerView_cpv_showAlphaChecker,
                        showAlphaChecker
                    )
                    alphaCheckerRows = getInt(
                        R.styleable.ColorPickerView_cpv_alphaCheckerRows,
                        alphaCheckerRows
                    )
                } finally {
                    recycle()
                }
            }
    }

    var selectedColor = Color.RED
        set(value) {
            if (field != value) {
                field = value
                initializeSliderPaint()
                invalidate()
            }
        }

    private var _alphaValue = 1f

    var alphaValue: Float = 1f
        set(value) {
            field = value
            isSliderChangingState = true
            circleXFactor = value
            calculateBounds(width.toFloat(), height.toFloat())
            invalidate()
        }
        get() = _alphaValue


    private var onAlphaChanged: ((alpha: Float) -> Unit)? = null
    private var onAlphaChangedListener: OnAlphaChangedListener? = null

    private var onAlphaChangeEnd: ((alpha: Float) -> Unit)? = null
    private var onAlphaChangeEndListener: OnAlphaChangeEndListener? = null


    override fun onCirclePositionChanged(circlePositionX: Float, circlePositionY: Float) {

        _alphaValue = calculateAlphaAt(circlePositionX)

        circleColor =
            Color.argb(
                (255 * _alphaValue).toInt(),
                Color.red(selectedColor),
                Color.green(selectedColor),
                Color.blue(selectedColor)
            )

        callListeners(_alphaValue)

        invalidate()

    }

    override fun onDragEnded(lastX: Float, lastY: Float) {
        callEndListeners(_alphaValue)
    }

    override fun calculateBounds(targetWidth: Float, targetHeight: Float) {
        super.calculateBounds(targetWidth, targetHeight)

        _alphaValue = calculateAlphaAt(circleX).coerceIn(0f, 1f)

        circleColor = calculateCircleColor()

    }

    private fun calculateCircleColor(): Int {
        return Color.argb(
            (255 * _alphaValue).toInt(),
            Color.red(selectedColor),
            Color.green(selectedColor),
            Color.blue(selectedColor)
        )
    }

    private fun calculateAlphaAt(ex: Float): Float {
        return (ex - drawingStart) / (widthF - drawingStart)
    }

    override fun initializeSliderPaint() {
        alphaLinearGradient =
            LinearGradient(
                drawingStart,
                0f,
                widthF,
                0f,
                Color.argb(
                    0,
                    Color.red(selectedColor),
                    Color.green(selectedColor),
                    Color.blue(selectedColor)
                ),
                selectedColor,
                Shader.TileMode.MIRROR
            )

        circleColor =
            Color.argb(
                (255 * _alphaValue).toInt(),
                Color.red(selectedColor),
                Color.green(selectedColor),
                Color.blue(selectedColor)
            )


        linePaint.shader = alphaLinearGradient

        updateCheckerShader()
    }

    /**
     * Rebuilds the checkerboard tile so that its cells stay proportional to the thickness of the
     * slider bar, and pins the tile to the leading edge of the bar so cells land on whole pixels.
     */
    private fun updateCheckerShader() {
        val thickness = linePaint.strokeWidth
        if (thickness <= 0f) return

        val cellSize = (thickness / alphaCheckerRows).roundToInt().coerceAtLeast(1)

        if (cellSize != checkerCellSize || checkerPaint.shader == null) {
            checkerCellSize = cellSize
            checkerPaint.shader = BitmapShader(
                createCheckerBitmap(cellSize),
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT
            )
        }

        checkerMatrix.setTranslate(
            floor(drawingStart - strokeWidthHalf),
            floor(drawingTop - (alphaCheckerRows * cellSize * 0.5f))
        )

        checkerPaint.shader?.setLocalMatrix(checkerMatrix)
        checkerPaint.strokeWidth = thickness
        checkerPaint.strokeCap = linePaint.strokeCap
    }

    private fun createCheckerBitmap(cellSize: Int): Bitmap {
        val tileSize = cellSize * 2
        val cell = cellSize.toFloat()
        val tile = tileSize.toFloat()

        return Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888).also { bitmap ->
            val cellPaint = Paint().apply { color = CHECKER_DARK_COLOR }

            Canvas(bitmap).apply {
                drawColor(CHECKER_LIGHT_COLOR)
                drawRect(0f, 0f, cell, cell, cellPaint)
                drawRect(cell, cell, tile, tile, cellPaint)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (showAlphaChecker) {
            canvas.drawLine(drawingStart, drawingTop, widthF, drawingTop, checkerPaint)
        }

        super.onDraw(canvas)
    }

    override fun onSaveInstanceState(): Parcelable {
        return (super.onSaveInstanceState() as Bundle).apply {
            putInt(SELECTED_COLOR_KEY, selectedColor)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        selectedColor = (state as Bundle).getInt(SELECTED_COLOR_KEY)
        super.onRestoreInstanceState(state)
    }

    fun setOnAlphaChangedListener(onAlphaChangedListener: OnAlphaChangedListener) {
        this.onAlphaChangedListener = onAlphaChangedListener
    }

    fun setOnAlphaChangedListener(onAlphaChangedListener: ((alpha: Float) -> Unit)) {
        onAlphaChanged = onAlphaChangedListener
    }

    fun setOnAlphaChangeEndListener(listener: ((alpha: Float) -> Unit)) {
        onAlphaChangeEnd = listener
    }

    fun setOnAlphaChangeEndListener(listener: OnAlphaChangeEndListener) {
        onAlphaChangeEndListener = listener
    }

    private fun callListeners(alpha: Float) {
        onAlphaChanged?.invoke(alpha)
        onAlphaChangedListener?.onAlphaChanged(alpha)
    }

    private fun callEndListeners(alpha: Float) {
        onAlphaChangeEndListener?.onAlphaChangeEnd(alpha)
        onAlphaChangeEnd?.invoke(alpha)
    }


    interface OnAlphaChangedListener {
        fun onAlphaChanged(alpha: Float)
    }

    interface OnAlphaChangeEndListener {
        fun onAlphaChangeEnd(alpha: Float)
    }

    companion object {
        private const val SELECTED_COLOR_KEY = "sel"
        private const val DEFAULT_CHECKER_ROWS = 3
        private const val CHECKER_LIGHT_COLOR = 0xFFFFFFFF.toInt()
        private const val CHECKER_DARK_COLOR = 0xFFD7D7E1.toInt()
    }

}