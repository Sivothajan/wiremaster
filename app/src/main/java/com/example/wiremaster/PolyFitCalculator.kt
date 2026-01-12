package com.example.wiremaster

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.pow

object PolyFitCalculator {

    data class ParabolaResult(
        val a: Double,
        val b: Double,
        val c: Double,
        val vertex: PointF,
        val sagInPixels: Double
    )

    fun fitParabola(points: List<PointF>, p1: PointF, p2: PointF): ParabolaResult? {
        if (points.size < 3) return null

        val n = points.size.toDouble()
        var sumX = 0.0
        var sumX2 = 0.0
        var sumX3 = 0.0
        var sumX4 = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumX2Y = 0.0

        for (p in points) {
            val x = p.x.toDouble()
            val y = p.y.toDouble()
            sumX += x
            sumX2 += x * x
            sumX3 += x.pow(3)
            sumX4 += x.pow(4)
            sumY += y
            sumXY += x * y
            sumX2Y += x * x * y
        }

        val m = Array(3) { DoubleArray(3) }
        val r = DoubleArray(3)

        m[0][0] = sumX4; m[0][1] = sumX3; m[0][2] = sumX2; r[0] = sumX2Y
        m[1][0] = sumX3; m[1][1] = sumX2; m[1][2] = sumX; r[1] = sumXY
        m[2][0] = sumX2; m[2][1] = sumX; m[2][2] = n; r[2] = sumY

        val coeffs = solve3x3(m, r) ?: return null
        val a = coeffs[0]
        val b = coeffs[1]
        val c = coeffs[2]

        val vx = -b / (2 * a)
        val vy = (a * vx * vx) + (b * vx) + c

        val slope = (p2.y - p1.y) / (p2.x - p1.x)
        val yOnLine = p1.y + slope * (vx - p1.x)

        val sagPixels = abs(vy - yOnLine)

        return ParabolaResult(a, b, c, PointF(vx.toFloat(), vy.toFloat()), sagPixels)
    }

    private fun solve3x3(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = 3
        for (i in 0 until n) {
            var maxEl = abs(a[i][i])
            var maxRow = i
            for (k in i + 1 until n) {
                if (abs(a[k][i]) > maxEl) {
                    maxEl = abs(a[k][i])
                    maxRow = k
                }
            }
            if (maxEl == 0.0) return null

            val tmp = a[maxRow]; a[maxRow] = a[i]; a[i] = tmp
            val t = b[maxRow]; b[maxRow] = b[i]; b[i] = t

            for (k in i + 1 until n) {
                val c = -a[k][i] / a[i][i]
                for (j in i until n) {
                    if (i == j) a[k][j] = 0.0 else a[k][j] += c * a[i][j]
                }
                b[k] += c * b[i]
            }
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = 0.0
            for (j in i + 1 until n) sum += a[i][j] * x[j]
            x[i] = (b[i] - sum) / a[i][i]
        }
        return x
    }
}