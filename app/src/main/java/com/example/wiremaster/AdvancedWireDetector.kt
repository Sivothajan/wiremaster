package com.example.wiremaster

import android.graphics.Bitmap
import android.graphics.PointF
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc

object AdvancedWireDetector {

    data class DetectionResult(
        val p1: PointF,
        val p2: PointF,
        val pSag: PointF,
        val wirePoints: List<PointF>,
        val fittedParabola: PolyFitCalculator.ParabolaResult?
    )

    fun analyze(bitmap: Bitmap): DetectionResult {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)

        val corners = Mat()
        val blockSize = 2
        val apertureSize = 3
        val k = 0.04
        Imgproc.cornerHarris(gray, corners, blockSize, apertureSize, k)

        Core.normalize(corners, corners, 0.0, 255.0, Core.NORM_MINMAX)

        val cornerPoints = ArrayList<PointF>()
        val threshold = 150f
        val cornerData = FloatArray((corners.total() * corners.channels()).toInt())
        corners.get(0, 0, cornerData)

        val width = corners.cols()
        for (i in cornerData.indices) {
            if (cornerData[i] > threshold) {
                val x = (i % width).toFloat()
                val y = (i / width).toFloat()
                cornerPoints.add(PointF(x, y))
            }
        }

        val leftCluster = cornerPoints.filter { it.x < bitmap.width * 0.25 }.toMutableList()
        val rightCluster = cornerPoints.filter { it.x > bitmap.width * 0.75 }.toMutableList()

        val p1 = calculateCentroid(leftCluster) ?: PointF(bitmap.width * 0.1f, bitmap.height * 0.4f)
        val p2 =
            calculateCentroid(rightCluster) ?: PointF(bitmap.width * 0.9f, bitmap.height * 0.4f)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        Imgproc.dilate(edges, edges, Mat(), Point(-1.0, -1.0), 1)

        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE)

        var bestContourPoints: List<PointF> = ArrayList()
        var maxLen = 0.0

        for (c in contours) {
            val rect = Imgproc.boundingRect(c)
            if (rect.width > bitmap.width * 0.3) {
                val len = Imgproc.arcLength(MatOfPoint2f(*c.toArray()), false)
                if (len > maxLen) {
                    maxLen = len
                    bestContourPoints = c.toList().map { PointF(it.x.toFloat(), it.y.toFloat()) }
                }
            }
        }

        var fitResult: PolyFitCalculator.ParabolaResult? = null
        var pSag = PointF((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)

        if (bestContourPoints.isNotEmpty()) {
            fitResult = PolyFitCalculator.fitParabola(bestContourPoints, p1, p2)

            if (fitResult != null) {
                pSag = fitResult.vertex
            } else {
                val lowest = bestContourPoints.maxByOrNull { it.y }
                if (lowest != null) pSag = lowest
            }
        }

        return DetectionResult(p1, p2, pSag, bestContourPoints, fitResult)
    }

    private fun calculateCentroid(points: List<PointF>): PointF? {
        if (points.isEmpty()) return null
        var sumX = 0f
        var sumY = 0f
        for (p in points) {
            sumX += p.x
            sumY += p.y
        }
        return PointF(sumX / points.size, sumY / points.size)
    }
}