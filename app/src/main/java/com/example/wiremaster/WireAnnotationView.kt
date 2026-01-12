package com.example.wiremaster

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.withMatrix
import kotlin.math.hypot

class WireAnnotationView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var onPointPlaced: (() -> Unit)? = null

    var displayBitmap: Bitmap? = null
    var p1 = PointF(100f, 400f)
    var p2 = PointF(800f, 400f)
    var pSag = PointF(450f, 600f)

    var placementMode = 0

    private val drawMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private var scaleFactor = 1.0f

    private var selectedPoint: Int = -1
    private var isDragging = false
    private val touchPointArr = FloatArray(2)

    private val scaleDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 10.0f)
                drawMatrix.postScale(
                    detector.scaleFactor,
                    detector.scaleFactor,
                    detector.focusX,
                    detector.focusY
                )
                invalidate()
                return true
            }
        })

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!isDragging) {
                    drawMatrix.postTranslate(-distanceX, -distanceY)
                    invalidate()
                    return true
                }
                return false
            }
        })

    private val paintLine = Paint().apply {
        color = Color.CYAN; strokeWidth = 5f; style = Paint.Style.STROKE; isAntiAlias = true
    }
    private val paintDot =
        Paint().apply { color = Color.RED; style = Paint.Style.FILL; isAntiAlias = true }
    private val paintSelected =
        Paint().apply { color = Color.GREEN; style = Paint.Style.FILL; isAntiAlias = true }

    override fun performClick(): Boolean {
        super.performClick(); return true
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (displayBitmap == null) return

        canvas.withMatrix(drawMatrix) {
            drawBitmap(displayBitmap!!, 0f, 0f, null)

            val path = Path()
            path.moveTo(p1.x, p1.y)
            path.quadTo(pSag.x, pSag.y * 2 - (p1.y + p2.y) / 2, p2.x, p2.y)
            drawPath(path, paintLine)

            val dotSize = 40f / scaleFactor
            drawCircle(
                p1.x,
                p1.y,
                dotSize,
                if (selectedPoint == 1 || placementMode == 1) paintSelected else paintDot
            )
            drawCircle(
                p2.x,
                p2.y,
                dotSize,
                if (selectedPoint == 2 || placementMode == 2) paintSelected else paintDot
            )
            drawCircle(
                pSag.x,
                pSag.y,
                dotSize,
                if (selectedPoint == 3 || placementMode == 3) paintSelected else paintDot
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        if (event.pointerCount > 1 || scaleDetector.isInProgress) {
            return true
        }

        touchPointArr[0] = event.x
        touchPointArr[1] = event.y
        drawMatrix.invert(inverseMatrix)
        inverseMatrix.mapPoints(touchPointArr)

        val imgX = touchPointArr[0]
        val imgY = touchPointArr[1]
        val touchRadius = 100f / scaleFactor

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (placementMode > 0) {
                    if (placementMode == 1) p1.set(imgX, imgY)
                    if (placementMode == 2) p2.set(imgX, imgY)
                    if (placementMode == 3) pSag.set(imgX, imgY)

                    placementMode = 0
                    onPointPlaced?.invoke()
                    invalidate()
                    return true
                }

                if (hypot((imgX - p1.x).toDouble(), (imgY - p1.y).toDouble()) < touchRadius) {
                    selectedPoint = 1; isDragging = true
                } else if (hypot(
                        (imgX - p2.x).toDouble(),
                        (imgY - p2.y).toDouble()
                    ) < touchRadius
                ) {
                    selectedPoint = 2; isDragging = true
                } else if (hypot(
                        (imgX - pSag.x).toDouble(),
                        (imgY - pSag.y).toDouble()
                    ) < touchRadius
                ) {
                    selectedPoint = 3; isDragging = true
                } else {
                    selectedPoint = -1; isDragging = false
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && selectedPoint != -1 && event.pointerCount == 1) {
                    if (selectedPoint == 1) p1.set(imgX, imgY)
                    if (selectedPoint == 2) p2.set(imgX, imgY)
                    if (selectedPoint == 3) pSag.set(imgX, imgY)
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                isDragging = false
                selectedPoint = -1
                invalidate()
                performClick()
            }
        }
        return true
    }

    fun resetView() {
        drawMatrix.reset()
        scaleFactor = 1.0f
        displayBitmap?.let {
            val w = it.width.toFloat()
            val h = it.height.toFloat()
            p1.set(w * 0.2f, h * 0.5f); p2.set(w * 0.8f, h * 0.5f); pSag.set(w * 0.5f, h * 0.7f)
        }
        invalidate()
    }
}